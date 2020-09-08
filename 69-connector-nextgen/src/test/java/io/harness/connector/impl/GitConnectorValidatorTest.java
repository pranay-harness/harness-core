package io.harness.connector.impl;

import static io.harness.rule.OwnerRule.ABHINAV;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.harness.CategoryTest;
import io.harness.ManagerDelegateServiceDriver;
import io.harness.category.element.UnitTests;
import io.harness.connector.mappers.SecretRefHelper;
import io.harness.connector.validator.GitConnectorValidator;
import io.harness.delegate.beans.connector.ConnectorValidationResult;
import io.harness.delegate.beans.connector.gitconnector.GitAuthType;
import io.harness.delegate.beans.connector.gitconnector.GitConfigDTO;
import io.harness.delegate.beans.connector.gitconnector.GitConnectionType;
import io.harness.delegate.beans.connector.gitconnector.GitHTTPAuthenticationDTO;
import io.harness.delegate.beans.git.GitCommandExecutionResponse;
import io.harness.delegate.beans.git.GitCommandExecutionResponse.GitCommandStatus;
import io.harness.rule.Owner;
import io.harness.secretmanagerclient.services.api.SecretManagerClientService;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class GitConnectorValidatorTest extends CategoryTest {
  public static final String ACCOUNT_ID = "ACCOUNT_ID";
  @Mock ManagerDelegateServiceDriver managerDelegateServiceDriver;
  @Mock SecretManagerClientService secretManagerClientService;
  @InjectMocks GitConnectorValidator gitConnectorValidator;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testConnectorValidationForFailedResponse() {
    GitConfigDTO gitConfig = GitConfigDTO.builder()
                                 .gitAuth(GitHTTPAuthenticationDTO.builder()
                                              .passwordRef(SecretRefHelper.createSecretRef("acc.abcd"))
                                              .username("username")
                                              .build())
                                 .gitConnectionType(GitConnectionType.REPO)
                                 .branchName("branchName")
                                 .url("url")
                                 .gitAuthType(GitAuthType.HTTP)
                                 .build();
    doReturn(null).when(secretManagerClientService).getEncryptionDetails(any());
    ConnectorValidationResult connectorValidationResult =
        gitConnectorValidator.validate(gitConfig, ACCOUNT_ID, null, null);
    verify(managerDelegateServiceDriver, times(1)).sendTask(any(), any(), any());
    assertThat(connectorValidationResult.isValid()).isFalse();
    assertThat(connectorValidationResult.getErrorMessage()).isNotBlank();
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testConnectorValidationForSuccessfulResponse() {
    GitConfigDTO gitConfig = GitConfigDTO.builder()
                                 .gitAuth(GitHTTPAuthenticationDTO.builder()
                                              .passwordRef(SecretRefHelper.createSecretRef("acc.abcd"))

                                              .username("username")
                                              .build())
                                 .gitAuthType(GitAuthType.HTTP)
                                 .gitConnectionType(GitConnectionType.REPO)
                                 .branchName("branchName")
                                 .url("url")
                                 .build();
    GitCommandExecutionResponse gitResponse =
        GitCommandExecutionResponse.builder().gitCommandStatus(GitCommandStatus.SUCCESS).build();
    doReturn(null).when(secretManagerClientService).getEncryptionDetails(any());
    doReturn(gitResponse).when(managerDelegateServiceDriver).sendTask(any(), any(), any());
    ConnectorValidationResult connectorValidationResult =
        gitConnectorValidator.validate(gitConfig, ACCOUNT_ID, null, null);
    verify(managerDelegateServiceDriver, times(1)).sendTask(any(), any(), any());
    assertThat(connectorValidationResult.isValid()).isTrue();
  }
}