package io.harness.perpetualtask;

import io.harness.persistence.PersistentEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity(value = "perpetualTask", noClassnameStored = true)
public class PerpetualTaskRecord implements PersistentEntity {
  @Id private String taskId;
  private String clientName;
  private String clientHandle; // unique identifier known to client
  private long interval; // unit: second
  private long timeout; // unit: millisecond
  private String delegateId;
}
