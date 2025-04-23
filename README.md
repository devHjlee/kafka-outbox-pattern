# kafka-outbox-pattern
DB 트랜잭션과 Kafka 메시지 발행의 원자성을 보장해 데이터 정합성을 유지하는 이벤트 기반 패턴



bin/kafka-topics.sh \
--create \
--topic order.created \
--bootstrap-server localhost:9092 \
--partitions 3 \
--replication-factor 1


bin/kafka-topics.sh --list --bootstrap-server localhost:9092
