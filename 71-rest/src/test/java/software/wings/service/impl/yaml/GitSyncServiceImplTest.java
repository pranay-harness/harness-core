package software.wings.service.impl.yaml;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.DEEPAK;
import static io.harness.rule.OwnerRule.VARDAN_BANSAL;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import software.wings.WingsBaseTest;
import software.wings.beans.Application;
import software.wings.beans.EntityType;
import software.wings.beans.GitCommit;
import software.wings.beans.GitCommit.GitCommitKeys;
import software.wings.beans.GitConfig;
import software.wings.beans.GitDetail;
import software.wings.beans.SettingAttribute;
import software.wings.beans.yaml.Change.ChangeType;
import software.wings.beans.yaml.GitDiffResult;
import software.wings.beans.yaml.GitFileChange;
import software.wings.dl.WingsPersistence;
import software.wings.yaml.errorhandling.GitSyncError;
import software.wings.yaml.gitSync.GitFileActivity;
import software.wings.yaml.gitSync.GitFileActivity.GitFileActivityKeys;
import software.wings.yaml.gitSync.GitFileActivity.Status;
import software.wings.yaml.gitSync.GitFileProcessingSummary;
import software.wings.yaml.gitSync.YamlChangeSet;
import software.wings.yaml.gitSync.YamlGitConfig;

import java.util.Arrays;
import java.util.List;

public class GitSyncServiceImplTest extends WingsBaseTest {
  @InjectMocks @Inject private GitSyncServiceImpl gitSyncService;
  @Inject private WingsPersistence wingsPersistence;
  private String accountId = generateUuid();
  private String uuid = generateUuid();

  @Test
  @Owner(developers = VARDAN_BANSAL)
  @Category(UnitTests.class)
  public void test_shouldListErrors() {
    final GitSyncError gitSyncError1 = GitSyncError.builder()
                                           .gitCommitId("gitCommitId1")
                                           .yamlFilePath("yamlFilePath1")
                                           .yamlContent("yamlContent1")
                                           .accountId(accountId)
                                           .build();

    final GitSyncError gitSyncError2 = GitSyncError.builder()
                                           .gitCommitId("gitCommitId2")
                                           .yamlFilePath("yamlFilePath2")
                                           .yamlContent("yamlContent2")
                                           .accountId(accountId)
                                           .build();

    wingsPersistence.save(Arrays.asList(gitSyncError1, gitSyncError2));

    final PageRequest pageRequest = aPageRequest().withOffset("0").withLimit("2").build();

    final PageResponse<GitSyncError> errorList = gitSyncService.fetchErrors(pageRequest);
    assertThat(errorList.size()).isEqualTo(2);
  }

  @Test
  @Owner(developers = VARDAN_BANSAL)
  @Category(UnitTests.class)
  public void test_discardGitSyncErrorsForGivenIds() {
    final GitSyncError gitSyncError = GitSyncError.builder()
                                          .gitCommitId("gitCommitId")
                                          .yamlFilePath("yamlFilePath")
                                          .yamlContent("yamlContent")
                                          .accountId(accountId)
                                          .build();

    wingsPersistence.save(gitSyncError);
    gitSyncService.deleteGitSyncErrorAndLogFileActivity(
        Arrays.asList(gitSyncError), GitFileActivity.Status.DISCARDED, accountId);
    assertThat(wingsPersistence.get(GitSyncError.class, gitSyncError.getUuid())).isEqualTo(null);
  }

  @Test
  @Owner(developers = VARDAN_BANSAL)
  @Category(UnitTests.class)
  public void test_fetchRepositories() {
    final YamlGitConfig expectedYamlGitConfig = YamlGitConfig.builder()
                                                    .accountId(accountId)
                                                    .branchName("branchName")
                                                    .enabled(true)
                                                    .entityId(uuid)
                                                    .entityType(EntityType.APPLICATION)
                                                    .gitConnectorId(generateUuid())
                                                    .build();

    wingsPersistence.save(expectedYamlGitConfig);
    final Application application = Application.Builder.anApplication()
                                        .yamlGitConfig(expectedYamlGitConfig)
                                        .accountId(accountId)
                                        .appId(expectedYamlGitConfig.getEntityId())
                                        .uuid(uuid)
                                        .name("applicationName")
                                        .build();
    wingsPersistence.save(application);

    final SettingAttribute gitConfig = SettingAttribute.Builder.aSettingAttribute()
                                           .withAccountId(accountId)
                                           .withValue(GitConfig.builder().branch("branchName").build())
                                           .withUuid(expectedYamlGitConfig.getGitConnectorId())
                                           .build();
    wingsPersistence.save(gitConfig);
    final List<GitDetail> gitDetails = gitSyncService.fetchRepositories(accountId);
    assertThat(gitDetails.size()).isEqualTo(1);
    final GitDetail gitDetail = gitDetails.get(0);
    assertThat(gitDetail.getBranchName()).isEqualTo("branchName");
  }

  @Test
  @Owner(developers = VARDAN_BANSAL)
  @Category(UnitTests.class)
  public void test_fetchGitCommits() {
    final GitCommit gitCommit =
        GitCommit.builder()
            .commitId("commitId")
            .yamlGitConfigId("yamlGitConfigId")
            .fileProcessingSummary(
                GitFileProcessingSummary.builder().totalCount(10L).successCount(5L).failureCount(5L).build())
            .accountId(accountId)
            .status(GitCommit.Status.COMPLETED)
            .yamlChangeSet(YamlChangeSet.builder()
                               .gitFileChanges(Arrays.asList(GitFileChange.Builder.aGitFileChange()
                                                                 .withAccountId(accountId)
                                                                 .withChangeType(ChangeType.ADD)
                                                                 .build()))
                               .gitToHarness(true)
                               .build())
            .build();

    wingsPersistence.save(gitCommit);
    final PageResponse pageResponse =
        gitSyncService.fetchGitCommits(aPageRequest().withLimit("1").build(), true, accountId);
    assertThat(pageResponse).isNotNull();
    final List<GitCommit> responseList = pageResponse.getResponse();
    assertThat(responseList.size()).isEqualTo(1);
    assertThat(responseList.get(0).getAccountId()).isEqualTo(accountId);
  }

  @Test
  @Owner(developers = VARDAN_BANSAL)
  @Category(UnitTests.class)
  public void test_fetchGitSyncActivity() {
    final GitFileActivity fileActivity = GitFileActivity.builder()
                                             .commitId("commitId")
                                             .accountId(accountId)
                                             .fileContent("some file content")
                                             .triggeredBy(GitFileActivity.TriggeredBy.USER)
                                             .status(GitFileActivity.Status.SUCCESS)
                                             .build();

    wingsPersistence.save(fileActivity);
    final PageResponse pageResponse = gitSyncService.fetchGitSyncActivity(aPageRequest().withLimit("1").build());
    assertThat(pageResponse).isNotNull();
    final List<GitFileActivity> responseList = pageResponse.getResponse();
    assertThat(responseList.size()).isEqualTo(1);
    assertThat(responseList.get(0).getAccountId()).isEqualTo(accountId);
  }

  @Test
  @Owner(developers = VARDAN_BANSAL)
  @Category(UnitTests.class)
  public void test_shouldLogActivityForFiles() {
    final String commitId = "commitId";
    final String filePath = "file1.yaml";
    wingsPersistence.save(GitFileActivity.builder()
                              .accountId(accountId)
                              .commitId(commitId)
                              .processingCommitId(commitId)
                              .filePath(filePath)
                              .status(Status.QUEUED)
                              .build());
    gitSyncService.logActivityForFiles(commitId, Arrays.asList(filePath), Status.SUCCESS, "", accountId);
    final GitFileActivity fileActivity = wingsPersistence.createQuery(GitFileActivity.class)
                                             .filter(GitFileActivityKeys.accountId, accountId)
                                             .filter(GitFileActivityKeys.processingCommitId, commitId)
                                             .get();
    assertThat(fileActivity).isNotNull();
    assertThat(fileActivity.getCommitId()).isEqualTo(commitId);
    assertThat(fileActivity.getStatus()).isEqualTo(Status.SUCCESS);
    assertThat(fileActivity.getFilePath()).isEqualTo(filePath);
  }

  @Test
  @Owner(developers = VARDAN_BANSAL)
  @Category(UnitTests.class)
  public void test_shouldLogActivityForGitOperation() {
    final String commitId = "commitId";
    final String filePath = "file1.yaml";
    gitSyncService.logActivityForGitOperation(Arrays.asList(GitFileChange.Builder.aGitFileChange()
                                                                .withFilePath(filePath)
                                                                .withAccountId(accountId)
                                                                .withCommitId(commitId)
                                                                .withProcessingCommitId("commitId")
                                                                .withChangeFromAnotherCommit(Boolean.TRUE)
                                                                .build()),
        Status.SUCCESS, true, false, accountId, commitId);
    final GitFileActivity fileActivity = wingsPersistence.createQuery(GitFileActivity.class)
                                             .filter(GitFileActivityKeys.accountId, accountId)
                                             .filter(GitFileActivityKeys.processingCommitId, commitId)
                                             .get();
    assertThat(fileActivity).isNotNull();
    assertThat(fileActivity.getCommitId()).isEqualTo(commitId);
    assertThat(fileActivity.getStatus()).isEqualTo(Status.SUCCESS);
    assertThat(fileActivity.getFilePath()).isEqualTo(filePath);
  }

  @Test
  @Owner(developers = VARDAN_BANSAL)
  @Category(UnitTests.class)
  public void test_shouldLogActivityForSkippedFiles() {
    final String commitId = "commitId";
    final String filePath = "file1.yaml";
    wingsPersistence.save(GitFileActivity.builder()
                              .accountId(accountId)
                              .commitId(commitId)
                              .processingCommitId(commitId)
                              .filePath(filePath)
                              .status(Status.QUEUED)
                              .build());
    final GitFileChange changeFile1 = GitFileChange.Builder.aGitFileChange()
                                          .withFilePath(filePath)
                                          .withAccountId(accountId)
                                          .withCommitId(commitId)
                                          .withProcessingCommitId(commitId)
                                          .build();
    final String commitId1 = commitId.concat("_1");
    final GitFileChange changeFile2 = GitFileChange.Builder.aGitFileChange()
                                          .withFilePath(filePath.concat("_1"))
                                          .withAccountId(accountId)
                                          .withCommitId(commitId1)
                                          .withProcessingCommitId(commitId1)
                                          .build();
    gitSyncService.logActivityForSkippedFiles(Arrays.asList(changeFile2),
        GitDiffResult.builder().commitId(commitId).gitFileChanges(Arrays.asList(changeFile1, changeFile2)).build(),
        "skipped for testing", accountId);
    final GitFileActivity fileActivity = wingsPersistence.createQuery(GitFileActivity.class)
                                             .filter(GitFileActivityKeys.accountId, accountId)
                                             .filter(GitFileActivityKeys.processingCommitId, commitId)
                                             .get();
    assertThat(fileActivity).isNotNull();
    assertThat(fileActivity.getCommitId()).isEqualTo(commitId);
    assertThat(fileActivity.getStatus()).isEqualTo(Status.SKIPPED);
    assertThat(fileActivity.getFilePath()).isEqualTo(filePath);
    assertThat(fileActivity.getErrorMessage()).isEqualTo("skipped for testing");
  }

  @Test
  @Owner(developers = VARDAN_BANSAL)
  @Category(UnitTests.class)
  public void test_shouldAddFileProcessingSummaryToGitCommit() {
    final String commitId = "commitId";
    wingsPersistence.save(GitCommit.builder()
                              .commitId(commitId)
                              .yamlGitConfigId("yamlGitConfigId")
                              .accountId(accountId)
                              .status(GitCommit.Status.COMPLETED)
                              .yamlChangeSet(YamlChangeSet.builder()
                                                 .gitFileChanges(Arrays.asList(GitFileChange.Builder.aGitFileChange()
                                                                                   .withAccountId(accountId)
                                                                                   .withChangeType(ChangeType.ADD)
                                                                                   .build()))
                                                 .build())
                              .build());

    wingsPersistence.save(Arrays.asList(GitFileActivity.builder()
                                            .accountId(accountId)
                                            .commitId(commitId)
                                            .processingCommitId(commitId)
                                            .filePath("filePath1")
                                            .status(Status.QUEUED)
                                            .build(),
        GitFileActivity.builder()
            .accountId(accountId)
            .commitId(commitId)
            .processingCommitId(commitId)
            .filePath("filePath2")
            .status(Status.SUCCESS)
            .build(),
        GitFileActivity.builder()
            .accountId(accountId)
            .commitId(commitId)
            .processingCommitId(commitId)
            .filePath("filePath2")
            .status(Status.FAILED)
            .build()));

    gitSyncService.addFileProcessingSummaryToGitCommit(commitId, accountId,
        Arrays.asList(GitFileChange.Builder.aGitFileChange()
                          .withFilePath("filePath")
                          .withAccountId(accountId)
                          .withCommitId(commitId)
                          .withProcessingCommitId(commitId)
                          .build()));

    final GitCommit gitCommit = wingsPersistence.createQuery(GitCommit.class)
                                    .filter(GitCommitKeys.accountId, accountId)
                                    .filter(GitCommitKeys.commitId, commitId)
                                    .get();

    assertThat(gitCommit).isNotNull();
    assertThat(gitCommit.getFileProcessingSummary()).isNotNull();
  }
  @Owner(developers = DEEPAK)
  @Category(UnitTests.class)
  public void test_fetchGitToHarnessErrors() {
    final String commitId = "gitCommitId";
    // Saving GitSyncError
    final GitSyncError gitSyncError =
        GitSyncError.builder().accountId(accountId).fullSyncPath(false).gitCommitId(commitId).build();
    gitSyncError.setCreatedAt(System.currentTimeMillis());
    String id = wingsPersistence.save(gitSyncError);

    PageRequest<GitToHarnessErrorCommitStats> req = aPageRequest().withLimit("2").withOffset("0").build();
    List<GitToHarnessErrorCommitStats> errorsList =
        gitSyncService.fetchGitToHarnessErrors(req, accountId, null).getResponse();
    assertThat(errorsList.size()).isEqualTo(1);
    GitToHarnessErrorCommitStats error = errorsList.get(0);
    assertThat(error.getFailedCount()).isEqualTo(1);
  }

  @Test
  @Owner(developers = DEEPAK)
  @Category(UnitTests.class)
  public void test_fetchGitToHarnessErrorsCommitWise() {
    final String commitId = "gitCommitId";
    // Saving GitSyncError
    final GitSyncError gitSyncError =
        GitSyncError.builder().accountId(accountId).fullSyncPath(false).gitCommitId(commitId).build();
    gitSyncError.setCreatedAt(System.currentTimeMillis());
    String id = wingsPersistence.save(gitSyncError);

    PageRequest<GitSyncError> req = aPageRequest().withLimit("2").withOffset("0").build();
    List<GitSyncError> errorsList = gitSyncService.fetchErrorsInEachCommits(req, commitId, accountId).getResponse();
    assertThat(errorsList.size()).isEqualTo(1);
    GitSyncError error = errorsList.get(0);
    assertThat(error.equals(gitSyncError)).isTrue();
  }

  @Test
  @Owner(developers = VARDAN_BANSAL)
  @Category(UnitTests.class)
  public void test_shouldMarkRemainingFilesAsSkipped() {
    final String commitId = "gitCommitId";
    wingsPersistence.save(GitFileActivity.builder()
                              .accountId(accountId)
                              .commitId(commitId)
                              .processingCommitId(commitId)
                              .filePath("filePath1")
                              .status(Status.QUEUED)
                              .build());
    gitSyncService.markRemainingFilesAsSkipped(commitId, accountId);
    final GitFileActivity fileActivity = wingsPersistence.createQuery(GitFileActivity.class)
                                             .filter(GitFileActivityKeys.accountId, accountId)
                                             .filter(GitFileActivityKeys.processingCommitId, commitId)
                                             .get();
    assertThat(fileActivity).isNotNull();
    assertThat(fileActivity.getStatus()).isEqualTo(Status.SKIPPED);
  }
}
