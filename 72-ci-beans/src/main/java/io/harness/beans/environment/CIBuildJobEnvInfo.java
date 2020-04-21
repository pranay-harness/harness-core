package io.harness.beans.environment;

public interface CIBuildJobEnvInfo {
  enum Type { K8 }

  Type getType();
}
