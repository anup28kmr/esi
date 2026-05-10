package ee.ut.melih.notificationservice.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true", matchIfMissing = true)
public class KafkaConfig {

  @Bean
  public NewTopic paymentEventsTopic() {
    return TopicBuilder.name(KafkaTopics.PAYMENT_EVENTS).partitions(1).replicas(1).build();
  }

  @Bean
  public NewTopic deliveryEventsTopic() {
    return TopicBuilder.name(KafkaTopics.DELIVERY_EVENTS).partitions(1).replicas(1).build();
  }

  @Bean
  public NewTopic orderEventsTopic() {
    return TopicBuilder.name(KafkaTopics.ORDER_EVENTS).partitions(1).replicas(1).build();
  }

  @Bean
  public NewTopic notificationDlqTopic() {
    return TopicBuilder.name(KafkaTopics.NOTIFICATION_DLQ).partitions(1).replicas(1).build();
  }
}
