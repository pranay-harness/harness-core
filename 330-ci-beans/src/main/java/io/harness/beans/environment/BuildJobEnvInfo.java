package io.harness.beans.environment;

/**
 * Stores information for setting up environment for running  CI job
 */

public interface BuildJobEnvInfo {
  enum Type { K8 }

  Type getType();
}
