package io.harness.delegate.beans.executioncapability;

public interface ExecutionCapability {
  enum EvaluationMode { MANAGER, AGENT }

  EvaluationMode evaluationMode();

  CapabilityType getCapabilityType();
  String fetchCapabilityBasis();
}
