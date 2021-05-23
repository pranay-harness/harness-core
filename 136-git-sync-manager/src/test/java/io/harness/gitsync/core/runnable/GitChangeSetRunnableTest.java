package io.harness.gitsync.core.runnable;

import static io.harness.annotations.dev.HarnessTeam.DX;
import static io.harness.rule.OwnerRule.ABHINAV;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.IdentifierRef;
import io.harness.category.element.UnitTests;
import io.harness.gitsync.GitSyncTestBase;
import io.harness.gitsync.common.beans.YamlChangeSet;
import io.harness.gitsync.common.beans.YamlChangeSet.YamlChangeSetBuilder;
import io.harness.gitsync.common.beans.YamlChangeSetEventType;
import io.harness.gitsync.core.service.YamlChangeSetService;
import io.harness.rule.Owner;

import com.google.inject.Inject;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

@OwnedBy(DX)
public class GitChangeSetRunnableTest extends GitSyncTestBase {
  @Inject @Spy GitChangeSetRunnable gitChangeSetRunnable;
  @Inject YamlChangeSetService yamlChangeSetService;

  final String accountId = "accountId";
  final String branch = "branch";
  final String repo = "repo";
  final IdentifierRef connectorRef = IdentifierRef.builder().accountIdentifier(accountId).build();

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testRun() {
    final YamlChangeSetBuilder yamlChangeSetBuilder =
        YamlChangeSet.builder().accountId(accountId).connectorRef(connectorRef).branch(branch).repoUrl(repo);
    yamlChangeSetService.save(yamlChangeSetBuilder.eventType(YamlChangeSetEventType.BRANCH_CREATE.name()).build());
    yamlChangeSetService.save(yamlChangeSetBuilder.eventType(YamlChangeSetEventType.BRANCH_SYNC.name()).build());
    yamlChangeSetService.save(
        yamlChangeSetBuilder.eventType(YamlChangeSetEventType.GIT_TO_HARNESS_PUSH.name()).build());
    ArgumentCaptor<YamlChangeSet> argumentCaptor = ArgumentCaptor.forClass(YamlChangeSet.class);
    gitChangeSetRunnable.run();
    verify(gitChangeSetRunnable).processChangeSet(argumentCaptor.capture());
    assertThat(argumentCaptor.getAllValues().size()).isEqualTo(1);
    assertThat(argumentCaptor.getAllValues().get(0).getEventType())
        .isEqualTo(YamlChangeSetEventType.BRANCH_CREATE.name());
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testRun_1() {
    final YamlChangeSetBuilder yamlChangeSetBuilder =
        YamlChangeSet.builder().accountId(accountId).connectorRef(connectorRef).branch(branch).repoUrl(repo);
    yamlChangeSetService.save(yamlChangeSetBuilder.eventType(YamlChangeSetEventType.BRANCH_SYNC.name()).build());
    yamlChangeSetService.save(
        yamlChangeSetBuilder.eventType(YamlChangeSetEventType.GIT_TO_HARNESS_PUSH.name()).build());
    ArgumentCaptor<YamlChangeSet> argumentCaptor = ArgumentCaptor.forClass(YamlChangeSet.class);
    gitChangeSetRunnable.run();
    verify(gitChangeSetRunnable).processChangeSet(argumentCaptor.capture());
    assertThat(argumentCaptor.getAllValues().size()).isEqualTo(1);
    assertThat(argumentCaptor.getAllValues().get(0).getEventType())
        .isEqualTo(YamlChangeSetEventType.BRANCH_SYNC.name());
  }
}