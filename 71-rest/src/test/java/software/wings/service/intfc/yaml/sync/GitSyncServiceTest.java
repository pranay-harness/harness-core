package software.wings.service.intfc.yaml.sync;

import static io.harness.rule.OwnerRule.ABHINAV;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.beans.Account.GLOBAL_ACCOUNT_ID;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.WingsBaseTest;
import software.wings.beans.Application;
import software.wings.beans.EntityType;
import software.wings.beans.SettingAttribute;
import software.wings.service.impl.yaml.gitsync.ChangeSetDTO;
import software.wings.yaml.gitSync.GitSyncMetadata;
import software.wings.yaml.gitSync.YamlChangeSet;
import software.wings.yaml.gitSync.YamlGitConfig;

import java.util.List;

public class GitSyncServiceTest extends WingsBaseTest {
  @Inject GitSyncService gitSyncService;

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void shouldFetchYamlChangeSetBeingProcessed() {
    final String gitConnectorName = "connectorName";
    final String gitConnectorid = "connectorid";
    final String branchName = "branchName";
    final String appId = "appId";
    String yamlGitConfigId = setupConnectorAndYamlGitConfig(gitConnectorName, branchName, gitConnectorid, appId);

    saveYamlChangeSetCombinations(gitConnectorid, yamlGitConfigId, branchName, appId);
    saveYamlChangeSetCombinations(gitConnectorid, "random", branchName, "random");
    saveYamlChangeSetCombinations(gitConnectorid, "random", "random", "random");
    saveYamlChangeSetCombinations("random", "random", "random", "random");

    List<ChangeSetDTO> changeSetDTOList = gitSyncService.getCommitsWhichAreBeingProcessed(GLOBAL_ACCOUNT_ID, appId, 10);

    assertThat(changeSetDTOList).isNotNull();
    assertThat(changeSetDTOList.size()).isEqualTo(6);
  }

  private void saveYamlChangeSetCombinations(
      String gitConnectorid, String yamlGitConfigId, String branchName, String appId) {
    saveYamlChangeSet(YamlChangeSet.Status.QUEUED, gitConnectorid, yamlGitConfigId, branchName, true, appId);
    saveYamlChangeSet(YamlChangeSet.Status.RUNNING, gitConnectorid, yamlGitConfigId, branchName, true, appId);
    saveYamlChangeSet(YamlChangeSet.Status.QUEUED, gitConnectorid, yamlGitConfigId, branchName, false, appId);
    saveYamlChangeSet(YamlChangeSet.Status.RUNNING, gitConnectorid, yamlGitConfigId, branchName, false, appId);
  }

  private String setupConnectorAndYamlGitConfig(
      String gitConnectorName, String branchName, String gitConnectorid, String appId) {
    wingsPersistence.save(SettingAttribute.Builder.aSettingAttribute()
                              .withName(gitConnectorName)
                              .withUuid(gitConnectorid)
                              .withAccountId(GLOBAL_ACCOUNT_ID)
                              .build());
    wingsPersistence.save(
        Application.Builder.anApplication().uuid(appId).appId(appId).accountId(GLOBAL_ACCOUNT_ID).build());
    YamlGitConfig yamlGitConfig = YamlGitConfig.builder()
                                      .gitConnectorId(gitConnectorid)
                                      .branchName(branchName)
                                      .accountId(GLOBAL_ACCOUNT_ID)
                                      .entityType(EntityType.APPLICATION)
                                      .entityId(appId)
                                      .build();
    return wingsPersistence.save(yamlGitConfig);
  }

  private void saveYamlChangeSet(YamlChangeSet.Status status, String connectorId, String yamlGitConfigId,
      String branchName, boolean gitToHarness, String appId) {
    wingsPersistence.save(buildYamlChangeSet(status, connectorId, yamlGitConfigId, branchName, gitToHarness, appId));
  }

  private YamlChangeSet buildYamlChangeSet(YamlChangeSet.Status status, String connectorId, String yamlGitConfigId,
      String branchName, boolean gitToHarness, String appId) {
    GitSyncMetadata gitSyncMetadata = GitSyncMetadata.builder()
                                          .gitConnectorId(connectorId)
                                          .yamlGitConfigId(yamlGitConfigId)
                                          .branchName(branchName)
                                          .build();
    return YamlChangeSet.builder()
        .accountId(GLOBAL_ACCOUNT_ID)
        .status(status)
        .gitSyncMetadata(gitSyncMetadata)
        .gitToHarness(gitToHarness)
        .appId(appId)
        .build();
  }
}