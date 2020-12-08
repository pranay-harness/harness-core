package io.harness.delegate.task.stepstatus;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;

@Data
@Builder
@JsonTypeName("stepMapOutput")
public class StepMapOutput implements StepOutput {
  @Singular("output") Map<String, String> map;
}
