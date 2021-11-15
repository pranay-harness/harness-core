package io.harness.delegate.task.git;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.ABHINAV2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.cistatus.service.GithubAppConfig;
import io.harness.cistatus.service.GithubService;
import io.harness.connector.ConnectivityStatus;
import io.harness.connector.ConnectorValidationResult;
import io.harness.connector.service.git.NGGitService;
import io.harness.connector.service.scm.ScmDelegateClient;
import io.harness.connector.task.git.GitCommandTaskHandler;
import io.harness.delegate.beans.connector.scm.GitConnectionType;
import io.harness.delegate.beans.connector.scm.ScmConnector;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitConfig;
import io.harness.delegate.beans.connector.scm.github.GithubApiAccess;
import io.harness.delegate.beans.connector.scm.github.GithubApiAccessType;
import io.harness.delegate.beans.connector.scm.github.GithubAppSpec;
import io.harness.delegate.beans.connector.scm.github.GithubConnector;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabApiAccess;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabConnector;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabTokenSpec;
import io.harness.delegate.beans.git.GitCommandExecutionResponse;
import io.harness.encryption.Scope;
import io.harness.encryption.SecretRefData;
import io.harness.errorhandling.NGErrorHelper;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.runtime.SCMRuntimeException;
import io.harness.product.ci.scm.proto.GetUserReposResponse;
import io.harness.rule.Owner;
import io.harness.service.ScmServiceClient;
import io.harness.shell.SshSessionConfig;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

@OwnedBy(HarnessTeam.CDP)
public class GitCommandTaskHandlerTest extends CategoryTest {
  @Mock private NGGitService gitService;
  @Mock private GithubService gitHubService;
  @Mock private NGErrorHelper ngErrorHelper;
  @Mock private ScmDelegateClient scmDelegateClient;
  @Mock private ScmServiceClient scmServiceClient;

  @Spy @InjectMocks GitCommandTaskHandler gitCommandTaskHandler;

  private static final long SIMULATED_REQUEST_TIME_MILLIS = 1609459200000L;
  private static final String ACCOUNT_IDENTIFIER = generateUuid();
  private static final String SIMULATED_EXCEPTION_MESSAGE = generateUuid();
  private static final String TEST_GIT_REPO_URL = "https://www.github.com/dummy-org/dummy-repo";
  private static final String TEST_STRING_INPUT = generateUuid();
  private SshSessionConfig sshSessionConfig;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = ABHINAV2)
  @Category(UnitTests.class)
  public void testGitCredentials() {
    GitConfig gitConfig = GitConfig.builder().build();
    ScmConnector connector = GitlabConnector.builder().build();
    GitCommandExecutionResponse response = mock(GitCommandExecutionResponse.class, RETURNS_DEEP_STUBS);
    when(response.getConnectorValidationResult().getTestedAt()).thenReturn(SIMULATED_REQUEST_TIME_MILLIS);
    doReturn(response)
        .when(gitCommandTaskHandler)
        .handleValidateTask(
            any(GitConfig.class), any(ScmConnector.class), any(String.class), any(SshSessionConfig.class));

    ConnectorValidationResult validationResult =
        gitCommandTaskHandler.validateGitCredentials(gitConfig, connector, ACCOUNT_IDENTIFIER, sshSessionConfig);
    assertThat(validationResult.getStatus()).isEqualTo(ConnectivityStatus.SUCCESS);
    assertThat(validationResult.getTestedAt()).isEqualTo(SIMULATED_REQUEST_TIME_MILLIS);
  }

  @Test
  @Owner(developers = ABHINAV2)
  @Category(UnitTests.class)
  public void testGitCredentialsWhenException() {
    GitConfig gitConfig = GitConfig.builder().build();
    ScmConnector connector = GitlabConnector.builder().build();
    doThrow(new InvalidRequestException(SIMULATED_EXCEPTION_MESSAGE))
        .when(gitCommandTaskHandler)
        .handleValidateTask(
            any(GitConfig.class), any(ScmConnector.class), any(String.class), any(SshSessionConfig.class));

    ConnectorValidationResult validationResult =
        gitCommandTaskHandler.validateGitCredentials(gitConfig, connector, ACCOUNT_IDENTIFIER, sshSessionConfig);
    assertThat(validationResult.getStatus()).isEqualTo(ConnectivityStatus.FAILURE);
  }

  @Test
  @Owner(developers = ABHINAV2)
  @Category(UnitTests.class)
  public void testValidateTask() {
    GitConfig gitConfig = GitConfig.builder().gitConnectionType(GitConnectionType.REPO).build();
    ScmConnector connector = GithubConnector.builder()
                                 .url(TEST_GIT_REPO_URL)
                                 .apiAccess(GithubApiAccess.builder()
                                                .type(GithubApiAccessType.GITHUB_APP)
                                                .spec(GithubAppSpec.builder()
                                                          .applicationId(TEST_STRING_INPUT)
                                                          .installationId(TEST_STRING_INPUT)
                                                          .privateKeyRef(new SecretRefData(TEST_STRING_INPUT,
                                                              Scope.ACCOUNT, TEST_STRING_INPUT.toCharArray()))
                                                          .build())
                                                .build())
                                 .build();
    doNothing().when(gitService).validateOrThrow(any(GitConfig.class), any(String.class), any(SshSessionConfig.class));
    doReturn(TEST_STRING_INPUT).when(gitHubService).getToken(any(GithubAppConfig.class));

    GitCommandExecutionResponse response = (GitCommandExecutionResponse) gitCommandTaskHandler.handleValidateTask(
        gitConfig, connector, ACCOUNT_IDENTIFIER, sshSessionConfig);
    assertThat(response.getGitCommandStatus()).isEqualTo(GitCommandExecutionResponse.GitCommandStatus.SUCCESS);
  }

  @Test
  @Owner(developers = ABHINAV2)
  @Category(UnitTests.class)
  public void testValidateGithubAppWithException() {
    GitConfig gitConfig = GitConfig.builder().gitConnectionType(GitConnectionType.REPO).build();
    ScmConnector connector = GithubConnector.builder()
                                 .url(TEST_GIT_REPO_URL)
                                 .apiAccess(GithubApiAccess.builder()
                                                .type(GithubApiAccessType.GITHUB_APP)
                                                .spec(GithubAppSpec.builder()
                                                          .applicationId(TEST_STRING_INPUT)
                                                          .installationId(TEST_STRING_INPUT)
                                                          .privateKeyRef(new SecretRefData(TEST_STRING_INPUT,
                                                              Scope.ACCOUNT, TEST_STRING_INPUT.toCharArray()))
                                                          .build())
                                                .build())
                                 .build();
    doThrow(new RuntimeException(SIMULATED_EXCEPTION_MESSAGE, new SCMRuntimeException(SIMULATED_EXCEPTION_MESSAGE)))
        .when(gitHubService)
        .getToken(any(GithubAppConfig.class));
    assertThatThrownBy(
        () -> gitCommandTaskHandler.handleValidateTask(gitConfig, connector, ACCOUNT_IDENTIFIER, sshSessionConfig))
        .isInstanceOf(SCMRuntimeException.class);
  }

  @Test
  @Owner(developers = ABHINAV2)
  @Category(UnitTests.class)
  public void testValidateTaskGivenScmConnectorWithoutApiAccess() {
    GitConfig gitConfig =
        GitConfig.builder().gitConnectionType(GitConnectionType.ACCOUNT).validationRepo(TEST_STRING_INPUT).build();
    ScmConnector connector = GithubConnector.builder().build();

    GitCommandExecutionResponse response = (GitCommandExecutionResponse) gitCommandTaskHandler.handleValidateTask(
        gitConfig, connector, ACCOUNT_IDENTIFIER, sshSessionConfig);
    assertThat(response.getGitCommandStatus()).isEqualTo(GitCommandExecutionResponse.GitCommandStatus.SUCCESS);
  }

  @Test
  @Owner(developers = ABHINAV2)
  @Category(UnitTests.class)
  public void testValidateTaskHandleApiAccessValidation() {
    GitConfig gitConfig = GitConfig.builder().gitConnectionType(GitConnectionType.ACCOUNT).build();
    ScmConnector connector = GitlabConnector.builder()
                                 .apiAccess(GitlabApiAccess.builder().spec(GitlabTokenSpec.builder().build()).build())
                                 .build();
    GetUserReposResponse userReposResponse = GetUserReposResponse.newBuilder().build();
    doReturn(userReposResponse).when(scmDelegateClient).processScmRequest(any());

    GitCommandExecutionResponse response = (GitCommandExecutionResponse) gitCommandTaskHandler.handleValidateTask(
        gitConfig, connector, ACCOUNT_IDENTIFIER, sshSessionConfig);
    assertThat(response.getGitCommandStatus()).isEqualTo(GitCommandExecutionResponse.GitCommandStatus.SUCCESS);
  }

  @Test
  @Owner(developers = ABHINAV2)
  @Category(UnitTests.class)
  public void testApiAccessValidationGivenExceptionInScmRequest() {
    GitConfig gitConfig = GitConfig.builder().gitConnectionType(GitConnectionType.ACCOUNT).build();
    ScmConnector connector = GitlabConnector.builder()
                                 .apiAccess(GitlabApiAccess.builder().spec(GitlabTokenSpec.builder().build()).build())
                                 .build();
    doThrow(new SCMRuntimeException(SIMULATED_EXCEPTION_MESSAGE)).when(scmDelegateClient).processScmRequest(any());
    assertThatThrownBy(
        () -> gitCommandTaskHandler.handleValidateTask(gitConfig, connector, ACCOUNT_IDENTIFIER, sshSessionConfig))
        .isInstanceOf(SCMRuntimeException.class);
  }

  @Test
  @Owner(developers = ABHINAV2)
  @Category(UnitTests.class)
  public void testApiAccessValidationGivenErrorInRepoResponse() {
    GitConfig gitConfig = GitConfig.builder().gitConnectionType(GitConnectionType.ACCOUNT).build();
    ScmConnector connector = GitlabConnector.builder()
                                 .apiAccess(GitlabApiAccess.builder().spec(GitlabTokenSpec.builder().build()).build())
                                 .build();
    GetUserReposResponse userReposResponse = GetUserReposResponse.newBuilder().setStatus(400).build();
    doReturn(userReposResponse).when(scmDelegateClient).processScmRequest(any());

    assertThatThrownBy(
        () -> gitCommandTaskHandler.handleValidateTask(gitConfig, connector, ACCOUNT_IDENTIFIER, sshSessionConfig))
        .isInstanceOf(SCMRuntimeException.class);
  }
}
