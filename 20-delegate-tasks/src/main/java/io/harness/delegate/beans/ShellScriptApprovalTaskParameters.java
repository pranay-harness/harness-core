package io.harness.delegate.beans;

import io.harness.delegate.task.protocol.TaskParameters;
import io.harness.expression.Expression;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ShellScriptApprovalTaskParameters implements TaskParameters {
  private String accountId;
  private String appId;
  private String activityId;
  private String commandName;

  private io.harness.delegate.beans.ScriptType scriptType;
  private final String outputVars;
  @Expression private final String script;
  private String workingDirectory;
}
