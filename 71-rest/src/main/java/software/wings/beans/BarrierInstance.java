package software.wings.beans;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.reinert.jjschema.SchemaIgnore;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Indexes;

import java.time.OffsetDateTime;
import java.util.Date;
import java.util.List;

@Entity(value = "barrierInstances", noClassnameStored = true)
@Indexes({
  @Index(options = @IndexOptions(name = "search", unique = true), fields = {
    @Field("name"), @Field("pipeline.executionId")
  })
})
@EqualsAndHashCode(callSuper = false)
@Data
@Builder
public class BarrierInstance extends Base {
  public static final String NAME_KEY = "name";
  public static final String STATE_KEY = "state";
  public static final String PIPELINE_EXECUTION_ID_KEY = "pipeline.executionId";
  public static final String PIPELINE_WORKFLOWS_PIPELINE_STATE_ID_KEY = "pipeline.workflows.pipelineStateId";

  private String name;
  @Indexed private String state;

  @Data
  @Builder
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
  public static class Pipeline {
    private String executionId;
    List<Workflow> workflows;
  }

  private Pipeline pipeline;

  @SchemaIgnore
  @JsonIgnore
  @Indexed(options = @IndexOptions(expireAfterSeconds = 0))
  @Builder.Default
  private Date validUntil = Date.from(OffsetDateTime.now().plusMonths(1).toInstant());
}
