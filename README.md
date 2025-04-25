#Kafka Outbox Pattern with Spring Event - 주문 예제

Kafka Outbox 패턴을 Spring Boot와 Event 기반으로 구현하며,
주문(Order) 도메인을 중심으로,**트랜잭션 안정성 보장**과**Kafka 전송 재시도 가능성**을 확보하는 방법을 설명

---

##개요

Kafka Outbox 패턴은 DB 트랜잭션과 Kafka 전송 간의 일관성을 유지하기 위한 대표적인 방법입니다.
Spring의 `@TransactionalEventListener`를 활용하여, 도메인 이벤트를 트랜잭션 내부에서 발행하고,
트랜잭션 커밋 이후 Kafka로 안전하게 발송되도록 구성했습니다.

---

##Kafka Outbox 패턴

Kafka Outbox 패턴은**DB 트랜잭션과 Kafka 메시지 발행 사이의 일관성을 보장**하는 방식입니다.
직접 Kafka를 발행하는 대신, 먼저**Outbox 테이블에 메시지를 저장**하고,
트랜잭션 커밋 이후 별도 프로세스(리스너/스케줄러 등)에서 Kafka로 전송합니다.

>장애 발생 시 메시지를 유실하지 않고 재처리 가능하게 해주는 신뢰성 패턴입니다.

---

##CDC(Change Data Capture)

CDC는 DB의 변경 로그를 추적하여 Kafka로 보내는**비침투형 이벤트 수집 방식**입니다.
Outbox 패턴은**애플리케이션 내부에서 이벤트를 직접 저장하고 처리**하는 방식으로,
애플리케이션 코드의 제어 하에 있어 보다 명시적이고 확장성 있는 흐름을 구성할 수 있습니다.

---

##핵심 흐름 요약

1.사용자가 주문을 요청한다
2.OrderService에서 주문을 저장하고 이벤트를 발행한다
3.이벤트 리스너가 아웃박스 테이블에 이벤트를 저장한다 (Before Commit)
4.트랜잭션 커밋 후, 아웃박스에서 Kafka로 메시지를 발송한다 (After Commit)
5.Kafka 전송 성공 시 Outbox 상태를 업데이트한다 (`sent=true`)
6.OutboxScheduler 에서 일정주기로 Outbox 에서 발행되지 못한 메시지를 발행하고 상태를 업데이트 한다.
---

##주요 클래스

###1. OrderService

```java
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ApplicationEventPublisher publisher;

    @Transactional
    public Long createOrder(Long productId, int quantity) {
        Order order = Order.builder()
                .productId(productId)
                .quantity(quantity)
                .status(OrderStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        Order orderSave = orderRepository.save(order);

        // EventOutbox저장을위한 객체
        OrderCreatedEvent event = new OrderCreatedEvent(
                orderSave.getId(), orderSave.getProductId(), orderSave.getQuantity(), orderSave.getCreatedAt());

        // Spring이벤트 발행 : BEFORE_COMMIT / AFTER_COMMIT리스너에서 각각 처리
        publisher.publishEvent(event);

        return orderSave.getId();
    }
}
```

###2. OutboxBeforeCommitListener

```java
@Component
@RequiredArgsConstructor
public class OutboxBeforeCommitListener {

    private final EventOutboxRepository outboxRepository;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void saveOutbox(OrderCreatedEvent event) {
        EventOutbox outbox = EventOutbox.builder()
                .aggregateType("Order")
                .aggregateId(event.getOrderId())
                .type("OrderCreated")
                .payload(toJson(event))
                .createdAt(LocalDateTime.now())
                .sent(false)
                .build();

        outboxRepository.save(outbox);
    }

    private String toJson(OrderCreatedEvent event) {
        return String.format(
                "{\"orderId\":%d,\"productId\":%d,\"quantity\":%d,\"createdAt\":\"%s\"}",
                event.getOrderId(), event.getProductId(), event.getQuantity(), event.getCreatedAt()
        );
    }
}
```

###3. OutboxAfterCommitListener + OutBoxPublishService

```java
@Component
@RequiredArgsConstructor
public class OutboxAfterCommitListener {

    private final OutBoxPublishService outBoxPublishService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publishToKafka(OrderCreatedEvent event) {
        outBoxPublishService.publish(event);
    }
}
@Slf4j
@RequiredArgsConstructor
@Service
public class OutBoxPublishService {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final EventOutboxRepository eventOutboxRepository;

    @Transactional
    public void publish(OrderCreatedEvent event) {
        //저장된 Outbox를 기준으로 Kafka발행
        Optional<EventOutbox> eventOutbox = eventOutboxRepository.findFirstByAggregateIdAndTypeAndSentFalse(
                event.getOrderId(), "OrderCreated");

        eventOutbox.ifPresent(outbox -> {
            kafkaTemplate.send("order.created", outbox.getPayload())
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            outbox.completeSent();
                            eventOutboxRepository.save(outbox);
                        } else {
                            //실패 로깅
                        }
                    });
        });
    }
}
```

###4. OutboxScheduler

```java
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxScheduler {

    private final EventOutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void publishPendingOutboxEvents() {
        List<EventOutbox> unsentEvents = outboxRepository.findAllBySentFalseAndType("OrderCreated");

        for (EventOutbox outbox : unsentEvents) {
            try {
                kafkaTemplate.send("order.created", outbox.getPayload());
                outbox.completeSent(); // sent = true변경
                log.info("스케줄러 재전송 성공: id={}, aggregateId={}", outbox.getId(), outbox.getAggregateId());
            } catch (Exception e) {
                log.error("Kafka전송 실패 (재시도 예정): id={}, error={}", outbox.getId(), e.getMessage());
            }
        }
    }
}
```

---

##트랜잭션 흐름 정리

-OrderService는 트랜잭션 내에서 Order 저장 + 이벤트 발행을 수행합니다.
-BEFORE_COMMIT 리스너에서 Outbox 테이블에 기록하고,
-AFTER_COMMIT 리스너에서 Kafka로 전송하며 전송 성공 시 sent 상태를 업데이트합니다.

---

##추가: Kafka에서 메시지 순서 보장방법

Kafka는 메시지 순서를**파티션 내부에서만**보장합니다.
따라서 아래와 같은 조건을 만족해야 순서 보장이 가능합니다:

-`KafkaTemplate.send(topic, key, value)` 형태로**동일한 키**를 지정하여 전송 (ex: 주문 ID, 사용자 ID, 상품 ID)
-해당 키는 항상**동일한 파티션에 매핑**됨
-해당 파티션의 메시지는 Kafka가 순서대로 유지

---

##마무리
>**시스템 규모, 메시지 유실 방지, 재처리 요구가 있는 경우**  Kafka + Outbox 패턴 고려 필요

