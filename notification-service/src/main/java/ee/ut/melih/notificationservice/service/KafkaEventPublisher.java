package ee.ut.melih.notificationservice.service;

import ee.ut.melih.notificationservice.dto.EventEnvelope;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true", matchIfMissing = true)
public class KafkaEventPublisher {

  private final KafkaTemplate<String, EventEnvelope> template;

  public KafkaEventPublisher(KafkaTemplate<String, EventEnvelope> template) {
    this.template = template;
  }

  public void publish(String topic, EventEnvelope event) {
    template.send(topic, event.id().toString(), event);
  }
}
