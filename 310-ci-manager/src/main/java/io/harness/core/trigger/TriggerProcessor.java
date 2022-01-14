package io.harness.core.trigger;

public interface TriggerProcessor {
  void validateTriggerCondition();

  void validateTriggerAction();
}
