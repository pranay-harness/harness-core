package io.harness.cdng.gitclient;

import static io.harness.rule.OwnerRule.ABHINAV;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Fail.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.connector.gitconnector.GitAuthType;
import io.harness.delegate.beans.connector.gitconnector.GitConfigDTO;
import io.harness.delegate.beans.connector.gitconnector.GitConnectionType;
import io.harness.delegate.beans.connector.gitconnector.GitHTTPAuthenticationDTO;
import io.harness.delegate.beans.git.GitCommitAndPushRequest;
import io.harness.delegate.beans.git.GitFileChange;
import io.harness.delegate.beans.git.GitFileChange.ChangeType;
import io.harness.delegate.beans.git.GitPushResult;
import io.harness.rule.Owner;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;
import org.zeroturnaround.exec.stream.LogOutputStream;
import software.wings.WingsBaseTest;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

@Slf4j
public class GitClientNGTest extends WingsBaseTest {
  public static final String ACCOUNT_ID = "ACCOUNT_ID";
  @Inject @Spy GitClientNGImpl gitClientNG;
  @Inject GitClientHelperNG gitClientHelper;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testValidate() throws GitAPIException {
    GitConfigDTO gitConfig = GitConfigDTO.builder()
                                 .gitAuth(GitHTTPAuthenticationDTO.builder()
                                              .gitConnectionType(GitConnectionType.REPO)
                                              .accountId(ACCOUNT_ID)
                                              .branchName("branchName")
                                              .encryptedPassword("abcd")
                                              .url("http://url.com")
                                              .username("username")
                                              .build())
                                 .gitAuthType(GitAuthType.HTTP)
                                 .build();
    doThrow(new JGitInternalException("Exception caught during execution of ls-remote command"))
        .when(gitClientNG)
        .initGitAndGetBranches(any());
    gitClientNG.validate(gitConfig);
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testApplyChangeSetOnFileSystem() throws Exception {
    GitConfigDTO gitConfig = GitConfigDTO.builder().accountId(ACCOUNT_ID).build();
    GitCommitAndPushRequest gitCommitAndPushRequest =
        GitCommitAndPushRequest.builder().gitFileChanges(getSampleGitFileChanges()).build();

    List<String> filesToAdd = new ArrayList<>();

    String repoPath = Files.createTempDirectory(UUID.randomUUID().toString()).toString();
    File rootDirectory = new File(repoPath);
    FileUtils.cleanDirectory(rootDirectory);
    createLocalRepo(repoPath);
    Git git = Git.open(rootDirectory);
    // should not delete since files they are not tracked
    gitClientNG.applyChangeSetOnFileSystem(repoPath, gitConfig, gitCommitAndPushRequest, filesToAdd, git);

    Status status = git.status().call();
    assertThat(status.getAdded()).hasSize(0);
    assertThat(status.getRemoved()).hasSize(0);
    assertThat(status.getUntracked()).isNotEmpty();

    gitClientNG.applyGitAddCommand(filesToAdd, git);
    filesToAdd.clear();

    status = git.status().call();
    assertThat(status.getAdded().stream().map(filePath -> Paths.get(filePath).getFileName().toString()))
        .containsExactlyInAnyOrderElementsOf(
            gitCommitAndPushRequest.getGitFileChanges()
                .stream()
                .filter(gfc -> ChangeType.ADD == gfc.getChangeType())
                .map(gitFileChange -> Paths.get(gitFileChange.getFilePath()).getFileName().toString())
                .collect(Collectors.toSet()));

    // should delete the required files
    gitClientNG.applyChangeSetOnFileSystem(repoPath, gitConfig, gitCommitAndPushRequest, filesToAdd, git);

    status = git.status().call();
    assertThat(status.getRemoved()).isEmpty();
    assertThat(status.getAdded().stream().map(filePath -> Paths.get(filePath).getFileName().toString()))
        .doesNotContainAnyElementsOf(
            gitCommitAndPushRequest.getGitFileChanges()
                .stream()
                .filter(gfc -> ChangeType.DELETE == gfc.getChangeType())
                .map(gitFileChange -> Paths.get(gitFileChange.getFilePath()).getFileName().toString())
                .collect(Collectors.toSet()));

    doGitCommit(git);

    // CleanUp
    FileUtils.deleteQuietly(rootDirectory);
  }

  private void createLocalRepo(String repoPath) {
    String command = new StringBuilder(128)
                         .append("mkdir -p " + repoPath + ";")
                         .append("cd " + repoPath + ";")
                         .append("git init;")
                         .toString();

    executeCommand(command);
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

  private List<GitFileChange> getSampleGitFileChanges() {
    List<GitFileChange> gitFileChanges = new ArrayList<>();
    for (int i = 0; i < 10; i++) {
      gitFileChanges.add(GitFileChange.builder()
                             .changeType(ChangeType.ADD)
                             .filePath(i + ".txt")
                             .fileContent(i + " " + System.currentTimeMillis())
                             .build());
    }
    for (int i = 0; i < 10; i += 3) {
      gitFileChanges.add(GitFileChange.builder().changeType(ChangeType.DELETE).filePath(i + ".txt").build());
    }
    logger.info(gitFileChanges.toString());
    return gitFileChanges;
  }

  private void doGitCommit(Git git) throws Exception {
    RevCommit revCommit = git.commit().setCommitter("dummy", "dummy@Dummy").setAll(true).setMessage("dummy").call();
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testCommitAndPush() throws IOException {
    final String GIT_REPO_BASE_DIR = "./repository/" + ACCOUNT_ID + "/repoName";
    doNothing().when(gitClientNG).updateRemoteOriginInConfig(any(), any());
    List<GitFileChange> gitFileChanges = getSampleGitFileChanges();
    GitCommitAndPushRequest gitCommitAndPushRequest =
        GitCommitAndPushRequest.builder().gitFileChanges(gitFileChanges).build();

    List<String> filesToAdd = new ArrayList<>();
    String repoPath = GIT_REPO_BASE_DIR;

    File rootDirectory = new File(repoPath);
    rootDirectory.mkdirs();
    FileUtils.cleanDirectory(rootDirectory);
    createLocalRepo(repoPath);
    GitConfigDTO gitConfig = GitConfigDTO.builder()
                                 .accountId(ACCOUNT_ID)
                                 .gitAuth(GitHTTPAuthenticationDTO.builder().url(repoPath).build())
                                 .build();
    Git git = Git.open(rootDirectory);
    git.remoteSetUrl();
    doNothing().when(gitClientNG).ensureRepoLocallyClonedAndUpdated(any(), any(), any());
    doReturn(GitPushResult.builder().build()).when(gitClientNG).push(any(), any(), any());
    doReturn(gitFileChanges).when(gitClientNG).getFilesCommited(any(), any(), any());

    gitClientNG.applyChangeSetOnFileSystem(repoPath, gitConfig, gitCommitAndPushRequest, filesToAdd, git);

    gitClientNG.commitAndPush(gitConfig, gitCommitAndPushRequest, ACCOUNT_ID, null);
  }
}
