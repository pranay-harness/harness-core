package software.wings.service.impl.instance.sync.request;

import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.beans.KubernetesConfig;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class KubernetesFilter extends ContainerFilter {
  private KubernetesConfig kubernetesConfig;
  private List<String> replicationControllerNameList;

  public static final class Builder {
    protected String clusterName;
    private KubernetesConfig kubernetesConfig;
    private List<String> replicationControllerNameList;

    private Builder() {}

    public static Builder aKubernetesFilter() {
      return new Builder();
    }

    public Builder withClusterName(String clusterName) {
      this.clusterName = clusterName;
      return this;
    }

    public Builder withKubernetesConfig(KubernetesConfig kubernetesConfig) {
      this.kubernetesConfig = kubernetesConfig;
      return this;
    }

    public Builder withReplicationControllerNameList(List<String> replicationControllerNameList) {
      this.replicationControllerNameList = replicationControllerNameList;
      return this;
    }

    public Builder but() {
      return aKubernetesFilter()
          .withClusterName(clusterName)
          .withKubernetesConfig(kubernetesConfig)
          .withReplicationControllerNameList(replicationControllerNameList);
    }

    public KubernetesFilter build() {
      KubernetesFilter kubernetesFilter = new KubernetesFilter();
      kubernetesFilter.setClusterName(clusterName);
      kubernetesFilter.setKubernetesConfig(kubernetesConfig);
      kubernetesFilter.setReplicationControllerNameList(replicationControllerNameList);
      return kubernetesFilter;
    }
  }
}
