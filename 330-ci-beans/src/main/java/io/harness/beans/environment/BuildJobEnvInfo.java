package io.harness.beans.environment;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Stores information for setting up environment for running  CI job
 */

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", defaultImpl = K8BuildJobEnvInfo.class)
@JsonSubTypes({ @JsonSubTypes.Type(value = K8BuildJobEnvInfo.class, name = "k8") })
public interface BuildJobEnvInfo {
  enum Type { K8 }

  Type getType();
}
