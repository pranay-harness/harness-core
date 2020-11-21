package io.harness.connector.validator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.connector.ConnectorValidationResult;
import io.harness.delegate.beans.connector.jira.JiraConnectorDTO;
import io.harness.delegate.beans.connector.jira.connection.JiraTestConnectionTaskNGResponse;
import io.harness.encryption.SecretRefData;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;
import io.harness.secretmanagerclient.services.api.SecretManagerClientService;
import io.harness.service.DelegateGrpcClientWrapper;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class JiraConnectorValidatorTest {
  private static final String ACCOUNT_IDENTIFIER = "accountIdentifier";
  private static final String JIRA_URL = "https://jira.dev.harness.io";
  private static final String ORG_IDENTIFIER = "orgIdentifier";
  private static final String PROJECT_IDENTIFIER = "projectIdentifier";
  private static final String USERNAME = "username";

  @Mock private DelegateGrpcClientWrapper delegateGrpcClientWrapper;
  @Mock private SecretManagerClientService ngSecretService;
  @InjectMocks JiraConnectorValidator connectorValidator;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = OwnerRule.ALEXEI)
  @Category(UnitTests.class)
  public void shouldValidate() {
    JiraConnectorDTO jiraConnectorDTO = JiraConnectorDTO.builder()
                                            .username(USERNAME)
                                            .jiraUrl(JIRA_URL)
                                            .passwordRef(SecretRefData.builder().build())
                                            .build();

    when(ngSecretService.getEncryptionDetails(any(), any())).thenReturn(null);
    when(delegateGrpcClientWrapper.executeSyncTask(any()))
        .thenReturn(JiraTestConnectionTaskNGResponse.builder().canConnect(true).build());

    ConnectorValidationResult result =
        connectorValidator.validate(jiraConnectorDTO, ACCOUNT_IDENTIFIER, ORG_IDENTIFIER, PROJECT_IDENTIFIER);

    assertThat(result.isValid()).isTrue();
    verify(delegateGrpcClientWrapper).executeSyncTask(any());
  }

  @Test
  @Owner(developers = OwnerRule.ALEXEI)
  @Category(UnitTests.class)
  public void shouldValidateFailed() {
    JiraConnectorDTO jiraConnectorDTO = JiraConnectorDTO.builder()
                                            .username(USERNAME)
                                            .jiraUrl(JIRA_URL)
                                            .passwordRef(SecretRefData.builder().build())
                                            .build();

    when(ngSecretService.getEncryptionDetails(any(), any())).thenReturn(null);
    when(delegateGrpcClientWrapper.executeSyncTask(any()))
        .thenReturn(JiraTestConnectionTaskNGResponse.builder().canConnect(false).build());

    ConnectorValidationResult result =
        connectorValidator.validate(jiraConnectorDTO, ACCOUNT_IDENTIFIER, ORG_IDENTIFIER, PROJECT_IDENTIFIER);

    assertThat(result.isValid()).isFalse();
    verify(delegateGrpcClientWrapper).executeSyncTask(any());
  }
}
