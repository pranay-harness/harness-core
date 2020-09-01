package io.harness.connector.validator;

import static io.harness.delegate.beans.connector.docker.DockerAuthType.USER_PASSWORD;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.ManagerDelegateServiceDriver;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.connector.docker.DockerAuthenticationDTO;
import io.harness.delegate.beans.connector.docker.DockerConnectorDTO;
import io.harness.delegate.beans.connector.docker.DockerTestConnectionTaskResponse;
import io.harness.delegate.beans.connector.docker.DockerUserNamePasswordDTO;
import io.harness.encryption.Scope;
import io.harness.encryption.SecretRefData;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;
import io.harness.secretmanagerclient.services.api.SecretManagerClientService;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@Slf4j
public class DockerConnectionValidatorTest extends CategoryTest {
  @Mock private ManagerDelegateServiceDriver managerDelegateServiceDriver;
  @Mock private SecretManagerClientService ngSecretService;
  @InjectMocks private DockerConnectionValidator dockerConnectionValidator;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = OwnerRule.DEEPAK)
  @Category(UnitTests.class)
  public void validateTest() {
    String dockerRegistryUrl = "url";
    String dockerUserName = "dockerUserName";
    String passwordRefIdentifier = "passwordRefIdentifier";
    SecretRefData passwordSecretRef =
        SecretRefData.builder().identifier(passwordRefIdentifier).scope(Scope.ACCOUNT).build();

    DockerUserNamePasswordDTO dockerUserNamePasswordDTO =
        DockerUserNamePasswordDTO.builder().username(dockerUserName).passwordRef(passwordSecretRef).build();

    DockerAuthenticationDTO dockerAuthenticationDTO =
        DockerAuthenticationDTO.builder().authType(USER_PASSWORD).credentials(dockerUserNamePasswordDTO).build();
    DockerConnectorDTO dockerConnectorDTO =
        DockerConnectorDTO.builder().url(dockerRegistryUrl).authScheme(dockerAuthenticationDTO).build();
    when(ngSecretService.getEncryptionDetails(any(), any())).thenReturn(null);
    when(managerDelegateServiceDriver.sendTask(any(), anyMap(), any()))
        .thenReturn(DockerTestConnectionTaskResponse.builder().connectionSuccessFul(true).build());
    dockerConnectionValidator.validate(dockerConnectorDTO, "accountIdentifier", "orgIdentifier", "projectIdentifier");
    verify(managerDelegateServiceDriver, times(1)).sendTask(any(), anyMap(), any());
  }
}