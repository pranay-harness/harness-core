package software.wings.service.impl.yaml;

import static io.harness.rule.OwnerRule.DEEPAK;
import static io.harness.rule.OwnerRule.ROHIT_KUMAR;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.beans.Application.GLOBAL_APP_ID;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.utils.WingsTestConstants.SETTING_ID;
import static software.wings.yaml.gitSync.YamlChangeSet.MAX_RETRY_COUNT_EXCEEDED_CODE;
import static software.wings.yaml.gitSync.YamlChangeSet.Status.QUEUED;
import static software.wings.yaml.gitSync.YamlChangeSet.Status.SKIPPED;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.MockitoAnnotations;
import software.wings.WingsBaseTest;
import software.wings.beans.GitConfig;
import software.wings.beans.SettingAttribute;
import software.wings.yaml.gitSync.GitSyncMetadata;
import software.wings.yaml.gitSync.GitWebhookRequestAttributes;
import software.wings.yaml.gitSync.YamlChangeSet;
import software.wings.yaml.gitSync.YamlGitConfig;

import java.util.Collections;

public class YamlChangeSetServiceImplTest extends WingsBaseTest {
  @Inject private YamlChangeSetServiceImpl yamlChangeSetService;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = ROHIT_KUMAR)
  @Category(UnitTests.class)
  public void test_getQueuedChangeSetForWaitingQueueKey() throws Exception {
    final String accountid = "accountid";
    final YamlChangeSet fullSyncChangeSet =
        yamlChangeSetService.save(YamlChangeSet.builder()
                                      .fullSync(true)
                                      .status(QUEUED)
                                      .accountId(accountid)
                                      .gitToHarness(false)
                                      .appId(GLOBAL_APP_ID)
                                      .gitFileChanges(Collections.emptyList())
                                      .queueKey("queuekey")
                                      .gitSyncMetadata(GitSyncMetadata.builder().build())
                                      .build());

    final YamlChangeSet fullSyncChangeSet1 =
        yamlChangeSetService.save(YamlChangeSet.builder()
                                      .fullSync(true)
                                      .status(QUEUED)
                                      .accountId(accountid)
                                      .gitToHarness(false)
                                      .appId(GLOBAL_APP_ID)
                                      .gitFileChanges(Collections.emptyList())
                                      .queueKey("queuekey1")
                                      .gitSyncMetadata(GitSyncMetadata.builder().build())
                                      .build());

    final YamlChangeSet harnessToGitChangeSet =
        yamlChangeSetService.save(YamlChangeSet.builder()
                                      .fullSync(false)
                                      .status(QUEUED)
                                      .accountId(accountid)
                                      .gitToHarness(false)
                                      .appId(GLOBAL_APP_ID)
                                      .gitFileChanges(Collections.emptyList())
                                      .queueKey("queuekey")
                                      .gitSyncMetadata(GitSyncMetadata.builder().build())
                                      .build());

    final YamlChangeSet harnessToGitChangeSet1 =
        yamlChangeSetService.save(YamlChangeSet.builder()
                                      .fullSync(false)
                                      .status(QUEUED)
                                      .accountId(accountid)
                                      .gitToHarness(false)
                                      .appId(GLOBAL_APP_ID)
                                      .gitFileChanges(Collections.emptyList())
                                      .queueKey("queuekey1")
                                      .gitSyncMetadata(GitSyncMetadata.builder().build())
                                      .build());

    final YamlChangeSet gitToHarnessChangeSet =
        yamlChangeSetService.save(YamlChangeSet.builder()
                                      .fullSync(false)
                                      .status(QUEUED)
                                      .accountId(accountid)
                                      .gitToHarness(true)
                                      .appId(GLOBAL_APP_ID)
                                      .gitFileChanges(Collections.emptyList())
                                      .queueKey("queuekey")
                                      .gitSyncMetadata(GitSyncMetadata.builder().build())
                                      .build());

    final YamlChangeSet gitToHarnessChangeSet1 =
        yamlChangeSetService.save(YamlChangeSet.builder()
                                      .fullSync(false)
                                      .status(QUEUED)
                                      .accountId(accountid)
                                      .gitToHarness(true)
                                      .appId(GLOBAL_APP_ID)
                                      .gitFileChanges(Collections.emptyList())
                                      .queueKey("queuekey1")
                                      .gitSyncMetadata(GitSyncMetadata.builder().build())
                                      .build());
    {
      final YamlChangeSet selectedChangeSet =
          yamlChangeSetService.getQueuedChangeSetForWaitingQueueKey(accountid, "queuekey", 2);
      assertThat(selectedChangeSet.getUuid()).isEqualTo(fullSyncChangeSet.getUuid());
    }
    {
      final YamlChangeSet selectedChangeSet =
          yamlChangeSetService.getQueuedChangeSetForWaitingQueueKey(accountid, "queuekey1", 2);
      assertThat(selectedChangeSet.getUuid()).isEqualTo(fullSyncChangeSet1.getUuid());
    }
    {
      final YamlChangeSet selectedChangeSet =
          yamlChangeSetService.getQueuedChangeSetForWaitingQueueKey(accountid, "queuekey", 2);
      assertThat(selectedChangeSet).isNull();
      yamlChangeSetService.updateStatus(accountid, fullSyncChangeSet.getUuid(), YamlChangeSet.Status.COMPLETED);
    }

    {
      final YamlChangeSet selectedChangeSet =
          yamlChangeSetService.getQueuedChangeSetForWaitingQueueKey(accountid, "queuekey1", 2);
      assertThat(selectedChangeSet).isNull();
      yamlChangeSetService.updateStatus(accountid, fullSyncChangeSet1.getUuid(), YamlChangeSet.Status.COMPLETED);
    }
    {
      final YamlChangeSet selectedChangeSet =
          yamlChangeSetService.getQueuedChangeSetForWaitingQueueKey(accountid, "queuekey", 2);
      assertThat(selectedChangeSet.getUuid()).isEqualTo(gitToHarnessChangeSet.getUuid());
      yamlChangeSetService.updateStatus(accountid, gitToHarnessChangeSet.getUuid(), YamlChangeSet.Status.COMPLETED);
    }

    {
      final YamlChangeSet selectedChangeSet =
          yamlChangeSetService.getQueuedChangeSetForWaitingQueueKey(accountid, "queuekey1", 2);
      assertThat(selectedChangeSet.getUuid()).isEqualTo(gitToHarnessChangeSet1.getUuid());
      yamlChangeSetService.updateStatus(accountid, gitToHarnessChangeSet1.getUuid(), YamlChangeSet.Status.COMPLETED);
    }

    {
      final YamlChangeSet selectedChangeSet =
          yamlChangeSetService.getQueuedChangeSetForWaitingQueueKey(accountid, "queuekey", 2);
      assertThat(selectedChangeSet.getUuid()).isEqualTo(harnessToGitChangeSet.getUuid());
      yamlChangeSetService.updateStatus(accountid, harnessToGitChangeSet.getUuid(), YamlChangeSet.Status.COMPLETED);
    }
    {
      final YamlChangeSet selectedChangeSet =
          yamlChangeSetService.getQueuedChangeSetForWaitingQueueKey(accountid, "queuekey1", 2);
      assertThat(selectedChangeSet.getUuid()).isEqualTo(harnessToGitChangeSet1.getUuid());
      yamlChangeSetService.updateStatus(accountid, harnessToGitChangeSet1.getUuid(), YamlChangeSet.Status.COMPLETED);
    }
  }

  @Test
  @Owner(developers = DEEPAK)
  @Category(UnitTests.class)
  public void test_markQueuedYamlChangeSetsWithMaxRetriesAsSkipped() {
    // Testing whether the status is updated when retry count is more than MAX
    String accountId = "accountId";
    SettingAttribute settingAttribute =
        aSettingAttribute()
            .withAccountId(accountId)
            .withUuid(SETTING_ID)
            .withName("gitconnectorid")
            .withValue(GitConfig.builder().accountId(accountId).webhookToken("Webhook_Token").build())
            .build();

    YamlGitConfig yamlGitConfig =
        YamlGitConfig.builder().accountId(accountId).gitConnectorId(SETTING_ID).branchName("master").build();
    wingsPersistence.save(settingAttribute);
    wingsPersistence.save(yamlGitConfig);

    final YamlChangeSet changeSetToBeSkipped = yamlChangeSetService.save(
        YamlChangeSet.builder()
            .fullSync(false)
            .status(QUEUED)
            .accountId(accountId)
            .gitToHarness(true)
            .retryCount(4)
            .appId(GLOBAL_APP_ID)
            .gitFileChanges(Collections.emptyList())
            .gitWebhookRequestAttributes(
                GitWebhookRequestAttributes.builder().branchName("master").gitConnectorId(SETTING_ID).build())
            .build());
    yamlChangeSetService.markQueuedYamlChangeSetsWithMaxRetriesAsSkipped(accountId);
    YamlChangeSet updatedYamlChangeSet = yamlChangeSetService.get(accountId, changeSetToBeSkipped.getUuid());
    assertThat(updatedYamlChangeSet.getStatus()).isEqualTo(SKIPPED);
    assertThat(updatedYamlChangeSet.getMessageCode()).isEqualTo(MAX_RETRY_COUNT_EXCEEDED_CODE);

    // Testing whether the status is not updated when retry count is not more than MAX
    final YamlChangeSet changeSetNotToBeSkipped = yamlChangeSetService.save(
        YamlChangeSet.builder()
            .fullSync(false)
            .status(QUEUED)
            .accountId(accountId)
            .gitToHarness(true)
            .retryCount(3)
            .appId(GLOBAL_APP_ID)
            .gitWebhookRequestAttributes(
                GitWebhookRequestAttributes.builder().branchName("master").gitConnectorId(SETTING_ID).build())
            .gitFileChanges(Collections.emptyList())
            .build());
    yamlChangeSetService.markQueuedYamlChangeSetsWithMaxRetriesAsSkipped(accountId);
    YamlChangeSet updatedChangeSetWithLessRetries =
        yamlChangeSetService.get(accountId, changeSetNotToBeSkipped.getUuid());
    assertThat(updatedChangeSetWithLessRetries.getStatus()).isEqualTo(QUEUED);
    assertThat(updatedChangeSetWithLessRetries.getMessageCode()).isEqualTo(null);
  }
}