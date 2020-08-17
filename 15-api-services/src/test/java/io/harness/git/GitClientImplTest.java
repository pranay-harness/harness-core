package io.harness.git;

import static io.harness.git.model.DownloadFilesRequest.downloadFilesRequestBuilder;
import static io.harness.git.model.FetchFilesByPathRequest.fetchFilesByPathRequestBuilder;
import static io.harness.rule.OwnerRule.ARVIND;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Fail.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.exception.GeneralException;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.YamlException;
import io.harness.git.model.DownloadFilesRequest;
import io.harness.git.model.FetchFilesByPathRequest;
import io.harness.git.model.GitBaseRequest;
import io.harness.git.model.UsernamePasswordAuthRequest;
import io.harness.rule.Owner;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;
import org.zeroturnaround.exec.stream.LogOutputStream;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
public class GitClientImplTest extends CategoryTest {
  private static final String USERNAME = "USERNAME";
  private static final String PASSWORD = "PASSWORD";
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();
  @Mock GitClientHelper gitClientHelper;
  @InjectMocks @Spy GitClientV2Impl gitClient;
  private Git git;
  private String repoPath;

  @Before
  public void setUp() throws Exception {
    repoPath = Files.createTempDirectory(UUID.randomUUID().toString()).toString();
    createLocalRepo(repoPath);
    git = Git.open(new File(repoPath));
    doReturn("").when(gitClientHelper).getGitLogMessagePrefix(any());
  }

  @Test
  @Owner(developers = ARVIND)
  @Category(UnitTests.class)
  public void testValidate() throws Exception {
    assertThatThrownBy(() -> gitClient.validate(null)).isInstanceOf(GeneralException.class);
    assertThatThrownBy(() -> gitClient.validate(GitBaseRequest.builder().build()))
        .isInstanceOf(InvalidRequestException.class);
    GitBaseRequest request = GitBaseRequest.builder()
                                 .repoUrl(repoPath)
                                 .authRequest(new UsernamePasswordAuthRequest(USERNAME, PASSWORD.toCharArray()))
                                 .build();
    doReturn("").when(gitClientHelper).getGitLogMessagePrefix(any());
    assertThat(gitClient.validate(request)).isNull();

    String randomPath = Files.createTempDirectory(UUID.randomUUID().toString()).toString();
    request.setRepoUrl(randomPath);
    assertThat(gitClient.validate(request)).isNotNull();
  }

  @Test
  @Owner(developers = ARVIND)
  @Category(UnitTests.class)
  public void testEnsureRepoLocallyClonedAndUpdated() {
    assertThatThrownBy(() -> gitClient.ensureRepoLocallyClonedAndUpdated(null)).isInstanceOf(GeneralException.class);

    GitBaseRequest request = GitBaseRequest.builder()
                                 .repoUrl(repoPath)
                                 .authRequest(new UsernamePasswordAuthRequest(USERNAME, PASSWORD.toCharArray()))
                                 .branch("master")
                                 .build();
    doReturn(repoPath).when(gitClientHelper).getRepoDirectory(request);
    assertThat(git.getRepository().getConfig().getString("remote", "origin", "url")).isNull();
    gitClient.ensureRepoLocallyClonedAndUpdated(request);
    assertThat(git.getRepository().getConfig().getString("remote", "origin", "url")).isNotNull();
    addRemote(repoPath);
    gitClient.ensureRepoLocallyClonedAndUpdated(request);
  }

  @Test
  @Owner(developers = ARVIND)
  @Category(UnitTests.class)
  public void testDownloadFiles_Branch() throws Exception {
    DownloadFilesRequest request = downloadFilesRequestBuilder()
                                       .repoUrl(repoPath)
                                       .authRequest(new UsernamePasswordAuthRequest(USERNAME, PASSWORD.toCharArray()))
                                       .branch("master")
                                       .connectorId("CONNECTOR_ID")
                                       .accountId("ACCOUNT_ID")
                                       .destinationDirectory("./")
                                       .build();
    assertThatThrownBy(() -> gitClient.downloadFiles(request))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessageContaining("FilePaths can not be empty");
    request.setFilePaths(Collections.singletonList("./"));
    doReturn("").when(gitClientHelper).getLockObject(request.getConnectorId());
    doNothing().when(gitClientHelper).createDirStructureForFileDownload(any());
    doReturn(repoPath).when(gitClientHelper).getFileDownloadRepoDirectory(any());
    addRemote(repoPath);
    gitClient.downloadFiles(request);
  }

  @Test
  @Owner(developers = ARVIND)
  @Category(UnitTests.class)
  public void testDownloadFiles_Commit() throws Exception {
    DownloadFilesRequest request = downloadFilesRequestBuilder()
                                       .repoUrl(repoPath)
                                       .authRequest(new UsernamePasswordAuthRequest(USERNAME, PASSWORD.toCharArray()))
                                       .commitId("t1")
                                       .connectorId("CONNECTOR_ID")
                                       .accountId("ACCOUNT_ID")
                                       .destinationDirectory("./")
                                       .build();
    assertThatThrownBy(() -> gitClient.downloadFiles(request))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessageContaining("FilePaths can not be empty");
    request.setFilePaths(Collections.singletonList("./"));
    doReturn("").when(gitClientHelper).getLockObject(request.getConnectorId());
    doNothing().when(gitClientHelper).createDirStructureForFileDownload(any());
    doReturn(repoPath).when(gitClientHelper).getFileDownloadRepoDirectory(any());
    addRemote(repoPath);
    gitClient.downloadFiles(request);
  }

  @Test
  @Owner(developers = ARVIND)
  @Category(UnitTests.class)
  public void testDownloadFiles_Clone() throws Exception {
    DownloadFilesRequest request = downloadFilesRequestBuilder()
                                       .repoUrl(repoPath)
                                       .authRequest(new UsernamePasswordAuthRequest(USERNAME, PASSWORD.toCharArray()))
                                       .branch("master")
                                       .connectorId("CONNECTOR_ID")
                                       .accountId("ACCOUNT_ID")
                                       .destinationDirectory("./")
                                       .build();
    assertThatThrownBy(() -> gitClient.downloadFiles(request))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessageContaining("FilePaths can not be empty");
    request.setFilePaths(Collections.singletonList("./"));
    doReturn("").when(gitClientHelper).getLockObject(request.getConnectorId());
    doNothing().when(gitClientHelper).createDirStructureForFileDownload(any());
    doReturn(repoPath).when(gitClientHelper).getFileDownloadRepoDirectory(any());

    assertThatThrownBy(() -> gitClient.downloadFiles(request)).isInstanceOf(YamlException.class);
  }

  @Test
  @Owner(developers = ARVIND)
  @Category(UnitTests.class)
  public void testFetchFilesByPath() throws Exception {
    FetchFilesByPathRequest request =
        fetchFilesByPathRequestBuilder()
            .repoUrl(repoPath)
            .authRequest(new UsernamePasswordAuthRequest(USERNAME, PASSWORD.toCharArray()))
            .branch("master")
            .connectorId("CONNECTOR_ID")
            .accountId("ACCOUNT_ID")
            .build();
    assertThatThrownBy(() -> gitClient.fetchFilesByPath(request))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessageContaining("FilePaths can not be empty");
    request.setFilePaths(Collections.singletonList("./"));
    doReturn("").when(gitClientHelper).getLockObject(request.getConnectorId());
    doNothing().when(gitClientHelper).createDirStructureForFileDownload(any());
    doReturn(repoPath).when(gitClientHelper).getFileDownloadRepoDirectory(any());

    assertThatThrownBy(() -> gitClient.fetchFilesByPath(request)).isInstanceOf(YamlException.class);
    addRemote(repoPath);
    gitClient.fetchFilesByPath(request);
  }

  private void executeCommand(String command) {
    try {
      ProcessExecutor processExecutor = new ProcessExecutor()
                                            .timeout(30, TimeUnit.SECONDS)
                                            .command("/bin/sh", "-c", command)
                                            .readOutput(true)
                                            .redirectOutput(new LogOutputStream() {
                                              @Override
                                              protected void processLine(String line) {
                                                logger.info(line);
                                              }
                                            });

      ProcessResult processResult = processExecutor.execute();
      assertThat(processResult.getExitValue()).isEqualTo(0);

    } catch (InterruptedException | TimeoutException | IOException ex) {
      fail("Should not reach here.");
    }
  }

  private void addRemote(String repoPath) {
    try {
      String remoteRepo = Files.createTempDirectory(UUID.randomUUID().toString()).toString();
      createLocalRepo(remoteRepo);
      String command = new StringBuilder(128)
                           .append("cd " + repoPath + ";")
                           .append("git remote add origin " + remoteRepo + ";")
                           .append("touch 1.txt;")
                           .append("git add 1.txt;")
                           .append("git commit -m 'commit1';")
                           .append("git push origin master;")
                           .append("git tag t1;")
                           .append("git push origin t1;")
                           .append("git remote update;")
                           .append("git fetch;")
                           .toString();

      executeCommand(command);
    } catch (Exception e) {
      fail("Should not reach here.");
    }
  }

  private void createLocalRepo(String repoPath) {
    String command = new StringBuilder(128)
                         .append("mkdir -p " + repoPath + ";")
                         .append("cd " + repoPath + ";")
                         .append("git init;")
                         .toString();

    executeCommand(command);
  }
}