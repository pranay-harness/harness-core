package io.harness.data;

import io.harness.ambiance.LevelExecution;
import io.harness.persistence.CreatedAtAccess;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UuidAccess;
import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexes;

import java.util.List;

@Value
@Builder
@FieldNameConstants(innerTypeName = "OutcomeInstanceKeys")
@Indexes({
  @Index(options = @IndexOptions(name = "uniqueLevelExecution", unique = true),
      fields = { @Field("planExecutionId")
                 , @Field("levelExecutions.runtimeId"), @Field("name") })
  ,
      @Index(options = @IndexOptions(name = "planExecutionIdx"), fields = { @Field("ambiance.planExecutionId") })
})
@Entity(value = "outcomeInstances", noClassnameStored = true)
public class OutcomeInstance implements PersistentEntity, UuidAccess, CreatedAtAccess {
  @Id String uuid;
  @NonNull String planExecutionId;
  @Singular List<LevelExecution> levelExecutions;
  @NonNull String name;
  Outcome outcome;
  long createdAt;
}
