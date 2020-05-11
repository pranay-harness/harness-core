package software.wings.beans.trigger;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.annotations.dev.OwnedBy;
import lombok.Builder;
import lombok.Value;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.NotNull;

@OwnedBy(CDC)
@JsonTypeName("PIPELINE")
@Value
@Builder
public class PipelineCondition implements Condition {
  @NotEmpty private String pipelineId;

  @NotNull private Type type = Type.PIPELINE_COMPLETION;

  private transient String pipelineName;
}
