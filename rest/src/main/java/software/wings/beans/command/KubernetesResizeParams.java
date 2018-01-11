package software.wings.beans.command;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.api.ContainerServiceData;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@EqualsAndHashCode(callSuper = true)
public class KubernetesResizeParams extends ContainerResizeParams {
  private String namespace;

  public static final class KubernetesResizeParamsBuilder {
    private String clusterName;
    private List<ContainerServiceData> desiredCounts = new ArrayList<>();
    private int serviceSteadyStateTimeout;
    private String namespace;

    private KubernetesResizeParamsBuilder() {}

    public static KubernetesResizeParamsBuilder aKubernetesResizeParams() {
      return new KubernetesResizeParamsBuilder();
    }

    public KubernetesResizeParamsBuilder withClusterName(String clusterName) {
      this.clusterName = clusterName;
      return this;
    }

    public KubernetesResizeParamsBuilder withDesiredCounts(List<ContainerServiceData> desiredCounts) {
      this.desiredCounts = desiredCounts;
      return this;
    }

    public KubernetesResizeParamsBuilder withServiceSteadyStateTimeout(int serviceSteadyStateTimeout) {
      this.serviceSteadyStateTimeout = serviceSteadyStateTimeout;
      return this;
    }

    public KubernetesResizeParamsBuilder withNamespace(String namespace) {
      this.namespace = namespace;
      return this;
    }

    public KubernetesResizeParamsBuilder but() {
      return aKubernetesResizeParams()
          .withClusterName(clusterName)
          .withDesiredCounts(desiredCounts)
          .withServiceSteadyStateTimeout(serviceSteadyStateTimeout)
          .withNamespace(namespace);
    }

    public KubernetesResizeParams build() {
      KubernetesResizeParams kubernetesResizeParams = new KubernetesResizeParams();
      kubernetesResizeParams.setClusterName(clusterName);
      kubernetesResizeParams.setDesiredCounts(desiredCounts);
      kubernetesResizeParams.setServiceSteadyStateTimeout(serviceSteadyStateTimeout);
      kubernetesResizeParams.setNamespace(namespace);
      return kubernetesResizeParams;
    }
  }
}
