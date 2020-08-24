package io.harness.serializer.morphia;

import io.harness.connector.entities.Connector;
import io.harness.connector.entities.embedded.appdynamicsconnector.AppDynamicsConnector;
import io.harness.connector.entities.embedded.docker.DockerConnector;
import io.harness.connector.entities.embedded.gitconnector.GitConfig;
import io.harness.connector.entities.embedded.gitconnector.GitSSHAuthentication;
import io.harness.connector.entities.embedded.gitconnector.GitUserNamePasswordAuthentication;
import io.harness.connector.entities.embedded.kubernetescluster.K8sClientKeyCert;
import io.harness.connector.entities.embedded.kubernetescluster.K8sOpenIdConnect;
import io.harness.connector.entities.embedded.kubernetescluster.K8sServiceAccount;
import io.harness.connector.entities.embedded.kubernetescluster.K8sUserNamePassword;
import io.harness.connector.entities.embedded.kubernetescluster.KubernetesClusterConfig;
import io.harness.connector.entities.embedded.kubernetescluster.KubernetesClusterDetails;
import io.harness.connector.entities.embedded.kubernetescluster.KubernetesDelegateDetails;
import io.harness.connector.entities.embedded.splunkconnector.SplunkConnector;
import io.harness.connector.entities.embedded.vaultconnector.VaultConnector;
import io.harness.morphia.MorphiaRegistrar;
import io.harness.morphia.MorphiaRegistrarHelperPut;

import java.util.Set;

public class ConnectorMorphiaClassesRegistrar implements MorphiaRegistrar {
  @Override
  public void registerClasses(Set<Class> set) {
    set.add(Connector.class);
    set.add(KubernetesClusterConfig.class);
    set.add(GitConfig.class);
    set.add(VaultConnector.class);
    set.add(AppDynamicsConnector.class);
    set.add(SplunkConnector.class);
    set.add(DockerConnector.class);
  }

  @Override
  public void registerImplementationClasses(MorphiaRegistrarHelperPut h, MorphiaRegistrarHelperPut w) {
    h.put("connector.entities.embedded.kubernetescluster.KubernetesDelegateDetails", KubernetesDelegateDetails.class);
    h.put("connector.entities.embedded.kubernetescluster.KubernetesClusterDetails", KubernetesClusterDetails.class);
    h.put("connector.entities.embedded.kubernetescluster.K8sClientKeyCert", K8sClientKeyCert.class);
    h.put("connector.entities.embedded.kubernetescluster.K8sOpenIdConnect", K8sOpenIdConnect.class);
    h.put("connector.entities.embedded.kubernetescluster.K8sServiceAccount", K8sServiceAccount.class);
    h.put("connector.entities.embedded.kubernetescluster.K8sUserNamePassword", K8sUserNamePassword.class);
    h.put("connector.entities.embedded.gitconnector.GitSSHAuthentication", GitSSHAuthentication.class);
    h.put("connector.entities.embedded.gitconnector.GitUserNamePasswordAuthentication",
        GitUserNamePasswordAuthentication.class);
  }
}
