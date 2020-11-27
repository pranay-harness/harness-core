package io.harness.serializer.spring;

import io.harness.advisers.fail.OnFailAdviserParameters;
import io.harness.advisers.ignore.IgnoreAdviserParameters;
import io.harness.advisers.retry.RetryAdviserParameters;
import io.harness.advisers.success.OnSuccessAdviserParameters;
import io.harness.engine.executions.node.NodeExecutionTimeoutCallback;
import io.harness.spring.AliasRegistrar;

import java.util.Map;

/**
 * DO NOT CHANGE the keys. This is how track the Interface Implementations
 */
public class OrchestrationAliasRegistrar implements AliasRegistrar {
  @Override
  public void register(Map<String, Class<?>> orchestrationElements) {
    orchestrationElements.put("ignoreAdviserParameters", IgnoreAdviserParameters.class);
    orchestrationElements.put("onFailAdviserParameters", OnFailAdviserParameters.class);
    orchestrationElements.put("onSuccessAdviserParameters", OnSuccessAdviserParameters.class);
    orchestrationElements.put("retryAdviserParameters", RetryAdviserParameters.class);
    orchestrationElements.put("nodeExecutionTimeoutCallback", NodeExecutionTimeoutCallback.class);
  }
}
