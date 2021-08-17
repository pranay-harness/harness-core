package software.wings.beans.infrastructure.instance;

import io.harness.iterator.PersistentRegularIterable;
import io.harness.mongo.index.CompoundMongoIndex;
import io.harness.mongo.index.MongoIndex;
import io.harness.persistence.PersistentEntity;

import com.google.common.collect.ImmutableList;
import java.util.List;
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
  public static List<MongoIndex> mongoIndexes() {
    return ImmutableList.<MongoIndex>builder()
        .add(CompoundMongoIndex.builder()
                 .name("perpetualTaskScheduleConfig_index1")
                 .field(PerpetualTaskScheduleConfigKeys.accountId)
                 .field(PerpetualTaskScheduleConfigKeys.perpetualTaskType)
                 .build())
        .build();
  }
  private String accountId;
  private int timeInterval;
  private String perpetualTaskType;
}
