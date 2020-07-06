package io.harness.serializer.kryo;

import com.esotericsoftware.kryo.Kryo;
import io.harness.connector.apis.dtos.K8Connector.ClientKeyCertDTO;
import io.harness.connector.apis.dtos.K8Connector.KubernetesAuthDTO;
import io.harness.connector.apis.dtos.K8Connector.KubernetesClusterConfigDTO;
import io.harness.connector.apis.dtos.K8Connector.KubernetesClusterDetailsDTO;
import io.harness.connector.apis.dtos.K8Connector.KubernetesCredentialDTO;
import io.harness.connector.apis.dtos.K8Connector.KubernetesDelegateDetailsDTO;
import io.harness.connector.apis.dtos.K8Connector.OpenIdConnectDTO;
import io.harness.connector.apis.dtos.K8Connector.ServiceAccountDTO;
import io.harness.connector.apis.dtos.K8Connector.UserNamePasswordDTO;
import io.harness.connector.common.kubernetes.KubernetesAuthType;
import io.harness.connector.common.kubernetes.KubernetesCredentialType;
import io.harness.serializer.KryoRegistrar;

public class ConnectorKryoRegistrar implements KryoRegistrar {
  @Override
  public void register(Kryo kryo) {
    kryo.register(KubernetesClusterConfigDTO.class, 12000);
    kryo.register(KubernetesCredentialType.class, 12001);
    kryo.register(KubernetesCredentialDTO.class, 12002);
    kryo.register(KubernetesDelegateDetailsDTO.class, 12003);
    kryo.register(KubernetesClusterDetailsDTO.class, 12004);
    kryo.register(KubernetesAuthDTO.class, 12005);
    kryo.register(KubernetesAuthType.class, 12006);
    kryo.register(UserNamePasswordDTO.class, 12007);
    kryo.register(ClientKeyCertDTO.class, 12008);
    kryo.register(ServiceAccountDTO.class, 12009);
    kryo.register(OpenIdConnectDTO.class, 12010);
  }
}
