package io.harness.connector.impl;

import static io.harness.rule.OwnerRule.ABHINAV;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.harness.CategoryTest;
import io.harness.ManagerDelegateServiceDriver;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.connector.gitconnector.GitAuthType;
import io.harness.delegate.beans.connector.gitconnector.GitConfigDTO;
import io.harness.delegate.beans.connector.gitconnector.GitConnectionType;
import io.harness.delegate.beans.connector.gitconnector.GitHTTPAuthenticationDTO;
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
  public void testConnectorValidation() {
    GitConfigDTO gitConfig = GitConfigDTO.builder()
                                 .gitAuth(GitHTTPAuthenticationDTO.builder()
                                              .gitConnectionType(GitConnectionType.REPO)
                                              .accountId(ACCOUNT_ID)
                                              .branchName("branchName")
                                              .encryptedPassword("abcd")
                                              .url("url")
                                              .username("username")
                                              .build())
                                 .gitAuthType(GitAuthType.HTTP)
                                 .build();
    gitConnectorValidator.validate(gitConfig, ACCOUNT_ID);
    doReturn(null).when(secretManagerClientService).getEncryptionDetails(any());
    verify(managerDelegateServiceDriver, times(1)).sendTask(any(), any(), any());
  }
}