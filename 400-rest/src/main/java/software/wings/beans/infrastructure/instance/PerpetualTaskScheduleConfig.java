package software.wings.beans.infrastructure.instance;

import io.harness.iterator.PersistentRegularIterable;
import io.harness.persistence.PersistentEntity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.UtilityClass;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldNameConstants(innerTypeName = "PerpetualTaskScheduleConfigKeys")
@Entity(value = "perpetualTaskScheduleConfig", noClassnameStored = true)
public class PerpetualTaskScheduleConfig implements PersistentEntity {
  @Id private String id;
  private String accountId;
  private int timeInterval;
  private String perpetualTaskType;
}
