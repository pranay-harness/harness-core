package software.wings.beans;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UuidAware;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Indexes;
import software.wings.beans.BarrierInstance.Pipeline.PipelineKeys;
import software.wings.beans.BarrierInstance.Workflow.WorkflowKeys;

import java.time.OffsetDateTime;
import java.util.Date;
import java.util.List;
import javax.validation.constraints.NotNull;

@Entity(value = "barrierInstances", noClassnameStored = true)
@Indexes({
  @Index(options = @IndexOptions(name = "search", unique = true), fields = {
    @Field("name"), @Field("pipeline.executionId")
  })
})
@Data
@Builder
@EqualsAndHashCode(callSuper = false)
@FieldNameConstants(innerTypeName = "BarrierInstanceKeys")
public class BarrierInstance implements PersistentEntity, UuidAware {
  @Id private String uuid;
  @Indexed @NotNull protected String appId;

  private String name;
  @Indexed private String state;

  @Data
  @Builder
  @FieldNameConstants(innerTypeName = "WorkflowKeys")
  public static class Workflow {
    private String uuid;

    private String pipelineStateId;
    private String pipelineStateExecutionId;

    private String workflowExecutionId;

    private String phaseUuid;
    private String phaseExecutionId;

    private String stepUuid;
    private String stepExecutionId;
  }

  @Value
  @Builder
  @FieldNameConstants(innerTypeName = "PipelineKeys")
  public static class Pipeline {
    private String executionId;
    List<Workflow> workflows;
  }

  private Pipeline pipeline;

  @JsonIgnore
  @SchemaIgnore
  @Indexed(options = @IndexOptions(expireAfterSeconds = 0))
  @Builder.Default
  private Date validUntil = Date.from(OffsetDateTime.now().plusMonths(1).toInstant());

  public static final class BarrierInstanceKeys {
    public static final String pipeline_executionId = pipeline + "." + PipelineKeys.executionId;
    public static final String pipeline_workflows_pipelineStateId =
        pipeline + "." + PipelineKeys.workflows + "." + WorkflowKeys.pipelineStateId;
  }
}
