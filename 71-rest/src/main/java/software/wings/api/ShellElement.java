package software.wings.api;

import io.harness.context.ContextElementType;
import io.harness.delegate.task.shell.ScriptType;
import lombok.Builder;
import lombok.Data;
import software.wings.sm.ContextElement;
import software.wings.sm.ExecutionContext;

import java.util.HashMap;
import java.util.Map;

@Data
@Builder
public class ShellElement implements ContextElement {
  private String uuid;
  ScriptType scriptType;
  String name;

  @Override
  public ContextElementType getElementType() {
    return ContextElementType.SHELL;
  }

  @Override
  public String getUuid() {
    return uuid;
  }

  @Override
  public ContextElement cloneMin() {
    return this;
  }

  @Override
  public Map<String, Object> paramMap(ExecutionContext context) {
    return new HashMap<>();
  }
}
