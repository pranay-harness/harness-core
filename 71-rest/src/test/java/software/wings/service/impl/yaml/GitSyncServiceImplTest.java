package software.wings.service.impl.yaml;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.DEEPAK;
import static io.harness.rule.OwnerRule.VARDAN_BANSAL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static software.wings.beans.GitCommit.Status.COMPLETED;
import static software.wings.beans.GitCommit.Status.FAILED;
import static software.wings.service.intfc.security.SecretManager.ACCOUNT_ID_KEY;

import com.google.inject.Inject;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.EntityType;
import software.wings.beans.GitCommit;
import software.wings.beans.GitConfig;
import software.wings.beans.GitDetail;
import software.wings.beans.GitFileActivitySummary;
import software.wings.beans.SettingAttribute;
import software.wings.beans.yaml.GitDiffResult;
import software.wings.beans.yaml.GitFileChange;
import software.wings.dl.WingsPersistence;
import software.wings.exception.YamlProcessingException;
import software.wings.service.intfc.yaml.sync.GitSyncErrorService;
import software.wings.service.intfc.yaml.sync.YamlGitConfigService;
import software.wings.yaml.errorhandling.GitSyncError;
import software.wings.yaml.errorhandling.GitToHarnessErrorDetails;
import software.wings.yaml.gitSync.GitFileActivity;
import software.wings.yaml.gitSync.GitFileActivity.GitFileActivityKeys;
import software.wings.yaml.gitSync.GitFileActivity.Status;
import software.wings.yaml.gitSync.GitFileProcessingSummary;
import software.wings.yaml.gitSync.YamlGitConfig;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GitSyncServiceImplTest extends WingsBaseTest {
  @InjectMocks @Inject private GitSyncServiceImpl gitSyncService;
  @Inject private WingsPersistence wingsPersistence;
  @InjectMocks @Inject private GitSyncErrorService gitSyncErrorService;
  @Mock YamlGitConfigService yamlGitConfigService;
  private String accountId = generateUuid();
  private String uuid = generateUuid();
  private String gitConnectorName = "gitConnectorName";
  private String repoURL = "https://abc.com";
  String branchName1 = "branchName1";
  private String gitConnectorId;

  @Before
  public void setup() {
    final SettingAttribute gitConfig = SettingAttribute.Builder.aSettingAttribute()
                                           .withAccountId(accountId)
                                           .withName(gitConnectorName)
                                           .withValue(GitConfig.builder().repoUrl(repoURL).branch("branchName").build())
                                           .build();
    gitConnectorId = wingsPersistence.save(gitConfig);
  }

  @Test
  @Owner(developers = VARDAN_BANSAL)
  @Category(UnitTests.class)
  public void test_shouldListErrors() {
    GitToHarnessErrorDetails gitToHarnessErrorDetails =
        GitToHarnessErrorDetails.builder().gitCommitId("gitCommitId1").yamlContent("yamlContent").build();
    GitToHarnessErrorDetails gitToHarnessErrorDetails1 =
        GitToHarnessErrorDetails.builder().gitCommitId("gitCommitId2").yamlContent("yamlContent").build();
    final GitSyncError gitSyncError1 = GitSyncError.builder()
                                           .yamlFilePath("yamlFilePath1")
                                           .additionalErrorDetails(gitToHarnessErrorDetails)
                                           .accountId(accountId)
                                           .build();

    final GitSyncError gitSyncError2 = GitSyncError.builder()
                                           .yamlFilePath("yamlFilePath2")
                                           .additionalErrorDetails(gitToHarnessErrorDetails1)
                                           .accountId(accountId)
                                           .build();

    wingsPersistence.save(Arrays.asList(gitSyncError1, gitSyncError2));

    final PageRequest pageRequest = aPageRequest().withOffset("0").withLimit("2").build();

    final PageResponse<GitSyncError> errorList = gitSyncErrorService.fetchErrors(pageRequest);
    assertThat(errorList.size()).isEqualTo(2);
  }

  @Test
  @Owner(developers = DEEPAK)
  @Category(UnitTests.class)
  public void test_fetchRepositories() {
    String applicationName = "app";
    String accountName = "account";
    final YamlGitConfig yamlGitConfig = YamlGitConfig.builder()
                                            .accountId(accountId)
                                            .branchName("branchName")
                                            .enabled(true)
                                            .entityId(uuid)
                                            .entityType(EntityType.APPLICATION)
                                            .gitConnectorId(gitConnectorId)
                                            .entityName(applicationName)
                                            .build();
    final YamlGitConfig yamlGitConfig1 = YamlGitConfig.builder()
                                             .accountId(accountId)
                                             .branchName(branchName1)
                                             .enabled(true)
                                             .entityId(uuid)
                                             .entityType(EntityType.ACCOUNT)
                                             .entityName(accountName)
                                             .gitConnectorId(gitConnectorId)
                                             .build();

    List<YamlGitConfig> yamlGitChangeSets = new ArrayList<>(Arrays.asList(yamlGitConfig, yamlGitConfig1));
    when(yamlGitConfigService.getYamlGitConfigAccessibleToUserWithEntityName(accountId)).thenReturn(yamlGitChangeSets);
    List<GitDetail> gitDetails = gitSyncService.fetchRepositoriesAccessibleToUser(accountId);
    assertThat(gitDetails.size()).isEqualTo(2);
    GitDetail gitDetail1 = gitDetails.get(0);
    GitDetail gitDetail2 = gitDetails.get(1);
    GitDetail accountLevelGitDetail = gitDetail1.getEntityType() == EntityType.ACCOUNT ? gitDetail1 : gitDetail2;
    GitDetail appLevelGitDetail = gitDetail1.getEntityType() == EntityType.APPLICATION ? gitDetail1 : gitDetail2;

    assertThat(accountLevelGitDetail.getBranchName()).isEqualTo(branchName1);
    assertThat(appLevelGitDetail.getBranchName()).isEqualTo("branchName");

    assertThat(accountLevelGitDetail.getConnectorName()).isEqualTo(gitConnectorName);
    assertThat(appLevelGitDetail.getConnectorName()).isEqualTo(gitConnectorName);

    assertThat(accountLevelGitDetail.getEntityName()).isEqualTo(accountName);
    assertThat(appLevelGitDetail.getEntityName()).isEqualTo(applicationName);
  }

  @Test
  @Owner(developers = DEEPAK)
  @Category(UnitTests.class)
  public void test_fetchGitCommits() {
    String appId = "appId";
    String commitId = "commitId";
    String commitMessage = "commitMessage";
    final GitFileActivitySummary gitFileActivitySummary =
        GitFileActivitySummary.builder()
            .commitId(commitId)
            .appId(appId)
            .commitMessage(commitMessage)
            .branchName(branchName1)
            .fileProcessingSummary(
                GitFileProcessingSummary.builder().totalCount(10L).successCount(5L).failureCount(5L).build())
            .accountId(accountId)
            .gitConnectorId(gitConnectorId)
            .status(COMPLETED)
            .gitToHarness(true)
            .build();

    wingsPersistence.save(gitFileActivitySummary);
    final PageResponse pageResponse =
        gitSyncService.fetchGitCommits(aPageRequest().withLimit("1").build(), true, accountId, appId);
    assertThat(pageResponse).isNotNull();
    final List<GitFileActivitySummary> responseList = pageResponse.getResponse();
    assertThat(responseList.size()).isEqualTo(1);
    assertThat(responseList.get(0).getAccountId()).isEqualTo(accountId);
    assertThat(responseList.get(0).getStatus()).isEqualTo(COMPLETED);
    assertThat(responseList.get(0).getGitToHarness()).isEqualTo(true);
    assertThat(responseList.get(0).getAppId()).isEqualTo(appId);
    assertThat(responseList.get(0).getCommitMessage()).isEqualTo(commitMessage);
    assertThat(responseList.get(0).getCommitId()).isEqualTo(commitId);
    assertThat(responseList.get(0).getBranchName()).isEqualTo(branchName1);
    assertThat(responseList.get(0).getGitConnectorId()).isEqualTo(gitConnectorId);
  }

  @Test
  @Owner(developers = VARDAN_BANSAL)
  @Category(UnitTests.class)
  public void test_fetchGitSyncActivity() {
    String appId = "appId";
    final GitFileActivity fileActivity = GitFileActivity.builder()
                                             .commitId("commitId")
                                             .accountId(accountId)
                                             .fileContent("some file content")
                                             .triggeredBy(GitFileActivity.TriggeredBy.USER)
                                             .gitConnectorId(gitConnectorId)
                                             .appId(appId)
                                             .status(GitFileActivity.Status.SUCCESS)
                                             .build();

    wingsPersistence.save(fileActivity);
    final PageResponse pageResponse =
        gitSyncService.fetchGitSyncActivity(aPageRequest().withLimit("1").build(), accountId, appId, false);
    assertThat(pageResponse).isNotNull();
    final List<GitFileActivity> responseList = pageResponse.getResponse();
    assertThat(responseList.size()).isEqualTo(1);
    assertThat(responseList.get(0).getAccountId()).isEqualTo(accountId);
  }

  @Test
  @Owner(developers = VARDAN_BANSAL)
  @Category(UnitTests.class)
  public void test_shouldupdateStatusOfGitFileActivity() {
    final String commitId = "commitId";
    final String filePath = "file1.yaml";
    wingsPersistence.save(GitFileActivity.builder()
                              .accountId(accountId)
                              .commitId(commitId)
                              .processingCommitId(commitId)
                              .filePath(filePath)
                              .status(Status.QUEUED)
                              .build());
    gitSyncService.updateStatusOfGitFileActivity(commitId, Arrays.asList(filePath), Status.SUCCESS, "", accountId);
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
    final String commitMessage = "commitMessage";
    gitSyncService.logActivityForGitOperation(
        Arrays.asList(GitFileChange.Builder.aGitFileChange()
                          .withYamlGitConfig(
                              YamlGitConfig.builder().branchName("branchName").gitConnectorId("gitConnectorId").build())
                          .withFilePath(filePath)
                          .withCommitMessage(commitMessage)
                          .withAccountId(accountId)
                          .withCommitId(commitId)
                          .withChangeFromAnotherCommit(Boolean.TRUE)
                          .build()),
        Status.SUCCESS, true, false, accountId, commitId, commitMessage);
    final GitFileActivity fileActivity = wingsPersistence.createQuery(GitFileActivity.class)
                                             .filter(GitFileActivityKeys.accountId, accountId)
                                             .filter(GitFileActivityKeys.commitId, commitId)
                                             .get();
    assertThat(fileActivity).isNotNull();
    assertThat(fileActivity.getCommitId()).isEqualTo(commitId);
    assertThat(fileActivity.getStatus()).isEqualTo(Status.SUCCESS);
    assertThat(fileActivity.getFilePath()).isEqualTo(filePath);
    assertThat(fileActivity.getCommitId()).isEqualTo(commitId);
    assertThat(fileActivity.getCommitMessage()).isEqualTo(commitMessage);
    assertThat(fileActivity.getProcessingCommitId()).isEqualTo(commitId);
    assertThat(fileActivity.getProcessingCommitMessage()).isEqualTo(commitMessage);
  }

  @Test
  @Owner(developers = DEEPAK)
  @Category(UnitTests.class)
  public void test_shouldLogActivityForGitOperationForExtraFiles() {
    final String commitId = "commitId";
    final String filePath = "file1.yaml";
    final String commitMessage = "commitMessage";
    final String processingCommitId = "processingCommitId";
    final String processingCommitMessage = "processingCommitMessage";
    gitSyncService.logActivityForGitOperation(
        Arrays.asList(GitFileChange.Builder.aGitFileChange()
                          .withYamlGitConfig(
                              YamlGitConfig.builder().branchName("branchName").gitConnectorId("gitConnectorId").build())
                          .withFilePath(filePath)
                          .withAccountId(accountId)
                          .withProcessingCommitId(processingCommitId)
                          .withCommitId(commitId)
                          .withProcessingCommitMessage(processingCommitMessage)
                          .withCommitMessage(commitMessage)
                          .withChangeFromAnotherCommit(Boolean.TRUE)
                          .build()),
        Status.SUCCESS, true, false, accountId, "", "");
    final GitFileActivity fileActivity = wingsPersistence.createQuery(GitFileActivity.class)
                                             .filter(GitFileActivityKeys.accountId, accountId)
                                             .filter(GitFileActivityKeys.commitId, commitId)
                                             .get();
    assertThat(fileActivity).isNotNull();
    assertThat(fileActivity.getCommitId()).isEqualTo(commitId);
    assertThat(fileActivity.getStatus()).isEqualTo(Status.SUCCESS);
    assertThat(fileActivity.getFilePath()).isEqualTo(filePath);
    assertThat(fileActivity.getCommitId()).isEqualTo(commitId);
    assertThat(fileActivity.getCommitMessage()).isEqualTo(commitMessage);
    assertThat(fileActivity.getProcessingCommitId()).isEqualTo(processingCommitId);
    assertThat(fileActivity.getProcessingCommitMessage()).isEqualTo(processingCommitMessage);
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
  @Owner(developers = DEEPAK)
  @Category(UnitTests.class)
  public void test_shouldCreateGitFileActivitySummary() {
    String commitId = "commitId";
    String commitMessage = "commitMessage";
    String filePath1 = "filePath1";
    String filePath2 = "filePath2";
    String appId1 = "appId1";
    String appId2 = "appId2";
    String branchName = "branchName";
    wingsPersistence.save(GitFileActivity.builder()
                              .accountId(accountId)
                              .commitId(commitId)
                              .processingCommitId(commitId)
                              .filePath(filePath1)
                              .gitConnectorId(gitConnectorId)
                              .branchName(branchName)
                              .appId(appId1)
                              .commitMessage(commitMessage)
                              .status(Status.SUCCESS)
                              .build());
    wingsPersistence.save(GitFileActivity.builder()
                              .accountId(accountId)
                              .commitId(commitId)
                              .processingCommitId(commitId)
                              .gitConnectorId(gitConnectorId)
                              .branchName(branchName)
                              .commitMessage(commitMessage)
                              .filePath(filePath2)
                              .appId(appId2)
                              .status(Status.SUCCESS)
                              .build());
    gitSyncService.createGitFileActivitySummaryForCommit(commitId, accountId, true, COMPLETED);
    List<GitFileActivitySummary> gitFileActivitySummaries =
        wingsPersistence.createQuery(GitFileActivitySummary.class).filter("commitId", commitId).asList();
    assertThat(gitFileActivitySummaries.size()).isEqualTo(2);
    GitFileActivitySummary fileActivitySummaryForApp1 =
        gitFileActivitySummaries.stream().filter(activity -> activity.getAppId().equals(appId1)).findFirst().get();
    GitFileActivitySummary fileActivitySummaryForApp2 =
        gitFileActivitySummaries.stream().filter(activity -> activity.getAppId().equals(appId2)).findFirst().get();

    assertThat(fileActivitySummaryForApp1.getAccountId()).isEqualTo(accountId);
    assertThat(fileActivitySummaryForApp1.getCommitId()).isEqualTo(commitId);
    assertThat(fileActivitySummaryForApp1.getGitConnectorId()).isEqualTo(gitConnectorId);
    assertThat(fileActivitySummaryForApp1.getBranchName()).isEqualTo(branchName);
    assertThat(fileActivitySummaryForApp1.getCommitMessage()).isEqualTo(commitMessage);
    assertThat(fileActivitySummaryForApp1.getAppId()).isEqualTo(appId1);
    assertThat(fileActivitySummaryForApp1.getStatus()).isEqualTo(COMPLETED);
    assertThat(fileActivitySummaryForApp1.getGitToHarness()).isEqualTo(true);
    assertThat(fileActivitySummaryForApp1.getFileProcessingSummary().getSuccessCount()).isEqualTo(1);
    assertThat(fileActivitySummaryForApp1.getFileProcessingSummary().getTotalCount()).isEqualTo(1);

    assertThat(fileActivitySummaryForApp2.getAccountId()).isEqualTo(accountId);
    assertThat(fileActivitySummaryForApp2.getCommitId()).isEqualTo(commitId);
    assertThat(fileActivitySummaryForApp2.getGitConnectorId()).isEqualTo(gitConnectorId);
    assertThat(fileActivitySummaryForApp2.getBranchName()).isEqualTo(branchName);
    assertThat(fileActivitySummaryForApp2.getCommitMessage()).isEqualTo(commitMessage);
    assertThat(fileActivitySummaryForApp2.getAppId()).isEqualTo(appId2);
    assertThat(fileActivitySummaryForApp2.getStatus()).isEqualTo(COMPLETED);
    assertThat(fileActivitySummaryForApp2.getGitToHarness()).isEqualTo(true);
    assertThat(fileActivitySummaryForApp2.getFileProcessingSummary().getSuccessCount()).isEqualTo(1);
    assertThat(fileActivitySummaryForApp2.getFileProcessingSummary().getTotalCount()).isEqualTo(1);
  }

  @Test
  @Owner(developers = DEEPAK)
  @Category(UnitTests.class)
  public void test_createGitFileSummaryForFailedOrSkippedCommit() {
    String appId = "appId";
    String commitId = "commitIdTest";
    String commitMessage = "commitMessageTest";
    String branchName = "branchName";
    GitCommit gitCommit = GitCommit.builder()
                              .commitId(commitId)
                              .gitConnectorId(gitConnectorId)
                              .accountId(accountId)
                              .branchName("branchName")
                              .status(FAILED)
                              .yamlGitConfigIds(Arrays.asList("yamlGitConfigId"))
                              .commitMessage(commitMessage)
                              .build();
    when(yamlGitConfigService.getAppIdsForYamlGitConfig(any())).thenReturn(Collections.singleton(appId));
    gitSyncService.createGitFileSummaryForFailedOrSkippedCommit(gitCommit, true);
    GitFileActivitySummary gitFileActivitySummary =
        wingsPersistence.createQuery(GitFileActivitySummary.class).filter("commitId", commitId).get();
    assertThat(gitFileActivitySummary.getAccountId()).isEqualTo(accountId);
    assertThat(gitFileActivitySummary.getCommitId()).isEqualTo(commitId);
    assertThat(gitFileActivitySummary.getGitConnectorId()).isEqualTo(gitConnectorId);
    assertThat(gitFileActivitySummary.getBranchName()).isEqualTo(branchName);
    assertThat(gitFileActivitySummary.getCommitMessage()).isEqualTo(commitMessage);
    assertThat(gitFileActivitySummary.getAppId()).isEqualTo(appId);
    assertThat(gitFileActivitySummary.getStatus()).isEqualTo(FAILED);
    assertThat(gitFileActivitySummary.getFileProcessingSummary()).isNull();
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

  @Test
  @Owner(developers = DEEPAK)
  @Category(UnitTests.class)
  public void test_logActivitiesForFailedChanges() {
    String errorMsg1 = "errorMessage1";
    String errorMsg2 = "errorMessage2";
    String commitId = "commitId";
    String filePath1 = "filePath1";
    String filePath2 = "filePath2";
    String commitMessage = "commitMessage";
    Map<String, YamlProcessingException.ChangeWithErrorMsg> failedYamlFileChangeMap = new HashMap<>();
    YamlGitConfig yamlGitConfig =
        YamlGitConfig.builder().gitConnectorId(gitConnectorId).branchName(branchName1).build();
    GitFileChange change1 = GitFileChange.Builder.aGitFileChange()
                                .withFilePath(filePath1)
                                .withAccountId(accountId)
                                .withCommitId(commitId)
                                .withProcessingCommitId(commitId)
                                .withCommitMessage(commitMessage)
                                .withYamlGitConfig(yamlGitConfig)
                                .withChangeFromAnotherCommit(false)
                                .withSyncFromGit(true)
                                .build();
    failedYamlFileChangeMap.put(change1.getFilePath(),
        YamlProcessingException.ChangeWithErrorMsg.builder().errorMsg(errorMsg1).change(change1).build());
    GitFileChange change2 = GitFileChange.Builder.aGitFileChange()
                                .withFilePath(filePath2)
                                .withAccountId(accountId)
                                .withCommitId(commitId)
                                .withProcessingCommitId(commitId)
                                .withCommitMessage(commitMessage)
                                .withYamlGitConfig(yamlGitConfig)
                                .withChangeFromAnotherCommit(false)
                                .withSyncFromGit(true)
                                .build();
    failedYamlFileChangeMap.put(change2.getFilePath(),
        YamlProcessingException.ChangeWithErrorMsg.builder().errorMsg(errorMsg2).change(change2).build());
    gitSyncService.logActivityForGitOperation(
        Arrays.asList(change1, change2), Status.QUEUED, true, false, "", commitId, "");
    gitSyncService.logActivitiesForFailedChanges(failedYamlFileChangeMap, accountId, false, commitMessage);
    List<GitFileActivity> gitFileActivities = wingsPersistence.createQuery(GitFileActivity.class)
                                                  .filter(ACCOUNT_ID_KEY, accountId)
                                                  .filter("commitId", commitId)
                                                  .asList();
    assertThat(gitFileActivities.size()).isEqualTo(2);
    GitFileActivity fileActivity1 =
        gitFileActivities.stream().filter(activity -> activity.getFilePath().equals(filePath1)).findFirst().get();
    GitFileActivity fileActivity2 =
        gitFileActivities.stream().filter(activity -> activity.getFilePath().equals(filePath2)).findFirst().get();

    assertThat(fileActivity1.getAccountId()).isEqualTo(accountId);
    assertThat(fileActivity1.getCommitId()).isEqualTo(commitId);
    assertThat(fileActivity1.getGitConnectorId()).isEqualTo(gitConnectorId);
    assertThat(fileActivity1.getBranchName()).isEqualTo(branchName1);
    assertThat(fileActivity1.getCommitMessage()).isEqualTo(commitMessage);
    assertThat(fileActivity1.getErrorMessage()).isEqualTo(errorMsg1);

    assertThat(fileActivity2.getAccountId()).isEqualTo(accountId);
    assertThat(fileActivity2.getCommitId()).isEqualTo(commitId);
    assertThat(fileActivity2.getGitConnectorId()).isEqualTo(gitConnectorId);
    assertThat(fileActivity2.getBranchName()).isEqualTo(branchName1);
    assertThat(fileActivity2.getCommitMessage()).isEqualTo(commitMessage);
    assertThat(fileActivity2.getErrorMessage()).isEqualTo(errorMsg2);
  }
}
