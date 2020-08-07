package io.harness.cdng.git.tasks;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.delegate.beans.TaskData.DEFAULT_ASYNC_CALL_TIMEOUT;
import static io.harness.rule.OwnerRule.ABHINAV;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;

import io.harness.category.element.UnitTests;
import io.harness.cdng.gitclient.GitClientNG;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.beans.connector.gitconnector.GitAuthType;
import io.harness.delegate.beans.connector.gitconnector.GitConfigDTO;
import io.harness.delegate.beans.connector.gitconnector.GitConnectionType;
import io.harness.delegate.beans.connector.gitconnector.GitHTTPAuthenticationDTO;
import io.harness.delegate.beans.git.GitCommand.GitCommandType;
import io.harness.delegate.beans.git.GitCommandParams;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.wings.WingsBaseTest;
import software.wings.beans.DelegateTaskPackage;
import software.wings.delegatetasks.validation.DelegateConnectionResult;
import software.wings.service.intfc.security.EncryptionService;

import java.util.Collections;
import java.util.List;

public class NGGitConnectionValidationTest extends WingsBaseTest {
  @Mock GitClientNG gitClient;
  @Mock EncryptionService encryptionService;

  GitConfigDTO gitConfig = GitConfigDTO.builder()
                               .gitAuth(GitHTTPAuthenticationDTO.builder()
                                            .gitConnectionType(GitConnectionType.REPO)
                                            .accountId("ACCOUNT_ID")
                                            .branchName("branchName")
                                            .encryptedPassword("abcd")
                                            .url("url")
                                            .username("username")
                                            .build())
                               .gitAuthType(GitAuthType.HTTP)
                               .build();

  @InjectMocks
  private NGGitConnectionValidation gitConnectionValidation = new NGGitConnectionValidation(generateUuid(),
      DelegateTaskPackage.builder()
          .data((TaskData.builder().async(true).timeout(DEFAULT_ASYNC_CALL_TIMEOUT))
                    .parameters(new Object[] {GitCommandParams.builder()
                                                  .gitCommandType(GitCommandType.VALIDATE)
                                                  .encryptionDetails(Collections.emptyList())
                                                  .gitConfig(gitConfig)
                                                  .build()})
                    .build())

          .build(),

      null);

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testConnectorValidation() {
    doReturn(null).when(gitClient).validate(any());
    doReturn(null).when(encryptionService).decrypt(any());
    List<DelegateConnectionResult> response = gitConnectionValidation.validate();
    assertThat(response).isNotNull();
    assertThat(response.size()).isEqualTo(1);
  }
}