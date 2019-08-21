package software.wings.api;

import com.google.common.collect.Maps;

import io.harness.context.ContextElementType;
import lombok.Builder;
import software.wings.sm.ContextElement;
import software.wings.sm.ExecutionContext;

import java.util.HashMap;
import java.util.Map;

@Builder
public class ShellScriptProvisionerOutputElement implements ContextElement {
  public static String KEY = "shellScriptProvisioner";
  private Map<String, Object> outputVariables;

  @Override
  public ContextElementType getElementType() {
    return ContextElementType.SHELL_SCRIPT_PROVISION;
  }

  @Override
  public String getUuid() {
    return null;
  }

  @Override
  public String getName() {
    return null;
  }

  @Override
  public Map<String, Object> paramMap(ExecutionContext context) {
    HashMap<String, Object> paramMap = Maps.newHashMap();
    paramMap.put(KEY, outputVariables);
    return paramMap;
  }

  @Override
  public ContextElement cloneMin() {
    return null;
  }

  public void addOutPuts(Map<String, Object> newMap) {
    if (outputVariables == null) {
      outputVariables = new HashMap<>();
    }
    outputVariables.putAll(newMap);
  }
}
