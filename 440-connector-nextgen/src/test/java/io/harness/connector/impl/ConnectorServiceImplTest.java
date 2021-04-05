package io.harness.connector.impl;

import static io.harness.delegate.beans.connector.k8Connector.KubernetesCredentialType.MANUAL_CREDENTIALS;
import static io.harness.rule.OwnerRule.MOHIT_GARG;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.connector.ConnectorsTestBase;
import io.harness.connector.services.ConnectorService;
import io.harness.delegate.beans.connector.k8Connector.KubernetesAuthDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesAuthType;
import io.harness.delegate.beans.connector.k8Connector.KubernetesClusterConfigDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesClusterDetailsDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesCredentialDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesUserNamePasswordDTO;
import io.harness.encryption.Scope;
import io.harness.encryption.SecretRefData;
import io.harness.exception.InvalidRequestException;
import io.harness.repositories.ConnectorRepository;
import io.harness.rule.Owner;
import io.harness.service.DelegateGrpcClientWrapper;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@Slf4j
@OwnedBy(HarnessTeam.DX)
public class ConnectorServiceImplTest extends ConnectorsTestBase {
  @Inject @InjectMocks ConnectorService connectorService;
  @Mock ConnectorRepository connectorRepository;
  @Mock DelegateGrpcClientWrapper delegateGrpcClientWrapper;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = MOHIT_GARG)
  @Category(UnitTests.class)
  public void testConnectorTestFlowWithErrorHandlingFramework() {
    String userName = "userName";
    String masterUrl = "https://abc.com";
    String passwordIdentifier = "passwordIdentifier";
    KubernetesAuthDTO kubernetesAuthDTO =
        KubernetesAuthDTO.builder()
            .authType(KubernetesAuthType.USER_PASSWORD)
            .credentials(
                KubernetesUserNamePasswordDTO.builder()
                    .username(userName)
                    .passwordRef(SecretRefData.builder().identifier(passwordIdentifier).scope(Scope.ACCOUNT).build())
                    .build())
            .build();
    KubernetesCredentialDTO connectorDTOWithDelegateCreds =
        KubernetesCredentialDTO.builder()
            .kubernetesCredentialType(MANUAL_CREDENTIALS)
            .config(KubernetesClusterDetailsDTO.builder().masterUrl(masterUrl).auth(kubernetesAuthDTO).build())
            .build();
    KubernetesClusterConfigDTO k8sClusterConfig =
        KubernetesClusterConfigDTO.builder().credential(connectorDTOWithDelegateCreds).build();

    String errorMessage = "Random error message";
    when(delegateGrpcClientWrapper.executeSyncTask(any())).thenThrow(new InvalidRequestException(errorMessage));
    try {
      //            kubernetesConnectionValidator.validate(k8sClusterConfig, "identifier", "identifier", "identifier",
      //            "identifier");
      connectorService.testConnection()
    } catch (Exception exception) {
      log.error("", exception);
    }
  }
}
