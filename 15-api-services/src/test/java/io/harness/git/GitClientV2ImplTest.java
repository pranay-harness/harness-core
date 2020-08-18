package io.harness.git;

import static io.harness.exception.WingsException.SRE;
import static io.harness.git.model.ChangeType.ADD;
import static io.harness.git.model.ChangeType.DELETE;
import static io.harness.git.model.ChangeType.RENAME;
import static io.harness.git.model.CommitAndPushRequest.commitAndPushRequestBuilder;
import static io.harness.git.model.DiffRequest.diffRequestBuilder;
import static io.harness.git.model.DownloadFilesRequest.downloadFilesRequestBuilder;
import static io.harness.git.model.FetchFilesBwCommitsRequest.fetchFilesBwCommitsRequestBuilder;
import static io.harness.git.model.FetchFilesByPathRequest.fetchFilesByPathRequestBuilder;
import static io.harness.git.model.PushResultGit.pushResultBuilder;
import static io.harness.rule.OwnerRule.ABHINAV;
import static io.harness.rule.OwnerRule.ARVIND;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Fail.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.exception.GeneralException;
import io.harness.exception.GitClientException;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.YamlException;
import io.harness.git.model.CommitAndPushRequest;
import io.harness.git.model.CommitAndPushResult;
import io.harness.git.model.CommitResult;
import io.harness.git.model.DiffRequest;
import io.harness.git.model.DiffResult;
import io.harness.git.model.DownloadFilesRequest;
import io.harness.git.model.FetchFilesBwCommitsRequest;
import io.harness.git.model.FetchFilesByPathRequest;
import io.harness.git.model.FetchFilesResult;
import io.harness.git.model.GitBaseRequest;
import io.harness.git.model.GitFileChange;
import io.harness.git.model.PushResultGit;
import io.harness.rule.Owner;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.revwalk.RevCommit;
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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

@Slf4j
public class GitClientV2ImplTest extends CategoryTest {
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
    addGitTag(repoPath, "t1");
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

  @Test
  @Owner(developers = ARVIND)
  @Category(UnitTests.class)
  public void testFetchFilesBetweenCommits() throws Exception {
    FetchFilesBwCommitsRequest request =
        fetchFilesBwCommitsRequestBuilder()
            .repoUrl(repoPath)
            .authRequest(new UsernamePasswordAuthRequest(USERNAME, PASSWORD.toCharArray()))
            .branch("master")
            .connectorId("CONNECTOR_ID")
            .accountId("ACCOUNT_ID")
            .build();

    assertThatThrownBy(() -> gitClient.fetchFilesBetweenCommits(request))
        .isInstanceOf(YamlException.class)
        .hasMessageContaining("Old commit id can not be empty");

    request.setOldCommitId("t1");

    assertThatThrownBy(() -> gitClient.fetchFilesBetweenCommits(request))
        .isInstanceOf(YamlException.class)
        .hasMessageContaining("New commit id can not be empty");

    request.setNewCommitId("t2");
    doReturn("").when(gitClientHelper).getLockObject(request.getConnectorId());

    doNothing().when(gitClientHelper).createDirStructureForFileDownload(any());
    doReturn(repoPath).when(gitClientHelper).getFileDownloadRepoDirectory(any());

    assertThatThrownBy(() -> gitClient.fetchFilesBetweenCommits(request)).isInstanceOf(YamlException.class);

    addRemote(repoPath);
    addGitTag(repoPath, "t1");
    addGitTag(repoPath, "t2");
    FetchFilesResult fetchFilesResult = gitClient.fetchFilesBetweenCommits(request);
    assertThat(fetchFilesResult).isNotNull();
    assertThat(fetchFilesResult.getCommitResult().getCommitId()).isEqualTo("t2");
    assertThat(fetchFilesResult.getFiles().size()).isEqualTo(1);
    assertThat(fetchFilesResult.getFiles().get(0).getFilePath()).isEqualTo("t2");
  }

  private String executeCommand(String command) {
    final String[] returnString = new String[1];
    try {
      ProcessExecutor processExecutor = new ProcessExecutor()
                                            .timeout(30, TimeUnit.SECONDS)
                                            .command("/bin/sh", "-c", command)
                                            .readOutput(true)
                                            .redirectOutput(new LogOutputStream() {
                                              @Override
                                              protected void processLine(String line) {
                                                returnString[0] = line;
                                              }
                                            });

      ProcessResult processResult = processExecutor.execute();
      assertThat(processResult.getExitValue()).isEqualTo(0);

    } catch (InterruptedException | TimeoutException | IOException ex) {
      fail("Should not reach here.");
    }
    return Arrays.toString(returnString);
  }

  private void addRemote(String repoPath) {
    try {
      String remoteRepo = Files.createTempDirectory(UUID.randomUUID().toString()).toString();
      createLocalRepo(remoteRepo);
      String command = new StringBuilder(128)
                           .append("cd " + repoPath + ";")
                           .append("git remote add origin " + remoteRepo + ";")
                           .append("touch base.txt;")
                           .append("git add base.txt;")
                           .append("git commit -m 'commit base';")
                           .append("git push origin master;")
                           .append("git tag base;")
                           .append("git push origin base;")
                           .append("git remote update;")
                           .append("git fetch;")
                           .toString();

      executeCommand(command);
    } catch (Exception e) {
      fail("Should not reach here.");
    }
  }

  private void addGitTag(String repoPath, String tag) {
    try {
      String command = new StringBuilder(128)
                           .append("cd " + repoPath + ";")
                           .append("touch " + tag + ";")
                           .append("git add " + tag + ";")
                           .append("git commit -m 'commit " + tag + "';")
                           .append("git push origin master;")
                           .append("git tag " + tag + ";")
                           .append("git push origin " + tag + ";")
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

  private String addGitCommit(String filename) {
    String command = new StringBuilder(128)
                         .append("cd " + repoPath + ";")
                         .append("touch " + filename + ";")
                         .append("git add " + filename + ";")
                         .append("git commit -m 'commit" + filename + " ';")
                         .append("git push origin master;")
                         .append("git remote update;")
                         .append("git fetch;")
                         .toString();
    return executeCommand(command);
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testDiff() throws GitAPIException, IOException {
    final DiffRequest diffRequest = diffRequestBuilder()
                                        .accountId("accountId")
                                        .branch("master")
                                        .authRequest(new UsernamePasswordAuthRequest(USERNAME, PASSWORD.toCharArray()))
                                        .build();
    doNothing().when(gitClient).ensureRepoLocallyClonedAndUpdated(diffRequest);
    doReturn(repoPath).when(gitClientHelper).getRepoDirectory(diffRequest);
    doNothing().when(gitClient).performGitPull(any(), any());

    addRemote(repoPath);
    addGitCommit("2.txt");
    addGitCommit("3.txt");
    final DiffResult diffResult = gitClient.diff(diffRequest);

    assertThat(diffResult).isNotNull();
    assertThat(diffResult.getCommitTimeMs()).isNotNull();
    assertThat(diffResult.getGitFileChanges().size()).isEqualTo(2);
    assertThat(diffResult.getAccountId()).isNotNull();
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testApplyChangeSetOnFileSystem() throws Exception {
    CommitAndPushRequest gitCommitAndPushRequest =
        commitAndPushRequestBuilder().gitFileChanges(getSampleGitFileChanges()).build();

    List<String> filesToAdd = new ArrayList<>();

    String repoPath = Files.createTempDirectory(UUID.randomUUID().toString()).toString();
    File rootDirectory = new File(repoPath);
    FileUtils.cleanDirectory(rootDirectory);
    createLocalRepo(repoPath);
    Git git = Git.open(rootDirectory);
    // should not delete since files they are not tracked
    gitClient.applyChangeSetOnFileSystem(repoPath, gitCommitAndPushRequest, filesToAdd, git);

    Status status = git.status().call();
    assertThat(status.getAdded()).hasSize(0);
    assertThat(status.getRemoved()).hasSize(0);
    assertThat(status.getUntracked()).isNotEmpty();

    gitClient.applyGitAddCommand(gitCommitAndPushRequest, filesToAdd, git);
    filesToAdd.clear();

    status = git.status().call();
    assertThat(status.getAdded().stream().map(filePath -> Paths.get(filePath).getFileName().toString()))
        .containsExactlyInAnyOrderElementsOf(
            gitCommitAndPushRequest.getGitFileChanges()
                .stream()
                .filter(gfc -> ADD == gfc.getChangeType())
                .map(gitFileChange -> Paths.get(gitFileChange.getFilePath()).getFileName().toString())
                .collect(Collectors.toSet()));

    // should delete the required files
    gitClient.applyChangeSetOnFileSystem(repoPath, gitCommitAndPushRequest, filesToAdd, git);

    status = git.status().call();
    assertThat(status.getRemoved()).isEmpty();
    assertThat(status.getAdded().stream().map(filePath -> Paths.get(filePath).getFileName().toString()))
        .doesNotContainAnyElementsOf(
            gitCommitAndPushRequest.getGitFileChanges()
                .stream()
                .filter(gfc -> DELETE == gfc.getChangeType())
                .map(gitFileChange -> Paths.get(gitFileChange.getFilePath()).getFileName().toString())
                .collect(Collectors.toSet()));

    doGitCommit(git);

    // Test Rename
    Path anyExistingFile = Files.list(Paths.get(repoPath)).findFirst().orElse(null);
    assertThat(anyExistingFile).isNotNull();
    String anyExistingFileName = anyExistingFile.getFileName().toString();
    String newFileName = anyExistingFileName + "-new";
    GitFileChange renameGitFileChange =
        GitFileChange.builder().changeType(RENAME).oldFilePath(anyExistingFileName).filePath(newFileName).build();
    gitCommitAndPushRequest.setGitFileChanges(asList(renameGitFileChange));

    gitClient.applyChangeSetOnFileSystem(repoPath, gitCommitAndPushRequest, filesToAdd, git);

    gitClient.applyGitAddCommand(gitCommitAndPushRequest, filesToAdd, git);
    filesToAdd.clear();
    status = git.status().call();
    assertThat(status.getAdded()).containsExactly(newFileName);
    assertThat(status.getRemoved()).containsExactly(anyExistingFileName);

    // CleanUp
    FileUtils.deleteQuietly(rootDirectory);
  }

  private void doGitCommit(Git git) throws Exception {
    RevCommit revCommit = git.commit().setCommitter("dummy", "dummy@Dummy").setAll(true).setMessage("dummy").call();
  }

  private List<GitFileChange> getSampleGitFileChanges() {
    List<GitFileChange> gitFileChanges = new ArrayList<>();
    for (int i = 0; i < 10; i++) {
      gitFileChanges.add(GitFileChange.builder()
                             .changeType(ADD)
                             .filePath(i + ".txt")
                             .fileContent(i + " " + System.currentTimeMillis())
                             .build());
    }
    for (int i = 0; i < 10; i += 3) {
      gitFileChanges.add(GitFileChange.builder().changeType(DELETE).filePath(i + ".txt").build());
    }
    logger.info(gitFileChanges.toString());
    return gitFileChanges;
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testCommit() {
    final CommitAndPushRequest commitAndPushRequest =
        commitAndPushRequestBuilder().gitFileChanges(getSampleGitFileChanges()).build();
    doNothing().when(gitClient).ensureRepoLocallyClonedAndUpdated(commitAndPushRequest);
    doReturn(repoPath).when(gitClientHelper).getRepoDirectory(commitAndPushRequest);
    final CommitResult commit = gitClient.commit(commitAndPushRequest);
    git.rm();
    assertThat(commit).isNotNull();
    assertThat(commit.getCommitId()).isNotNull();
    assertThat(commit.getCommitMessage()).isNotNull();
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testCommitAndPush() throws Exception {
    doNothing().when(gitClient).updateRemoteOriginInConfig(any(), any());
    List<GitFileChange> gitFileChanges = getSampleGitFileChanges();
    CommitAndPushRequest gitCommitAndPushRequest = commitAndPushRequestBuilder().gitFileChanges(gitFileChanges).build();

    PushResultGit toBeReturned = pushResultBuilder().refUpdate(PushResultGit.RefUpdate.builder().build()).build();
    addRemote(repoPath);
    doNothing().when(gitClient).ensureRepoLocallyClonedAndUpdated(gitCommitAndPushRequest);
    doReturn(repoPath).when(gitClientHelper).getRepoDirectory(gitCommitAndPushRequest);
    doReturn(toBeReturned).when(gitClient).push(gitCommitAndPushRequest);
    git.rm();
    final CommitAndPushResult commitAndPushResult = gitClient.commitAndPush(gitCommitAndPushRequest);
    assertThat(commitAndPushResult).isNotNull();
    assertThat(commitAndPushResult.getFilesCommittedToGit()).isNotNull();
    assertThat(commitAndPushResult.getGitCommitResult()).isNotNull();
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testPush() {
    doNothing().when(gitClient).updateRemoteOriginInConfig(any(), any());

    GitFileChange gitFileChange = GitFileChange.builder().changeType(ADD).filePath(repoPath + "/1.txt").build();
    CommitAndPushRequest gitCommitAndPushRequest =
        commitAndPushRequestBuilder()
            .gitFileChanges(Collections.singletonList(gitFileChange))
            .accountId("accountId")
            .branch("master")
            .authRequest(new UsernamePasswordAuthRequest(USERNAME, PASSWORD.toCharArray()))
            .build();
    doNothing().when(gitClient).ensureRepoLocallyClonedAndUpdated(gitCommitAndPushRequest);
    doReturn(repoPath).when(gitClientHelper).getRepoDirectory(gitCommitAndPushRequest);
    addRemote(repoPath);
    git.rm();
    final PushResultGit pushResultGit = gitClient.push(gitCommitAndPushRequest);
    assertThat(pushResultGit).isNotNull();
    assertThat(pushResultGit.getRefUpdate()).isNotNull();
    assertThat(pushResultGit.getRefUpdate().getExpectedOldObjectId()).isNotNull();
  }

  @Test
  @Owner(developers = ARVIND)
  @Category(UnitTests.class)
  public void testFetchFilesBetweenCommits_Exceptions() throws Exception {
    FetchFilesBwCommitsRequest request =
        fetchFilesBwCommitsRequestBuilder()
            .repoUrl(repoPath)
            .authRequest(new UsernamePasswordAuthRequest(USERNAME, PASSWORD.toCharArray()))
            .branch("master")
            .connectorId("CONNECTOR_ID")
            .accountId("ACCOUNT_ID")
            .build();

    request.setOldCommitId("t1");
    request.setNewCommitId("t2");

    doReturn("").when(gitClientHelper).getLockObject(request.getConnectorId());
    doThrow(new GitClientException("m1", SRE)).when(gitClientHelper).createDirStructureForFileDownload(any());
    assertThatThrownBy(() -> gitClient.fetchFilesBetweenCommits(request))
        .isInstanceOf(GitClientException.class)
        .hasMessageContaining("m1");
  }
}