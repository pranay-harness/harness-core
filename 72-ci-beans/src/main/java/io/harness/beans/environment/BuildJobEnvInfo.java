package io.harness.beans.environment;

public interface BuildJobEnvInfo {
  enum Type { K8 }

  Type getType();
}
