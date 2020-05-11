package software.wings.beans.trigger;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static software.wings.scheduler.ScheduledTriggerJob.PREFIX;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.annotation.HarnessEntity;
import io.harness.annotations.dev.OwnedBy;
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
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Indexes;
import software.wings.beans.HarnessTagLink;
import software.wings.beans.entityinterface.ApplicationAccess;
import software.wings.beans.entityinterface.TagAware;
import software.wings.beans.trigger.Condition.Type;

import java.util.ArrayList;
import java.util.List;
import javax.validation.constraints.NotNull;

/**
 * Created by sgurubelli on 10/25/17.
 */

@OwnedBy(CDC)
@Data
@Builder
@AllArgsConstructor
@Indexes({
  @Index(
      options = @IndexOptions(name = "uniqueTriggerIdx", unique = true), fields = { @Field("appId")
                                                                                    , @Field("name") })
  ,
      @Index(options = @IndexOptions(name = "tokenIdx"), fields = { @Field("accountId")
                                                                    , @Field("token") }),
      @Index(options = @IndexOptions(name = "uniqueTypeIdx"), fields = {
        @Field("accountId"), @Field("appId"), @Field("type")
      }), @Index(options = @IndexOptions(name = "iterations"), fields = { @Field("type")
                                                                          , @Field("nextIterations") })
})
@FieldNameConstants(innerTypeName = "DeploymentTriggerKeys")
@Entity(value = "deploymentTriggers", noClassnameStored = true)
@HarnessEntity(exportable = true)
@Slf4j
public class DeploymentTrigger implements PersistentEntity, UuidAware, CreatedAtAware, CreatedByAware, UpdatedAtAware,
                                          UpdatedByAware, PersistentCronIterable, TagAware, ApplicationAccess {
  @Id @NotNull(groups = {DeploymentTrigger.class}) @SchemaIgnore private String uuid;
  @NotNull protected String appId;
  @Indexed protected String accountId;
  @SchemaIgnore private EmbeddedUser createdBy;
  @SchemaIgnore @Indexed private long createdAt;

  @SchemaIgnore private EmbeddedUser lastUpdatedBy;
  @SchemaIgnore @NotNull private long lastUpdatedAt;

  @EntityName @NotEmpty @Trimmed private String name;
  private String description;
  private boolean triggerDisabled;
  private boolean triggerInvalid;
  private String errorMsg;

  private List<Long> nextIterations;
  private Action action;
  @NotNull private Condition condition;
  private Type type;
  @JsonIgnore private String webHookToken;
  private transient List<HarnessTagLink> tagLinks;

  @Override
  public List<Long> recalculateNextIterations(String fieldName, boolean skipMissing, long throttled) {
    if (nextIterations == null) {
      nextIterations = new ArrayList<>();
    }

    try {
      ScheduledCondition scheduledCondition = (ScheduledCondition) condition;
      if (expandNextIterations(
              skipMissing, throttled, PREFIX + scheduledCondition.getCronExpression(), nextIterations)) {
        return nextIterations;
      }
    } catch (Exception ex) {
      logger.error("Failed to schedule trigger {}", name, ex);
    }

    return null;
  }

  @Override
  public Long obtainNextIteration(String fieldName) {
    return isEmpty(nextIterations) ? null : nextIterations.get(0);
  }
}
