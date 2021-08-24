package software.wings.delegatetasks;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.rule.OwnerRule.TMACARI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.beans.FileContentBatchResponse;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.connector.scm.ScmConnector;
import io.harness.delegate.beans.connector.scm.github.GithubConnectorDTO;
import io.harness.delegate.beans.connector.scm.github.GithubTokenSpecDTO;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabConnectorDTO;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabTokenSpecDTO;
import io.harness.delegate.task.scm.ScmDelegateClient;
import io.harness.logging.LogCallback;
import io.harness.product.ci.scm.proto.FileBatchContentResponse;
import io.harness.product.ci.scm.proto.FileContent;
import io.harness.rule.Owner;
import io.harness.service.ScmServiceClient;

import software.wings.WingsBaseTest;
import software.wings.beans.GitConfig;
import software.wings.beans.GitConfig.ProviderType;
import software.wings.beans.GitFileConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.beans.yaml.GitFetchFilesResult;

import java.io.File;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@TargetModule(HarnessModule._930_DELEGATE_TASKS)
@OwnedBy(CDP)
public class ScmFetchFilesHelperTest extends WingsBaseTest {
  @Mock private ScmDelegateClient scmDelegateClient;
  @Mock private ScmServiceClient scmServiceClient;
  @InjectMocks private ScmFetchFilesHelper scmFetchFilesHelper;

  @Before
  public void setUp() throws Exception {}

  @Test
  @Owner(developers = TMACARI)
  @Category(UnitTests.class)
  public void testShouldDownloadFilesUsingScm() {
    ScmFetchFilesHelper spyScmFetchFilesHelper = spy(scmFetchFilesHelper);
    LogCallback logCallback = mock(ExecutionLogCallback.class);
    GitConfig gitConfig = GitConfig.builder().repoUrl("helm-url").build();
    GitFileConfig gitFileConfig = GitFileConfig.builder().build();
    doReturn(GithubConnectorDTO.builder().build()).when(spyScmFetchFilesHelper).getScmConnector(any());
    doReturn(FileContentBatchResponse.builder()
                 .fileBatchContentResponse(
                     FileBatchContentResponse.newBuilder()
                         .addFileContents(
                             FileContent.newBuilder().setStatus(200).setContent("content").setPath("path").build())
                         .build())
                 .build())
        .when(scmDelegateClient)
        .processScmRequest(any());
    spyScmFetchFilesHelper.downloadFilesUsingScm("test", gitFileConfig, gitConfig, logCallback);
    File file = new File("test/path");
    assertThat(file.exists()).isTrue();
  }

  @Test
  @Owner(developers = TMACARI)
  @Category(UnitTests.class)
  public void testShouldFetchFilesFromRepoWithScm() {
    ScmFetchFilesHelper spyScmFetchFilesHelper = spy(scmFetchFilesHelper);
    LogCallback logCallback = mock(ExecutionLogCallback.class);
    List<String> filePathList = Collections.singletonList("test");
    GitConfig gitConfig = GitConfig.builder().repoUrl("helm-url").build();
    GitFileConfig gitFileConfig = GitFileConfig.builder().build();
    doReturn(GithubConnectorDTO.builder().build()).when(spyScmFetchFilesHelper).getScmConnector(any());
    doReturn(FileContentBatchResponse.builder()
                 .fileBatchContentResponse(
                     FileBatchContentResponse.newBuilder()
                         .addFileContents(FileContent.newBuilder().setStatus(200).setContent("content").build())
                         .addFileContents(FileContent.newBuilder().setStatus(400).setContent("content").build())
                         .build())
                 .build())
        .when(scmDelegateClient)
        .processScmRequest(any());
    GitFetchFilesResult result =
        spyScmFetchFilesHelper.fetchFilesFromRepoWithScm(gitFileConfig, gitConfig, filePathList, logCallback);
    assertThat(result.getFiles().size()).isEqualTo(1);
    assertThat(result.getFiles().get(0).getFileContent()).isEqualTo("content");
  }

  @Test
  @Owner(developers = TMACARI)
  @Category(UnitTests.class)
  public void testShouldUseScm() {
    assertThat(scmFetchFilesHelper.shouldUseScm(
                   true, GitConfig.builder().sshSettingAttribute(null).providerType(ProviderType.GITHUB).build()))
        .isTrue();
    assertThat(scmFetchFilesHelper.shouldUseScm(
                   true, GitConfig.builder().sshSettingAttribute(null).providerType(ProviderType.GITLAB).build()))
        .isTrue();
    assertThat(scmFetchFilesHelper.shouldUseScm(
                   true, GitConfig.builder().sshSettingAttribute(null).providerType(ProviderType.GIT).build()))
        .isFalse();
    assertThat(scmFetchFilesHelper.shouldUseScm(false,
                   GitConfig.builder()
                       .sshSettingAttribute(SettingAttribute.Builder.aSettingAttribute().build())
                       .providerType(ProviderType.GITHUB)
                       .build()))
        .isFalse();
    assertThat(scmFetchFilesHelper.shouldUseScm(false,
                   GitConfig.builder()
                       .sshSettingAttribute(SettingAttribute.Builder.aSettingAttribute().build())
                       .providerType(ProviderType.GITLAB)
                       .build()))
        .isFalse();
    assertThat(scmFetchFilesHelper.shouldUseScm(
                   false, GitConfig.builder().sshSettingAttribute(null).providerType(ProviderType.GITHUB).build()))
        .isFalse();
    assertThat(scmFetchFilesHelper.shouldUseScm(
                   false, GitConfig.builder().sshSettingAttribute(null).providerType(ProviderType.GITLAB).build()))
        .isFalse();
  }

  @Test
  @Owner(developers = TMACARI)
  @Category(UnitTests.class)
  public void testGetScmConnector() {
    ScmConnector gitHubScmConnector = scmFetchFilesHelper.getScmConnector(GitConfig.builder()
                                                                              .repoUrl("GitHubURL")
                                                                              .password("password".toCharArray())
                                                                              .providerType(ProviderType.GITHUB)
                                                                              .build());
    assertThat(gitHubScmConnector.getUrl()).isEqualTo("GitHubURL");
    assertThat(((GithubTokenSpecDTO) ((GithubConnectorDTO) gitHubScmConnector).getApiAccess().getSpec())
                   .getTokenRef()
                   .getDecryptedValue())
        .isEqualTo("password".toCharArray());

    ScmConnector gitLabScmConnector = scmFetchFilesHelper.getScmConnector(GitConfig.builder()
                                                                              .repoUrl("GitlabURL")
                                                                              .password("password".toCharArray())
                                                                              .providerType(ProviderType.GITLAB)
                                                                              .build());
    assertThat(gitLabScmConnector.getUrl()).isEqualTo("GitlabURL");
    assertThat(((GitlabTokenSpecDTO) ((GitlabConnectorDTO) gitLabScmConnector).getApiAccess().getSpec())
                   .getTokenRef()
                   .getDecryptedValue())
        .isEqualTo("password".toCharArray());

    assertThat(scmFetchFilesHelper.getScmConnector(GitConfig.builder().providerType(ProviderType.GIT).build()))
        .isNull();
  }
}
