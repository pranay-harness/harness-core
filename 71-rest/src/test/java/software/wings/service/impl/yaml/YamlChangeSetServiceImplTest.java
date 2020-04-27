package software.wings.service.impl.yaml;

import static io.harness.rule.OwnerRule.DEEPAK;
import static io.harness.rule.OwnerRule.ROHIT_KUMAR;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.beans.Application.GLOBAL_APP_ID;
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
import software.wings.yaml.gitSync.YamlChangeSet;

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
  public void test_getQueuedChangeSetForWaitingAccount() throws Exception {
    final String accountid = "accountid";
    final YamlChangeSet fullSyncChangeSet = yamlChangeSetService.save(YamlChangeSet.builder()
                                                                          .fullSync(true)
                                                                          .status(QUEUED)
                                                                          .accountId(accountid)
                                                                          .gitToHarness(false)
                                                                          .appId(GLOBAL_APP_ID)
                                                                          .gitFileChanges(Collections.emptyList())
                                                                          .build());

    final YamlChangeSet harnessToGitChangeSet = yamlChangeSetService.save(YamlChangeSet.builder()
                                                                              .fullSync(false)
                                                                              .status(QUEUED)
                                                                              .accountId(accountid)
                                                                              .gitToHarness(false)
                                                                              .appId(GLOBAL_APP_ID)
                                                                              .gitFileChanges(Collections.emptyList())
                                                                              .build());

    final YamlChangeSet gitToHarnessChangeSet = yamlChangeSetService.save(YamlChangeSet.builder()
                                                                              .fullSync(false)
                                                                              .status(QUEUED)
                                                                              .accountId(accountid)
                                                                              .gitToHarness(true)
                                                                              .appId(GLOBAL_APP_ID)
                                                                              .gitFileChanges(Collections.emptyList())
                                                                              .build());
    {
      final YamlChangeSet selectedChangeSet = yamlChangeSetService.getQueuedChangeSetForWaitingAccount(accountid);
      assertThat(selectedChangeSet.getUuid()).isEqualTo(fullSyncChangeSet.getUuid());
    }
    {
      final YamlChangeSet selectedChangeSet = yamlChangeSetService.getQueuedChangeSetForWaitingAccount(accountid);
      assertThat(selectedChangeSet).isNull();
      yamlChangeSetService.updateStatus(accountid, fullSyncChangeSet.getUuid(), YamlChangeSet.Status.COMPLETED);
    }
    {
      final YamlChangeSet selectedChangeSet = yamlChangeSetService.getQueuedChangeSetForWaitingAccount(accountid);
      assertThat(selectedChangeSet.getUuid()).isEqualTo(gitToHarnessChangeSet.getUuid());
      yamlChangeSetService.updateStatus(accountid, gitToHarnessChangeSet.getUuid(), YamlChangeSet.Status.COMPLETED);
    }

    {
      final YamlChangeSet selectedChangeSet = yamlChangeSetService.getQueuedChangeSetForWaitingAccount(accountid);
      assertThat(selectedChangeSet.getUuid()).isEqualTo(harnessToGitChangeSet.getUuid());
      yamlChangeSetService.updateStatus(accountid, harnessToGitChangeSet.getUuid(), YamlChangeSet.Status.COMPLETED);
    }
  }

  @Test
  @Owner(developers = DEEPAK)
  @Category(UnitTests.class)
  public void test_markQueuedYamlChangeSetsWithMaxRetriesAsSkipped() {
    // Testing whether the status is updated when retry count is more than MAX
    String accountId = "accountId";
    final YamlChangeSet changeSetToBeSkipped = yamlChangeSetService.save(YamlChangeSet.builder()
                                                                             .fullSync(false)
                                                                             .status(QUEUED)
                                                                             .accountId(accountId)
                                                                             .gitToHarness(true)
                                                                             .retryCount(4)
                                                                             .appId(GLOBAL_APP_ID)
                                                                             .gitFileChanges(Collections.emptyList())
                                                                             .build());
    yamlChangeSetService.markQueuedYamlChangeSetsWithMaxRetriesAsSkipped(accountId);
    YamlChangeSet updatedYamlChangeSet = yamlChangeSetService.get(accountId, changeSetToBeSkipped.getUuid());
    assertThat(updatedYamlChangeSet.getStatus()).isEqualTo(SKIPPED);
    assertThat(updatedYamlChangeSet.getMessageCode()).isEqualTo(MAX_RETRY_COUNT_EXCEEDED_CODE);

    // Testing whether the status is not updated when retry count is not more than MAX
    final YamlChangeSet changeSetNotToBeSkipped = yamlChangeSetService.save(YamlChangeSet.builder()
                                                                                .fullSync(false)
                                                                                .status(QUEUED)
                                                                                .accountId(accountId)
                                                                                .gitToHarness(true)
                                                                                .retryCount(3)
                                                                                .appId(GLOBAL_APP_ID)
                                                                                .gitFileChanges(Collections.emptyList())
                                                                                .build());
    yamlChangeSetService.markQueuedYamlChangeSetsWithMaxRetriesAsSkipped(accountId);
    YamlChangeSet updatedChangeSetWithLessRetries =
        yamlChangeSetService.get(accountId, changeSetNotToBeSkipped.getUuid());
    assertThat(updatedChangeSetWithLessRetries.getStatus()).isEqualTo(QUEUED);
    assertThat(updatedChangeSetWithLessRetries.getMessageCode()).isEqualTo(null);
  }
}