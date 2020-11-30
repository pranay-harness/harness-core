package io.harness.beans.execution;

import static io.harness.beans.execution.ExecutionSource.Type.MANUAL;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder
@JsonTypeName("Manual")
@TypeAlias("MANUAL")
public class ManualExecutionSource implements ExecutionSource {
  private String branch;
  private String tag;

  @Override
  public Type getType() {
    return MANUAL;
  }
}
