package io.harness.delegate.beans.ci.vm;

import io.harness.delegate.beans.DelegateMetaInfo;
import io.harness.delegate.beans.ci.CITaskExecutionResponse;
import io.harness.logging.CommandExecutionStatus;

import java.util.Map;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class VmTaskExecutionResponse implements CITaskExecutionResponse {
  private DelegateMetaInfo delegateMetaInfo;
  private String errorMessage;
  private String ipAddress;
  private Map<String, String> outputVars;
  private CommandExecutionStatus commandExecutionStatus;
  @Builder.Default private static final CITaskExecutionResponse.Type type = Type.VM;

  @Override
  public CITaskExecutionResponse.Type getType() {
    return type;
  }
}