package io.harness.gitsync.core.runnable;

import static io.harness.gitsync.common.beans.YamlChangeSet.Status.QUEUED;
import static io.harness.rule.OwnerRule.ABHINAV;
import static io.harness.rule.OwnerRule.ADWAIT;

import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.SETTING_ID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.git.YamlGitConfigDTO;
import io.harness.git.model.GitFileChange;
import io.harness.gitsync.GitSyncBaseTest;
import io.harness.gitsync.common.beans.YamlChangeSet;
import io.harness.gitsync.common.service.YamlGitConfigService;
import io.harness.gitsync.core.beans.GitSyncMetadata;
import io.harness.gitsync.core.impl.YamlChangeSetServiceImpl;
import io.harness.gitsync.core.service.YamlGitService;
import io.harness.rule.Owner;

import software.wings.utils.WingsTestConstants;

import com.google.inject.Inject;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

public class GitChangeSetRunnableTest extends GitSyncBaseTest {
  @Inject YamlGitConfigService yamlGitConfigService;
  @Inject @Spy GitChangeSetRunnableHelper gitChangeSetRunnableHelper;
  @Inject @Spy YamlChangeSetServiceImpl yamlChangeSetService;
  @Mock private YamlGitService yamlGitSyncService;

  @InjectMocks @Inject @Spy private GitChangeSetRunnable gitChangeSetRunnable;

  @Inject

  public void setUp() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void test_run() {
    doReturn(2).when(gitChangeSetRunnable).getMaxRunningChangesetsForAccount();
    final String webhookToken = "Webhook_Token";

    YamlGitConfigDTO yamlGitConfig = YamlGitConfigDTO.builder()
                                         .accountId(ACCOUNT_ID)
                                         .gitConnectorId(SETTING_ID)
                                         .branch("master")
                                         .rootFolders(Arrays.asList(YamlGitConfigDTO.RootFolder.builder().build()))
                                         .build();

    yamlGitConfigService.save(yamlGitConfig);
    GitFileChange gitFileChange = GitFileChange.builder().build();

    yamlChangeSetService.save(YamlChangeSet.builder()
                                  .fullSync(true)
                                  .status(QUEUED)
                                  .accountId(ACCOUNT_ID)
                                  .gitToHarness(false)
                                  .gitFileChanges(Arrays.asList(gitFileChange))
                                  .queueKey("queuekey1")
                                  .gitSyncMetadata(GitSyncMetadata.builder().build())
                                  .build());

    yamlChangeSetService.save(YamlChangeSet.builder()
                                  .fullSync(true)
                                  .status(QUEUED)
                                  .accountId(ACCOUNT_ID)
                                  .gitToHarness(false)
                                  .gitFileChanges(Arrays.asList(gitFileChange))
                                  .queueKey("queuekey1")
                                  .gitSyncMetadata(GitSyncMetadata.builder().build())
                                  .build());

    doNothing().when(yamlGitSyncService).handleHarnessChangeSet(any(YamlChangeSet.class), eq(ACCOUNT_ID));

    gitChangeSetRunnable.run();

    verify(yamlGitSyncService, times(1)).handleHarnessChangeSet(any(YamlChangeSet.class), eq(ACCOUNT_ID));
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testRetryAnyStuckYamlChangeSet() {
    YamlChangeSet yamlChangeSet = YamlChangeSet.builder().accountId(WingsTestConstants.ACCOUNT_ID).build();
    yamlChangeSet.setUuid("12345");

    doReturn(Arrays.asList(yamlChangeSet)).when(gitChangeSetRunnableHelper).getStuckYamlChangeSets(any(), anyList());
    doReturn(true)
        .when(yamlChangeSetService)
        .updateStatusAndIncrementRetryCountForYamlChangeSets(anyString(), any(), anyList(), anyList());

    gitChangeSetRunnable.retryAnyStuckYamlChangeSet(Arrays.asList("12345"));
    ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
    verify(yamlChangeSetService)
        .updateStatusAndIncrementRetryCountForYamlChangeSets(anyString(), any(), anyList(), captor.capture());
    List stuckChangeSetIds = captor.getValue();
    assertThat(stuckChangeSetIds).isNotNull();
    assertThat(stuckChangeSetIds).hasSize(1);
    assertThat(stuckChangeSetIds.get(0)).isEqualTo("12345");
  }
}
