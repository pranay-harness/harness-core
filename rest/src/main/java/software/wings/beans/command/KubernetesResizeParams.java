package software.wings.beans.command;

import io.fabric8.kubernetes.api.model.HasMetadata;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.api.ContainerServiceData;

import java.util.ArrayList;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class KubernetesResizeParams extends ContainerResizeParams {
  private String kubernetesType;
  private String namespace;

  public static final class KubernetesResizeParamsBuilder {
    private String clusterName;
    private List<ContainerServiceData> desiredCounts = new ArrayList<>();
    private String kubernetesType;
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

    public KubernetesResizeParamsBuilder withKubernetesType(String kubernetesType) {
      this.kubernetesType = kubernetesType;
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
          .withKubernetesType(kubernetesType)
          .withNamespace(namespace);
    }

    public KubernetesResizeParams build() {
      KubernetesResizeParams kubernetesResizeParams = new KubernetesResizeParams();
      kubernetesResizeParams.setClusterName(clusterName);
      kubernetesResizeParams.setDesiredCounts(desiredCounts);
      kubernetesResizeParams.setKubernetesType(kubernetesType);
      kubernetesResizeParams.setNamespace(namespace);
      return kubernetesResizeParams;
    }
  }
}
