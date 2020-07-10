package io.harness.serializer.morphia;

import io.harness.connector.entities.Connector;
import io.harness.connector.entities.embedded.gitconnector.GitConfig;
import io.harness.connector.entities.embedded.gitconnector.GitSSHAuthentication;
import io.harness.connector.entities.embedded.gitconnector.UserNamePasswordGitAuthentication;
import io.harness.connector.entities.embedded.kubernetescluster.ClientKeyCertK8;
import io.harness.connector.entities.embedded.kubernetescluster.KubernetesClusterConfig;
import io.harness.connector.entities.embedded.kubernetescluster.KubernetesClusterDetails;
import io.harness.connector.entities.embedded.kubernetescluster.KubernetesDelegateDetails;
import io.harness.connector.entities.embedded.kubernetescluster.OpenIdConnectK8;
import io.harness.connector.entities.embedded.kubernetescluster.ServiceAccountK8;
import io.harness.connector.entities.embedded.kubernetescluster.UserNamePasswordK8;
import io.harness.morphia.MorphiaRegistrarHelperPut;

import java.util.Set;

public class ConnectorMorphiaClassesRegistrar implements io.harness.morphia.MorphiaRegistrar {
  @Override
  public void registerClasses(Set<Class> set) {
    set.add(Connector.class);
    set.add(KubernetesClusterConfig.class);
    set.add(GitConfig.class);
  }

  @Override
  public void registerImplementationClasses(MorphiaRegistrarHelperPut h, MorphiaRegistrarHelperPut w) {
    h.put("connector.entities.embedded.kubernetescluster.KubernetesDelegateDetails", KubernetesDelegateDetails.class);
    h.put("connector.entities.embedded.kubernetescluster.KubernetesClusterDetails", KubernetesClusterDetails.class);
    h.put("connector.entities.embedded.kubernetescluster.ClientKeyCertK8", ClientKeyCertK8.class);
    h.put("connector.entities.embedded.kubernetescluster.OpenIdConnectK8", OpenIdConnectK8.class);
    h.put("connector.entities.embedded.kubernetescluster.ServiceAccountK8", ServiceAccountK8.class);
    h.put("connector.entities.embedded.kubernetescluster.UserNamePasswordK8", UserNamePasswordK8.class);
    h.put("connector.entities.embedded.gitconnector.GitSSHAuthentication", GitSSHAuthentication.class);
    h.put("connector.entities.embedded.gitconnector.UserNamePasswordGitAuthentication",
        UserNamePasswordGitAuthentication.class);
  }
}
