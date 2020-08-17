package io.harness.git;

import static io.harness.git.Constants.GIT_DEFAULT_LOG_PREFIX;
import static io.harness.git.Constants.GIT_HELM_LOG_PREFIX;
import static io.harness.git.Constants.GIT_TERRAFORM_LOG_PREFIX;
import static io.harness.git.Constants.GIT_TRIGGER_LOG_PREFIX;
import static io.harness.git.Constants.GIT_YAML_LOG_PREFIX;
import static io.harness.rule.OwnerRule.ARVIND;
import static io.harness.rule.OwnerRule.DEEPAK;
import static io.harness.rule.OwnerRule.YOGESH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.common.cache.LoadingCache;
import com.google.inject.Inject;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.exception.GitConnectionDelegateException;
import io.harness.exception.NonPersistentLockException;
import io.harness.filesystem.FileIo;
import io.harness.git.model.GitBaseRequest;
import io.harness.git.model.GitFile;
import io.harness.reflection.ReflectionUtils;
import io.harness.rule.Owner;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.TransportException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;

@Slf4j
public class GitClientHelperTest extends CategoryTest {
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();
  @Inject @InjectMocks GitClientHelper gitClientHelper;

  @Test(expected = GitConnectionDelegateException.class)
  @Owner(developers = DEEPAK)
  @Category(UnitTests.class)
  public void test_checkIfGitConnectivityIssueInCaseOfTransportException() {
    gitClientHelper.checkIfGitConnectivityIssue(
        new GitAPIException("Git Exception", new TransportException("Transport Exception")) {});
  }

  @Test
  @Owner(developers = DEEPAK)
  @Category(UnitTests.class)
  public void test_checkIfGitConnectivityIssueIsNotTrownInCaseOfOtherExceptions() {
    gitClientHelper.checkIfGitConnectivityIssue(new GitAPIException("newTransportException") {});
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void testGetRepoDirectory() {
    final String repoDirectory = gitClientHelper.getRepoDirectory(GitBaseRequest.builder()
                                                                      .connectorId("id")
                                                                      .accountId("accountId")
                                                                      .repoType("HELM")
                                                                      .repoUrl("http://github.com/my-repo")
                                                                      .build());
    assertThat(repoDirectory)
        .isEqualTo("./repository/helm/accountId/id/my-repo/9d0502fc8d289f365a3fdcb24607c878b68fad36");
  }

  @Test
  @Owner(developers = ARVIND)
  @Category(UnitTests.class)
  public void testGetGitLogMessagePrefix() {
    assertThat(gitClientHelper.getGitLogMessagePrefix(null)).isEqualTo(GIT_DEFAULT_LOG_PREFIX);
    assertThat(gitClientHelper.getGitLogMessagePrefix("TERRAFORM")).isEqualTo(GIT_TERRAFORM_LOG_PREFIX);
    assertThat(gitClientHelper.getGitLogMessagePrefix("YAML")).isEqualTo(GIT_YAML_LOG_PREFIX);
    assertThat(gitClientHelper.getGitLogMessagePrefix("TRIGGER")).isEqualTo(GIT_TRIGGER_LOG_PREFIX);
    assertThat(gitClientHelper.getGitLogMessagePrefix("HELM")).isEqualTo(GIT_HELM_LOG_PREFIX);
    assertThat(gitClientHelper.getGitLogMessagePrefix("----")).isEqualTo(GIT_DEFAULT_LOG_PREFIX);
  }

  @Test
  @Owner(developers = ARVIND)
  @Category(UnitTests.class)
  public void testGetLockObject() throws Exception {
    Field cache = ReflectionUtils.getFieldByName(gitClientHelper.getClass(), "cache");
    cache.setAccessible(true);
    LoadingCache<String, Object> cacheMap = (LoadingCache<String, Object>) cache.get(gitClientHelper);
    assertThat(cacheMap.size()).isEqualTo(0L);
    assertThatThrownBy(() -> gitClientHelper.getLockObject(null)).isInstanceOf(NonPersistentLockException.class);
    gitClientHelper.getLockObject("123");
    assertThat(cacheMap.size()).isEqualTo(1L);
  }

  @Test
  @Owner(developers = ARVIND)
  @Category(UnitTests.class)
  public void testGetFileDownloadRepoDirectory() {
    final String repoDirectory = gitClientHelper.getFileDownloadRepoDirectory(GitBaseRequest.builder()
                                                                                  .connectorId("id")
                                                                                  .accountId("accountId")
                                                                                  .repoType("HELM")
                                                                                  .repoUrl("http://github.com/my-repo")
                                                                                  .build());
    assertThat(repoDirectory)
        .isEqualTo("./repository/gitFileDownloads/accountId/id/my-repo/9d0502fc8d289f365a3fdcb24607c878b68fad36");
  }

  @Test
  @Owner(developers = ARVIND)
  @Category(UnitTests.class)
  public void testCreateDirStructureForFileDownload() throws Exception {
    gitClientHelper.createDirStructureForFileDownload(GitBaseRequest.builder()
                                                          .connectorId("id")
                                                          .accountId("accountId")
                                                          .repoType("HELM")
                                                          .repoUrl("http://github.com/my-repo")
                                                          .build());

    assertThat(FileIo.checkIfFileExist(
                   "./repository/gitFileDownloads/accountId/id/my-repo/9d0502fc8d289f365a3fdcb24607c878b68fad36"))
        .isEqualTo(true);
    FileIo.deleteDirectoryAndItsContentIfExists("./repository");
  }

  @Test
  @Owner(developers = ARVIND)
  @Category(UnitTests.class)
  public void testAddFiles() throws Exception {
    String repoPath = "./repository/repo/";
    String filePath = "internalPath/1.txt";
    Path repoFilePath = Paths.get(repoPath + filePath);
    FileIo.createDirectoryIfDoesNotExist(repoPath + "/internalPath");
    FileIo.writeFile(repoFilePath, "ABC\nDEF".getBytes());
    Stream<Path> walk = Files.walk(repoFilePath, 1);
    List<GitFile> files = new LinkedList<>();
    walk.forEach(path -> { gitClientHelper.addFiles(files, path, repoPath); });

    assertThat(files.size()).isEqualTo(1);
    assertThat(files.get(0).getFilePath()).isEqualTo(filePath);
    assertThat(files.get(0).getFileContent()).isEqualTo("ABC\nDEF\n");
    FileIo.deleteDirectoryAndItsContentIfExists("./repository");
  }
}