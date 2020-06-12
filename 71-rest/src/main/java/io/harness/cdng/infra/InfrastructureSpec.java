package io.harness.cdng.infra;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import io.harness.cdng.infra.yaml.Infrastructure;
import io.harness.cdng.infra.yaml.K8SDirectInfrastructure;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.NonFinal;

import java.io.Serializable;

@Value
@Builder
public class InfrastructureSpec implements Serializable {
  @NonFinal @JsonIgnore private Infrastructure infrastructure;

  @JsonSetter("kubernetesDirect")
  public void setK8sDirectInfra(K8SDirectInfrastructure k8sDirectInfra) {
    this.infrastructure = k8sDirectInfra;
  }
}
