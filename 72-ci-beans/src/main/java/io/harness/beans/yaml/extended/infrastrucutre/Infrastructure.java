package io.harness.beans.yaml.extended.infrastrucutre;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonValue;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", visible = true, defaultImpl = UseFromStageInfraYaml.class)
public interface Infrastructure {
  enum Type {
    KUBERNETES_DIRECT("kubernetes-direct"),
    KUBERNETES_GCP("kubernetes-gcp"),
    USE_FROM_STAGE("use-from-stage");

    private final String yamlName;

    Type(String yamlName) {
      this.yamlName = yamlName;
    }

    @JsonValue
    public String getYamlName() {
      return yamlName;
    }
  }
  Type getType();
}
