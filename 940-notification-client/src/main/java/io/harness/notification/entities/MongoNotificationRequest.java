package io.harness.notification.entities;

import io.harness.queue.Queuable;

import lombok.*;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Document(collection = "notification-requests")
public class MongoNotificationRequest extends Queuable {
  byte[] bytes;
}
