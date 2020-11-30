package io.harness.beans.yaml.extended.infrastrucutre;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonValue;
import org.springframework.data.annotation.TypeAlias;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", visible = true, defaultImpl = UseFromStageInfraYaml.class)
@JsonSubTypes({
  @JsonSubTypes.Type(value = K8sDirectInfraYaml.class, name = "kubernetes-direct")
  , @JsonSubTypes.Type(value = K8sGCPInfraYaml.class, name = "kubernetes-gcp"),
      @JsonSubTypes.Type(value = UseFromStageInfraYaml.class, name = "useFromStageInfraYaml")
})

public interface Infrastructure {
  @TypeAlias("infrastructure_type")
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
