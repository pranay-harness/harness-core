package io.harness.beans.yaml.extended.infrastrucutre;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonValue;
import org.springframework.data.annotation.TypeAlias;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", visible = true, defaultImpl = UseFromStageInfraYaml.class)
@JsonSubTypes({
  @JsonSubTypes.Type(value = K8sDirectInfraYaml.class, name = "KubernetesDirect")
  , @JsonSubTypes.Type(value = UseFromStageInfraYaml.class, name = "UseFromStage"),
      @JsonSubTypes.Type(value = AwsVmInfraYaml.class, name = "AwsVm")
})

public interface Infrastructure {
  @TypeAlias("infrastructure_type")
  enum Type {
    @JsonProperty("KubernetesDirect") KUBERNETES_DIRECT("KubernetesDirect"),
    @JsonProperty("UseFromStage") USE_FROM_STAGE("UseFromStage"),
    @JsonProperty("AwsVm") AWS_VM("AwsVm");

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
