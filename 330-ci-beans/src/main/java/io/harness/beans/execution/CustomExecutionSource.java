package io.harness.beans.execution;

import static io.harness.beans.execution.ExecutionSource.Type.CUSTOM;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder
@JsonTypeName("Custom")
@TypeAlias("CUSTOM")
public class CustomExecutionSource implements ExecutionSource {
  private String branch;
  private String tag;

  @Override
  public Type getType() {
    return CUSTOM;
  }
}
