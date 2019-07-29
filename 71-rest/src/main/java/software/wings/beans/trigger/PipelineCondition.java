package software.wings.beans.trigger;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Value;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.NotNull;

@JsonTypeName("PIPELINE")
@Value
@Builder
public class PipelineCondition implements Condition {
  @NotEmpty private String pipelineId;

  @NotNull private Type type = Type.PIPELINE_COMPLETION;

  private transient String pipelineName;
}
