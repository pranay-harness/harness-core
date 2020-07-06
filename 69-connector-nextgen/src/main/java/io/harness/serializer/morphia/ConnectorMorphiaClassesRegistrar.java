package io.harness.serializer.morphia;

import io.harness.connector.entities.Connector;
import io.harness.connector.entities.connectorTypes.kubernetesCluster.ClientKeyCertK8;
import io.harness.connector.entities.connectorTypes.kubernetesCluster.KubernetesClusterConfig;
import io.harness.connector.entities.connectorTypes.kubernetesCluster.KubernetesClusterDetails;
import io.harness.connector.entities.connectorTypes.kubernetesCluster.KubernetesDelegateDetails;
import io.harness.connector.entities.connectorTypes.kubernetesCluster.OpenIdConnectK8;
import io.harness.connector.entities.connectorTypes.kubernetesCluster.ServiceAccountK8;
import io.harness.connector.entities.connectorTypes.kubernetesCluster.UserNamePasswordK8;
import io.harness.morphia.MorphiaRegistrarHelperPut;

import java.util.Set;

public class ConnectorMorphiaClassesRegistrar implements io.harness.morphia.MorphiaRegistrar {
  @Override
  public void registerClasses(Set<Class> set) {
    set.add(Connector.class);
    set.add(KubernetesClusterConfig.class);
  }

  @Override
  public void registerImplementationClasses(MorphiaRegistrarHelperPut h, MorphiaRegistrarHelperPut w) {
    h.put("connector.entities.connectorTypes.kubernetesCluster.KubernetesDelegateDetails",
        KubernetesDelegateDetails.class);
    h.put(
        "connector.entities.connectorTypes.kubernetesCluster.KubernetesClusterDetails", KubernetesClusterDetails.class);
    h.put("connector.entities.connectorTypes.kubernetesCluster.ClientKeyCertK8", ClientKeyCertK8.class);
    h.put("connector.entities.connectorTypes.kubernetesCluster.OpenIdConnectK8", OpenIdConnectK8.class);
    h.put("connector.entities.connectorTypes.kubernetesCluster.ServiceAccountK8", ServiceAccountK8.class);
    h.put("connector.entities.connectorTypes.kubernetesCluster.UserNamePasswordK8", UserNamePasswordK8.class);
  }
}
