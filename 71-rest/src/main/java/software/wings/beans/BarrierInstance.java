package software.wings.beans;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.annotation.HarnessEntity;
import io.harness.iterator.PersistentRegularIterable;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UuidAware;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.UtilityClass;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Indexes;
import software.wings.beans.BarrierInstancePipeline.BarrierInstancePipelineKeys;
import software.wings.beans.BarrierInstanceWorkflow.BarrierInstanceWorkflowKeys;
import software.wings.beans.entityinterface.ApplicationAccess;

import java.time.OffsetDateTime;
import java.util.Date;
import javax.validation.constraints.NotNull;

@Indexes({
  @Index(options = @IndexOptions(name = "search2", unique = true),
      fields = { @Field("name")
                 , @Field("pipeline.executionId"), @Field("pipeline.parallelIndex") })
  ,
      @Index(options = @IndexOptions(name = "next"), fields = { @Field("state")
                                                                , @Field("nextIteration") })
})
@Data
@Builder
@EqualsAndHashCode(callSuper = false)
@FieldNameConstants(innerTypeName = "BarrierInstanceKeys")
@Entity(value = "barrierInstances", noClassnameStored = true)
@HarnessEntity(exportable = false)
public class BarrierInstance implements PersistentEntity, UuidAware, PersistentRegularIterable, ApplicationAccess {
  @Id private String uuid;
  @Indexed @NotNull protected String appId;

  private String name;
  @Indexed private String state;

  private Long nextIteration;

  @Override
  public Long obtainNextIteration(String fieldName) {
    return nextIteration;
  }

  @Override
  public void updateNextIteration(String fieldName, Long nextIteration) {
    this.nextIteration = nextIteration;
  }

  private BarrierInstancePipeline pipeline;

  @JsonIgnore
  @SchemaIgnore
  @Indexed(options = @IndexOptions(expireAfterSeconds = 0))
  @Builder.Default
  private Date validUntil = Date.from(OffsetDateTime.now().plusMonths(1).toInstant());

  @UtilityClass
  public static final class BarrierInstanceKeys {
    public static final String pipeline_executionId = pipeline + "." + BarrierInstancePipelineKeys.executionId;
    public static final String pipeline_parallelIndex = pipeline + "." + BarrierInstancePipelineKeys.parallelIndex;
    public static final String pipeline_workflows_pipelineStageId =
        pipeline + "." + BarrierInstancePipelineKeys.workflows + "." + BarrierInstanceWorkflowKeys.pipelineStageId;
  }
}
