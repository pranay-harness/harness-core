package software.wings.beans;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import com.google.common.collect.ImmutableList;

import io.harness.annotation.HarnessEntity;
import io.harness.annotations.dev.OwnedBy;
import io.harness.distribution.constraint.Consumer.State;
import io.harness.iterator.PersistentRegularIterable;
import io.harness.mongo.index.Field;
import io.harness.mongo.index.Index;
import io.harness.mongo.index.TtlIndex;
import io.harness.mongo.index.UniqueIndex;
import io.harness.persistence.AccountAccess;
import io.harness.persistence.UuidAware;
import io.harness.validation.Update;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import software.wings.beans.ResourceConstraintInstance.ResourceConstraintInstanceKeys;

import java.time.OffsetDateTime;
import java.util.Date;
import java.util.List;
import javax.validation.constraints.NotNull;

@OwnedBy(CDC)
@Data
@Builder
@EqualsAndHashCode(callSuper = false)
@UniqueIndex(name = "uniqueUnitOrder",
    fields =
    {
      @Field(ResourceConstraintInstanceKeys.resourceConstraintId)
      , @Field(ResourceConstraintInstanceKeys.resourceUnit), @Field(ResourceConstraintInstanceKeys.order),
    })
@Index(name = "usageIndex",
    fields =
    { @Field(ResourceConstraintInstanceKeys.resourceConstraintId)
      , @Field(ResourceConstraintInstanceKeys.order), })
@Index(name = "iterationIndex",
    fields = { @Field(ResourceConstraintInstanceKeys.state)
               , @Field(ResourceConstraintInstanceKeys.nextIteration), })
@Index(name = "app_release_entity",
    fields =
    {
      @Field(ResourceConstraintInstanceKeys.appId)
      , @Field(ResourceConstraintInstanceKeys.releaseEntityType), @Field(ResourceConstraintInstanceKeys.releaseEntityId)
    })
@FieldNameConstants(innerTypeName = "ResourceConstraintInstanceKeys")
@Entity(value = "resourceConstraintInstances", noClassnameStored = true)
@HarnessEntity(exportable = false)
public class ResourceConstraintInstance implements PersistentRegularIterable, UuidAware, AccountAccess {
  public static final List<String> NOT_FINISHED_STATES =
      ImmutableList.<String>builder().add(State.ACTIVE.name()).add(State.BLOCKED.name()).build();

  @Id @NotNull(groups = {Update.class}) private String uuid;
  @NotNull protected String appId;

  private String accountId;

  private String resourceConstraintId;
  private String resourceUnit;
  private int order;

  private String state;
  private int permits;

  private String releaseEntityType;
  private String releaseEntityId;

  private long acquiredAt;

  private Long nextIteration;

  @Default @TtlIndex private Date validUntil = Date.from(OffsetDateTime.now().plusMonths(1).toInstant());

  @Override
  public void updateNextIteration(String fieldName, Long nextIteration) {
    this.nextIteration = nextIteration;
  }

  @Override
  public Long obtainNextIteration(String fieldName) {
    return nextIteration;
  }
}
