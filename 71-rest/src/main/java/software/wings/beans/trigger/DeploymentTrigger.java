package software.wings.beans.trigger;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static software.wings.scheduler.ScheduledTriggerJob.PREFIX;

import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.annotation.HarnessExportableEntity;
import io.harness.beans.EmbeddedUser;
import io.harness.data.validator.EntityName;
import io.harness.data.validator.Trimmed;
import io.harness.iterator.PersistentCronIterable;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.CreatedByAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UpdatedByAware;
import io.harness.persistence.UuidAware;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Indexes;
import software.wings.beans.trigger.Condition.Type;

import java.util.ArrayList;
import java.util.List;
import javax.validation.constraints.NotNull;

/**
 * Created by sgurubelli on 10/25/17.
 */

@Entity(value = "deploymentTriggers", noClassnameStored = true)
@Data
@Builder
@AllArgsConstructor
@Indexes({
  @Index(
      options = @IndexOptions(name = "uniqueTriggerIdx", unique = true), fields = { @Field("appId")
                                                                                    , @Field("name") })
  ,
      @Index(options = @IndexOptions(name = "uniqueTypeIdx"), fields = {
        @Field("accountId"), @Field("appId"), @Field("type")
      }), @Index(options = @IndexOptions(name = "iterations"), fields = { @Field("type")
                                                                          , @Field("nextIterations") })
})
@HarnessExportableEntity
@FieldNameConstants(innerTypeName = "DeploymentTriggerKeys")
public class DeploymentTrigger implements PersistentEntity, UuidAware, CreatedAtAware, CreatedByAware, UpdatedAtAware,
                                          UpdatedByAware, PersistentCronIterable {
  @Id @NotNull(groups = {DeploymentTrigger.class}) @SchemaIgnore private String uuid;
  @NotNull protected String appId;
  @Indexed protected String accountId;
  @SchemaIgnore private EmbeddedUser createdBy;
  @SchemaIgnore @Indexed private long createdAt;

  @SchemaIgnore private EmbeddedUser lastUpdatedBy;
  @SchemaIgnore @NotNull private long lastUpdatedAt;

  @EntityName @NotEmpty @Trimmed private String name;
  private String description;
  private boolean triggerEnabled = true;

  private List<Long> nextIterations;
  private Action action;
  @NotNull private Condition condition;
  private Type type;

  @Override
  public boolean skipMissed() {
    return true;
  }

  @Override
  public List<Long> recalculateNextIterations(String fieldName) {
    if (nextIterations == null) {
      nextIterations = new ArrayList<>();
    }

    ScheduledCondition scheduledCondition = (ScheduledCondition) condition;
    if (expandNextIterations(PREFIX + scheduledCondition.getCronExpression(), nextIterations)) {
      return nextIterations;
    }

    return null;
  }

  @Override
  public Long obtainNextIteration(String fieldName) {
    return isEmpty(nextIterations) ? null : nextIterations.get(0);
  }
}
