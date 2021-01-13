package io.harness.connector.impl;

import static io.harness.delegate.beans.connector.ConnectivityStatus.FAILURE;
import static io.harness.delegate.beans.connector.ConnectivityStatus.SUCCESS;
import static io.harness.encryption.Scope.ACCOUNT;
import static io.harness.rule.OwnerRule.ABHINAV;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.connector.validator.scmValidators.GitConnectorValidator;
import io.harness.delegate.beans.connector.ConnectorValidationResult;
import io.harness.delegate.beans.connector.scm.GitAuthType;
import io.harness.delegate.beans.connector.scm.GitConnectionType;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitConfigDTO;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitHTTPAuthenticationDTO;
import io.harness.delegate.beans.git.GitCommandExecutionResponse;
import io.harness.delegate.beans.git.GitCommandExecutionResponse.GitCommandStatus;
import io.harness.encryption.SecretRefHelper;
import io.harness.errorhandling.NGErrorHelper;
import io.harness.rule.Owner;
import io.harness.secretmanagerclient.services.api.SecretManagerClientService;
import io.harness.service.DelegateGrpcClientWrapper;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class GitConnectorValidatorTest extends CategoryTest {
  public static final String ACCOUNT_ID = "ACCOUNT_ID";
  @Mock DelegateGrpcClientWrapper delegateGrpcClientWrapper;
  @Mock SecretManagerClientService secretManagerClientService;
  @Mock NGErrorHelper ngErrorHelper;
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
                                              .passwordRef(SecretRefHelper.createSecretRef(ACCOUNT + "abcd"))
                                              .username("username")
                                              .build())
                                 .gitConnectionType(GitConnectionType.REPO)
                                 .branchName("branchName")
                                 .url("url")
                                 .gitAuthType(GitAuthType.HTTP)
                                 .build();
    GitCommandExecutionResponse gitResponse =
        GitCommandExecutionResponse.builder().gitCommandStatus(GitCommandStatus.FAILURE).build();
    doReturn(gitResponse).when(delegateGrpcClientWrapper).executeSyncTask(any());
    doReturn(null).when(secretManagerClientService).getEncryptionDetails(any());
    ConnectorValidationResult connectorValidationResult =
        gitConnectorValidator.validate(gitConfig, ACCOUNT_ID, null, null);
    verify(delegateGrpcClientWrapper, times(1)).executeSyncTask(any());
    assertThat(connectorValidationResult.getStatus()).isEqualTo(FAILURE);
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testConnectorValidationForSuccessfulResponse() {
    GitConfigDTO gitConfig =
        GitConfigDTO.builder()
            .gitAuth(GitHTTPAuthenticationDTO.builder()
                         .passwordRef(SecretRefHelper.createSecretRef(ACCOUNT.getYamlRepresentation() + ".abcd"))
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
    doReturn(gitResponse).when(delegateGrpcClientWrapper).executeSyncTask(any());
    ConnectorValidationResult connectorValidationResult =
        gitConnectorValidator.validate(gitConfig, ACCOUNT_ID, null, null);
    verify(delegateGrpcClientWrapper, times(1)).executeSyncTask(any());
    assertThat(connectorValidationResult.getStatus()).isEqualTo(SUCCESS);
  }
}
