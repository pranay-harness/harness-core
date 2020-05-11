package software.wings.service.impl.yaml;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.VARDAN_BANSAL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

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
import software.wings.beans.GitCommit.GitCommitKeys;
import software.wings.beans.GitConfig;
import software.wings.beans.GitDetail;
import software.wings.beans.SettingAttribute;
import software.wings.beans.yaml.Change.ChangeType;
import software.wings.beans.yaml.GitDiffResult;
import software.wings.beans.yaml.GitFileChange;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.yaml.sync.GitSyncErrorService;
import software.wings.service.intfc.yaml.sync.YamlGitConfigService;
import software.wings.yaml.errorhandling.GitSyncError;
import software.wings.yaml.errorhandling.GitToHarnessErrorDetails;
import software.wings.yaml.gitSync.GitFileActivity;
import software.wings.yaml.gitSync.GitFileActivity.GitFileActivityKeys;
import software.wings.yaml.gitSync.GitFileActivity.Status;
import software.wings.yaml.gitSync.GitFileProcessingSummary;
import software.wings.yaml.gitSync.YamlChangeSet;
import software.wings.yaml.gitSync.YamlGitConfig;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GitSyncServiceImplTest extends WingsBaseTest {
  @InjectMocks @Inject private GitSyncServiceImpl gitSyncService;
  @Inject private WingsPersistence wingsPersistence;
  @InjectMocks @Inject private GitSyncErrorService gitSyncErrorService;
  @Mock YamlGitConfigService yamlGitConfigService;
  private String accountId = generateUuid();
  private String uuid = generateUuid();
  private String gitConnectorName = "gitConnectorName";
  private String repoURL = "https://abc.com";
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
  @Owner(developers = VARDAN_BANSAL)
  @Category(UnitTests.class)
  public void test_fetchRepositories() {
    String branchName1 = "branchName1";
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
            .gitConnectorId(gitConnectorId)
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
                                             .gitConnectorId(gitConnectorId)
                                             .status(GitFileActivity.Status.SUCCESS)
                                             .build();

    wingsPersistence.save(fileActivity);
    final PageResponse pageResponse =
        gitSyncService.fetchGitSyncActivity(aPageRequest().withLimit("1").build(), accountId);
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
    gitSyncService.logActivityForGitOperation(
        Arrays.asList(GitFileChange.Builder.aGitFileChange()
                          .withYamlGitConfig(
                              YamlGitConfig.builder().branchName("branchName").gitConnectorId("gitConnectorId").build())
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
