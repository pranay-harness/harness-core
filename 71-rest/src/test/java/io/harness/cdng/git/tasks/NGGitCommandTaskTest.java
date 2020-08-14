package io.harness.cdng.git.tasks;

import static io.harness.delegate.beans.TaskData.DEFAULT_ASYNC_CALL_TIMEOUT;
import static io.harness.rule.OwnerRule.ABHINAV;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;

import io.harness.category.element.UnitTests;
import io.harness.cdng.gitclient.GitClientNG;
import io.harness.connector.mappers.SecretRefHelper;
import io.harness.delegate.beans.DelegateTaskPackage;
import io.harness.delegate.beans.ResponseData;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.beans.connector.gitconnector.GitAuthType;
import io.harness.delegate.beans.connector.gitconnector.GitConfigDTO;
import io.harness.delegate.beans.connector.gitconnector.GitConnectionType;
import io.harness.delegate.beans.connector.gitconnector.GitHTTPAuthenticationDTO;
import io.harness.delegate.beans.git.GitCommand.GitCommandType;
import io.harness.delegate.beans.git.GitCommandExecutionResponse;
import io.harness.delegate.beans.git.GitCommandExecutionResponse.GitCommandStatus;
import io.harness.delegate.beans.git.GitCommandParams;
import io.harness.delegate.beans.git.GitCommitAndPushRequest;
import io.harness.delegate.beans.git.GitCommitAndPushResult;
import io.harness.delegate.git.NGGitService;
import io.harness.encryption.SecretRefData;
import io.harness.rule.Owner;
import io.harness.security.encryption.SecretDecryptionService;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.wings.WingsBaseTest;
import software.wings.beans.TaskType;

import java.util.Collections;

public class NGGitCommandTaskTest extends WingsBaseTest {
  @Mock GitClientNG gitClient;
  @Mock NGGitService ngGitService;
  @Mock SecretDecryptionService encryptionService;
  String passwordIdentifier = "passwordIdentifier";
  String passwordReference = "acc." + passwordIdentifier;

  SecretRefData passwordRef = SecretRefHelper.createSecretRef(passwordReference);
  GitConfigDTO gitConfig = GitConfigDTO.builder()
                               .gitAuth(GitHTTPAuthenticationDTO.builder()
                                            .gitConnectionType(GitConnectionType.REPO)
                                            .branchName("branchName")
                                            .passwordRef(passwordRef)
                                            .url("url")
                                            .username("username")
                                            .build())
                               .gitAuthType(GitAuthType.HTTP)
                               .build();
  @InjectMocks
  private NGGitCommandTask ngGitCommandValidationTask =
      (NGGitCommandTask) TaskType.NG_GIT_COMMAND.getDelegateRunnableTask(
          DelegateTaskPackage.builder()
              .delegateId("delegateid")
              .data((TaskData.builder().async(true).timeout(DEFAULT_ASYNC_CALL_TIMEOUT))
                        .parameters(new Object[] {GitCommandParams.builder()
                                                      .gitCommandType(GitCommandType.VALIDATE)
                                                      .encryptionDetails(Collections.emptyList())
                                                      .gitConfig(gitConfig)
                                                      .build()})
                        .build())
              .build(),
          notifyResponseData -> {}, () -> true);

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testValidationTask() {
    GitCommandParams task = GitCommandParams.builder()
                                .gitConfig(gitConfig)
                                .encryptionDetails(Collections.emptyList())
                                .gitCommandType(GitCommandType.VALIDATE)
                                .build();
    doReturn(null).when(ngGitService).validate(any(), any());
    doReturn(null).when(encryptionService).decrypt(any(), any());
    ResponseData response = ngGitCommandValidationTask.run(task);
    assertThat(response).isInstanceOf(GitCommandExecutionResponse.class);
    assertThat(((GitCommandExecutionResponse) response).getGitCommandStatus()).isEqualTo(GitCommandStatus.SUCCESS);
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testHandleCommitAndPush() {
    GitCommandParams task = GitCommandParams.builder()
                                .gitConfig(gitConfig)
                                .encryptionDetails(Collections.emptyList())
                                .gitCommandRequest(GitCommitAndPushRequest.builder().build())
                                .gitCommandType(GitCommandType.COMMIT_AND_PUSH)
                                .build();
    doReturn(GitCommitAndPushResult.builder().build()).when(gitClient).commitAndPush(any(), any(), any(), any());
    doReturn(null).when(encryptionService).decrypt(any(), any());
    ResponseData response = ngGitCommandValidationTask.run(task);
    assertThat(response).isInstanceOf(GitCommandExecutionResponse.class);
    assertThat(((GitCommandExecutionResponse) response).getGitCommandStatus()).isEqualTo(GitCommandStatus.SUCCESS);
  }
}