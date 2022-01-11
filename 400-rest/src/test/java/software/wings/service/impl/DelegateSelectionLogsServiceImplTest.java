/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl;

import static io.harness.beans.EnvironmentType.PROD;
import static io.harness.beans.FeatureName.DELEGATE_SELECTION_LOGS_DISABLED;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.ARVIND;
import static io.harness.rule.OwnerRule.JENNY;
import static io.harness.rule.OwnerRule.MARKO;
import static io.harness.rule.OwnerRule.SANJA;
import static io.harness.rule.OwnerRule.VUK;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import io.harness.annotations.dev.BreakDependencyOn;
import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.beans.Cd1SetupFields;
import io.harness.beans.DelegateTask;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.Delegate;
import io.harness.delegate.beans.DelegateEntityOwner;
import io.harness.delegate.beans.DelegateProfile;
import io.harness.delegate.beans.DelegateSelectionLogParams;
import io.harness.delegate.beans.DelegateSelectionLogResponse;
import io.harness.ff.FeatureFlagService;
import io.harness.persistence.HPersistence;
import io.harness.rule.Owner;
import io.harness.selection.log.BatchDelegateSelectionLog;
import io.harness.selection.log.DelegateSelectionLog;
import io.harness.selection.log.DelegateSelectionLog.DelegateSelectionLogBuilder;
import io.harness.selection.log.DelegateSelectionLog.DelegateSelectionLogKeys;
import io.harness.selection.log.DelegateSelectionLogMetadata;
import io.harness.selection.log.DelegateSelectionLogTaskMetadata;
import io.harness.selection.log.ProfileScopingRulesMetadata;
import io.harness.threading.Concurrent;

import software.wings.WingsBaseTest;
import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.Service;
import software.wings.dl.WingsPersistence;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@OwnedBy(HarnessTeam.DEL)
@TargetModule(HarnessModule._420_DELEGATE_SERVICE)
@BreakDependencyOn("io.harness.beans.Cd1SetupFields")
@BreakDependencyOn("software.wings.WingsBaseTest")
@BreakDependencyOn("software.wings.beans.Application")
@BreakDependencyOn("software.wings.beans.Environment")
@BreakDependencyOn("software.wings.beans.Service")
@BreakDependencyOn("software.wings.dl.WingsPersistence")
public class DelegateSelectionLogsServiceImplTest extends WingsBaseTest {
  private static final String DISCONNECTED = "Disconnected";
  private static final String INFO = "Info";
  private static final String REJECTED = "Rejected";
  private static final String SELECTED = "Selected";
  private static final String ACCEPTED = "Accepted";

  private static final String MISSING_SELECTOR_MESSAGE = "missing selector";

  @Inject protected WingsPersistence wingsPersistence;
  @Mock protected FeatureFlagService featureFlagService;
  @InjectMocks @Inject DelegateSelectionLogsServiceImpl delegateSelectionLogsService;
  @Inject private HPersistence persistence;

  private Map<String, String> obtainTaskSetupAbstractions() {
    String envId = generateUuid();
    Environment env = new Environment();
    env.setUuid(envId);
    env.setName("env-" + envId);
    env.setEnvironmentType(PROD);
    wingsPersistence.save(env);

    String serviceId = generateUuid();
    Service service = new Service();
    service.setUuid(serviceId);
    service.setName("srv-" + serviceId);
    wingsPersistence.save(service);

    String appId = generateUuid();
    Application app = new Application();
    app.setUuid(appId);
    app.setName("app-" + appId);
    wingsPersistence.save(app);

    Map<String, String> setupAbstractions = new HashMap<>();
    setupAbstractions.put(Cd1SetupFields.APP_ID_FIELD, appId);
    setupAbstractions.put(Cd1SetupFields.SERVICE_ID_FIELD, serviceId);
    setupAbstractions.put(Cd1SetupFields.ENV_ID_FIELD, envId);

    return setupAbstractions;
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void shouldSaveSelectionLog() {
    DelegateSelectionLog selectionLog =
        createDelegateSelectionLogBuilder().uuid(generateUuid()).message("ffenabled").groupId(generateUuid()).build();

    BatchDelegateSelectionLog batch =
        BatchDelegateSelectionLog.builder().delegateSelectionLogs(Arrays.asList(selectionLog)).build();

    delegateSelectionLogsService.save(batch);

    assertThat(wingsPersistence.get(DelegateSelectionLog.class, selectionLog.getUuid())).isNotNull();
  }

  @Test
  @Owner(developers = JENNY)
  @Category(UnitTests.class)
  public void shouldSaveDuplicates() {
    String taskId = generateUuid();
    String accountId = generateUuid();

    wingsPersistence.ensureIndexForTesting(DelegateSelectionLog.class);

    Concurrent.test(10, n -> {
      BatchDelegateSelectionLog batch = BatchDelegateSelectionLog.builder()
                                            .delegateSelectionLogs(createDelegateSelectionLogs(taskId, accountId))
                                            .build();
      delegateSelectionLogsService.save(batch);
    });

    assertThat(wingsPersistence.createQuery(DelegateSelectionLog.class)
                   .filter(DelegateSelectionLogKeys.taskId, taskId)
                   .filter(DelegateSelectionLogKeys.accountId, accountId)
                   .count())
        .isEqualTo(20L);
  }
  @Test
  @Owner(developers = VUK)
  @Category(UnitTests.class)
  public void shouldNotCreateBatch() {
    BatchDelegateSelectionLog result = delegateSelectionLogsService.createBatch(null);
    assertThat(result).isNull();

    result = delegateSelectionLogsService.createBatch(DelegateTask.builder().build());
    assertThat(result).isNull();

    result = delegateSelectionLogsService.createBatch(
        DelegateTask.builder().uuid(generateUuid()).selectionLogsTrackingEnabled(false).build());
    assertThat(result).isNull();
  }

  @Test
  @Owner(developers = VUK)
  @Category(UnitTests.class)
  public void shouldCreateBatch() {
    DelegateTask task = DelegateTask.builder()
                            .uuid(generateUuid())
                            .setupAbstraction("ng", "true")
                            .selectionLogsTrackingEnabled(true)
                            .build();
    BatchDelegateSelectionLog batchDelegateSelectionLog = delegateSelectionLogsService.createBatch(task);

    assertThat(batchDelegateSelectionLog).isNotNull();
    assertThat(batchDelegateSelectionLog.getDelegateSelectionLogs()).isNotNull();
    assertThat(batchDelegateSelectionLog.getDelegateSelectionLogs()).isEmpty();
    assertThat(batchDelegateSelectionLog.getTaskId()).isEqualTo(task.getUuid());
    assertThat(batchDelegateSelectionLog.isTaskNg()).isTrue();
  }

  @Test
  @Owner(developers = VUK)
  @Category(UnitTests.class)
  public void shouldNotLogCanAssign() {
    assertThatCode(() -> delegateSelectionLogsService.logNoIncludeScopeMatched(null, null, ""))
        .doesNotThrowAnyException();
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void shouldNotLogTaskAssigned() {
    assertThatCode(() -> delegateSelectionLogsService.logTaskAssigned(null, null, "")).doesNotThrowAnyException();
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void shouldLogTaskAssigned() {
    String taskId = generateUuid();
    String accountId = generateUuid();
    String delegate1Id = generateUuid();

    BatchDelegateSelectionLog batch = BatchDelegateSelectionLog.builder().taskId(taskId).build();

    delegateSelectionLogsService.logTaskAssigned(batch, accountId, delegate1Id);

    assertThat(batch.getDelegateSelectionLogs()).isNotEmpty();
    assertThat(batch.getDelegateSelectionLogs().get(0).getDelegateIds().size()).isEqualTo(1);
    assertThat(batch.getDelegateSelectionLogs().get(0).getAccountId()).isEqualTo(accountId);
    assertThat(batch.getDelegateSelectionLogs().get(0).getTaskId()).isEqualTo(taskId);
    assertThat(batch.getDelegateSelectionLogs().get(0).getConclusion()).isEqualTo(SELECTED);
    assertThat(batch.getDelegateSelectionLogs().get(0).getMessage()).isEqualTo("Delegate assigned for task execution");
    assertThat(batch.getDelegateSelectionLogs().get(0).getEventTimestamp()).isNotNull();
  }

  @Test
  @Owner(developers = VUK)
  @Category(UnitTests.class)
  public void shouldNotLogNoIncludeScopeMatched() {
    assertThatCode(() -> delegateSelectionLogsService.logNoIncludeScopeMatched(null, null, ""))
        .doesNotThrowAnyException();
  }

  @Test
  @Owner(developers = VUK)
  @Category(UnitTests.class)
  public void shouldLogNoIncludeScopeMatched() {
    String taskId = generateUuid();
    String accountId = generateUuid();
    String delegate1Id = generateUuid();
    String delegate2Id = generateUuid();

    BatchDelegateSelectionLog batch = BatchDelegateSelectionLog.builder().taskId(taskId).build();

    delegateSelectionLogsService.logNoIncludeScopeMatched(batch, accountId, delegate1Id);

    assertThat(batch.getDelegateSelectionLogs()).isNotEmpty();
    assertThat(batch.getDelegateSelectionLogs().get(0).getDelegateIds().size()).isEqualTo(1);
    assertThat(batch.getDelegateSelectionLogs().get(0).getAccountId()).isEqualTo(accountId);
    assertThat(batch.getDelegateSelectionLogs().get(0).getTaskId()).isEqualTo(taskId);
    assertThat(batch.getDelegateSelectionLogs().get(0).getConclusion()).isEqualTo(REJECTED);
    assertThat(batch.getDelegateSelectionLogs().get(0).getMessage())
        .isEqualTo("The delegate is not scoped to execute this task");
    assertThat(batch.getDelegateSelectionLogs().get(0).getEventTimestamp()).isNotNull();

    delegateSelectionLogsService.logNoIncludeScopeMatched(batch, accountId, delegate2Id);

    assertThat(batch.getDelegateSelectionLogs()).isNotEmpty();
    assertThat(batch.getDelegateSelectionLogs().get(0).getDelegateIds().size()).isEqualTo(2);
    assertThat(batch.getDelegateSelectionLogs().get(0).getAccountId()).isEqualTo(accountId);
    assertThat(batch.getDelegateSelectionLogs().get(0).getTaskId()).isEqualTo(taskId);
    assertThat(batch.getDelegateSelectionLogs().get(0).getConclusion()).isEqualTo(REJECTED);
    assertThat(batch.getDelegateSelectionLogs().get(0).getMessage())
        .isEqualTo("The delegate is not scoped to execute this task");
    assertThat(batch.getDelegateSelectionLogs().get(0).getEventTimestamp()).isNotNull();
  }

  @Test
  @Owner(developers = VUK)
  @Category(UnitTests.class)
  public void shouldNotLogExcludeScopeMatched() {
    assertThatCode(() -> delegateSelectionLogsService.logExcludeScopeMatched(null, null, "", null))
        .doesNotThrowAnyException();
  }

  @Test
  @Owner(developers = VUK)
  @Category(UnitTests.class)
  public void shouldLogExcludeScopeMatched() {
    String taskId = generateUuid();
    String accountId = generateUuid();
    String delegate1Id = generateUuid();
    String delegate2Id = generateUuid();

    BatchDelegateSelectionLog batch = BatchDelegateSelectionLog.builder().taskId(taskId).build();

    delegateSelectionLogsService.logExcludeScopeMatched(batch, accountId, delegate1Id, "testScope");

    assertThat(batch.getDelegateSelectionLogs()).isNotEmpty();
    assertThat(batch.getDelegateSelectionLogs().get(0).getDelegateIds().size()).isEqualTo(1);
    assertThat(batch.getDelegateSelectionLogs().get(0).getAccountId()).isEqualTo(accountId);
    assertThat(batch.getDelegateSelectionLogs().get(0).getTaskId()).isEqualTo(taskId);
    assertThat(batch.getDelegateSelectionLogs().get(0).getConclusion()).isEqualTo(REJECTED);
    assertThat(batch.getDelegateSelectionLogs().get(0).getMessage())
        .isEqualTo("Delegate is excluded to execute this task because of exclusion scope testScope");
    assertThat(batch.getDelegateSelectionLogs().get(0).getEventTimestamp()).isNotNull();
    delegateSelectionLogsService.logExcludeScopeMatched(batch, accountId, delegate2Id, "testScope");

    assertThat(batch.getDelegateSelectionLogs()).isNotEmpty();
    assertThat(batch.getDelegateSelectionLogs().get(0).getDelegateIds().size()).isEqualTo(2);
    assertThat(batch.getDelegateSelectionLogs().get(0).getAccountId()).isEqualTo(accountId);
    assertThat(batch.getDelegateSelectionLogs().get(0).getTaskId()).isEqualTo(taskId);
    assertThat(batch.getDelegateSelectionLogs().get(0).getConclusion()).isEqualTo(REJECTED);
    assertThat(batch.getDelegateSelectionLogs().get(0).getMessage())
        .isEqualTo("Delegate is excluded to execute this task because of exclusion scope testScope");
    assertThat(batch.getDelegateSelectionLogs().get(0).getEventTimestamp()).isNotNull();
  }

  @Test
  @Owner(developers = SANJA)
  @Category(UnitTests.class)
  public void shouldNotLogProfileScopeRuleNotMatched() {
    assertThatCode(() -> delegateSelectionLogsService.logProfileScopeRuleNotMatched(null, null, "", null, null))
        .doesNotThrowAnyException();
  }

  @Test
  @Owner(developers = SANJA)
  @Category(UnitTests.class)
  public void shouldLogProfileScopeNotMatched() {
    String taskId = generateUuid();
    String accountId = generateUuid();
    String delegate1Id = generateUuid();
    String delegate2Id = generateUuid();
    Set<String> scopingRulesDescriptions = ImmutableSet.<String>builder()
                                               .add("Application: Harness App; Services: service1, service2;")
                                               .add("Application: Test App; Environment: env1;")
                                               .build();

    String profile1Id = generateUuid();
    String profile2Id = generateUuid();

    BatchDelegateSelectionLog batch = BatchDelegateSelectionLog.builder().taskId(taskId).build();

    // Test adding first selection log to batch
    delegateSelectionLogsService.logProfileScopeRuleNotMatched(
        batch, accountId, delegate1Id, profile1Id, scopingRulesDescriptions);

    assertThat(batch.getDelegateSelectionLogs()).isNotEmpty();
    assertThat(batch.getDelegateSelectionLogs().get(0).getDelegateIds().size()).isEqualTo(1);
    assertThat(batch.getDelegateSelectionLogs().get(0).getAccountId()).isEqualTo(accountId);
    assertThat(batch.getDelegateSelectionLogs().get(0).getTaskId()).isEqualTo(taskId);
    assertThat(batch.getDelegateSelectionLogs().get(0).getConclusion()).isEqualTo(REJECTED);
    assertThat(batch.getDelegateSelectionLogs().get(0).getMessage())
        .isEqualTo("Delegate profile scoping rules not matched");
    assertThat(batch.getDelegateSelectionLogs().get(0).getEventTimestamp()).isNotNull();
    assertThat(batch.getDelegateSelectionLogs().get(0).getDelegateMetadata()).isNotEmpty();
    assertThat(batch.getDelegateSelectionLogs().get(0).getDelegateMetadata().get(delegate1Id)).isNotNull();
    assertThat(
        batch.getDelegateSelectionLogs().get(0).getDelegateMetadata().get(delegate1Id).getProfileScopingRulesMetadata())
        .isNotNull();
    assertThat(batch.getDelegateSelectionLogs()
                   .get(0)
                   .getDelegateMetadata()
                   .get(delegate1Id)
                   .getProfileScopingRulesMetadata()
                   .getProfileId())
        .isEqualTo(profile1Id);
    assertThat(batch.getDelegateSelectionLogs()
                   .get(0)
                   .getDelegateMetadata()
                   .get(delegate1Id)
                   .getProfileScopingRulesMetadata()
                   .getScopingRulesDescriptions())
        .containsExactlyInAnyOrderElementsOf(scopingRulesDescriptions);

    // Test adding second selection log with same message
    delegateSelectionLogsService.logProfileScopeRuleNotMatched(
        batch, accountId, delegate2Id, profile2Id, scopingRulesDescriptions);

    assertThat(batch.getDelegateSelectionLogs()).isNotEmpty();
    assertThat(batch.getDelegateSelectionLogs().get(0).getDelegateIds().size()).isEqualTo(2);
    assertThat(batch.getDelegateSelectionLogs().get(0).getAccountId()).isEqualTo(accountId);
    assertThat(batch.getDelegateSelectionLogs().get(0).getTaskId()).isEqualTo(taskId);
    assertThat(batch.getDelegateSelectionLogs().get(0).getConclusion()).isEqualTo(REJECTED);
    assertThat(batch.getDelegateSelectionLogs().get(0).getMessage())
        .isEqualTo("Delegate profile scoping rules not matched");
    assertThat(batch.getDelegateSelectionLogs().get(0).getEventTimestamp()).isNotNull();
    assertThat(batch.getDelegateSelectionLogs().get(0).getDelegateMetadata()).isNotEmpty();
    assertThat(batch.getDelegateSelectionLogs().get(0).getDelegateMetadata().get(delegate1Id)).isNotNull();
    assertThat(
        batch.getDelegateSelectionLogs().get(0).getDelegateMetadata().get(delegate1Id).getProfileScopingRulesMetadata())
        .isNotNull();
    assertThat(batch.getDelegateSelectionLogs()
                   .get(0)
                   .getDelegateMetadata()
                   .get(delegate1Id)
                   .getProfileScopingRulesMetadata()
                   .getProfileId())
        .isEqualTo(profile1Id);
    assertThat(batch.getDelegateSelectionLogs()
                   .get(0)
                   .getDelegateMetadata()
                   .get(delegate1Id)
                   .getProfileScopingRulesMetadata()
                   .getScopingRulesDescriptions())
        .containsExactlyInAnyOrderElementsOf(scopingRulesDescriptions);

    assertThat(batch.getDelegateSelectionLogs().get(0).getDelegateMetadata().get(delegate2Id)).isNotNull();
    assertThat(
        batch.getDelegateSelectionLogs().get(0).getDelegateMetadata().get(delegate2Id).getProfileScopingRulesMetadata())
        .isNotNull();
    assertThat(batch.getDelegateSelectionLogs()
                   .get(0)
                   .getDelegateMetadata()
                   .get(delegate2Id)
                   .getProfileScopingRulesMetadata()
                   .getProfileId())
        .isEqualTo(profile2Id);
    assertThat(batch.getDelegateSelectionLogs()
                   .get(0)
                   .getDelegateMetadata()
                   .get(delegate2Id)
                   .getProfileScopingRulesMetadata()
                   .getScopingRulesDescriptions())
        .containsExactlyInAnyOrderElementsOf(scopingRulesDescriptions);

    // Test adding duplicate
    delegateSelectionLogsService.logProfileScopeRuleNotMatched(
        batch, accountId, delegate2Id, profile2Id, scopingRulesDescriptions);

    assertThat(batch.getDelegateSelectionLogs()).isNotEmpty();
    assertThat(batch.getDelegateSelectionLogs().get(0).getDelegateIds().size()).isEqualTo(2);
  }

  @Test
  @Owner(developers = VUK)
  @Category(UnitTests.class)
  public void shouldNotLogMissingSelector() {
    assertThatCode(() -> delegateSelectionLogsService.logMissingSelector(null, null, "")).doesNotThrowAnyException();
  }

  @Test
  @Owner(developers = VUK)
  @Category(UnitTests.class)
  public void shouldLogMissingSelector() {
    String taskId = generateUuid();
    String accountId = generateUuid();
    String delegate1Id = generateUuid();
    String delegate2Id = generateUuid();
    String selector = generateUuid();
    String selectorOrigin = generateUuid();

    BatchDelegateSelectionLog batch = BatchDelegateSelectionLog.builder().taskId(taskId).build();

    delegateSelectionLogsService.logMissingSelector(batch, accountId, delegate1Id);

    assertThat(batch.getDelegateSelectionLogs()).isNotEmpty();
    assertThat(batch.getDelegateSelectionLogs().get(0).getDelegateIds().size()).isEqualTo(1);
    assertThat(batch.getDelegateSelectionLogs().get(0).getAccountId()).isEqualTo(accountId);
    assertThat(batch.getDelegateSelectionLogs().get(0).getTaskId()).isEqualTo(taskId);
    assertThat(batch.getDelegateSelectionLogs().get(0).getConclusion()).isEqualTo(REJECTED);
    assertThat(batch.getDelegateSelectionLogs().get(0).getMessage())
        .isEqualTo("The delegate selector tags are not part of the task selector tags");
    assertThat(batch.getDelegateSelectionLogs().get(0).getEventTimestamp()).isNotNull();
    delegateSelectionLogsService.logMissingSelector(batch, accountId, delegate2Id);

    assertThat(batch.getDelegateSelectionLogs()).isNotEmpty();
    assertThat(batch.getDelegateSelectionLogs().get(0).getDelegateIds().size()).isEqualTo(2);
    assertThat(batch.getDelegateSelectionLogs().get(0).getAccountId()).isEqualTo(accountId);
    assertThat(batch.getDelegateSelectionLogs().get(0).getTaskId()).isEqualTo(taskId);
    assertThat(batch.getDelegateSelectionLogs().get(0).getConclusion()).isEqualTo(REJECTED);
    assertThat(batch.getDelegateSelectionLogs().get(0).getMessage())
        .isEqualTo("The delegate selector tags are not part of the task selector tags");
    assertThat(batch.getDelegateSelectionLogs().get(0).getEventTimestamp()).isNotNull();
  }

  @Test
  @Owner(developers = JENNY)
  @Category(UnitTests.class)
  public void shouldLogMissingSelectorsWhenNoDelegateSelector() {
    String taskId = generateUuid();
    String accountId = generateUuid();
    String delegate1Id = generateUuid();

    BatchDelegateSelectionLog batch = BatchDelegateSelectionLog.builder().taskId(taskId).build();

    delegateSelectionLogsService.logMissingSelector(batch, accountId, delegate1Id);

    assertThat(batch.getDelegateSelectionLogs()).isNotEmpty();
    assertThat(batch.getDelegateSelectionLogs().get(0).getDelegateIds().size()).isEqualTo(1);
    assertThat(batch.getDelegateSelectionLogs().get(0).getAccountId()).isEqualTo(accountId);
    assertThat(batch.getDelegateSelectionLogs().get(0).getTaskId()).isEqualTo(taskId);
    assertThat(batch.getDelegateSelectionLogs().get(0).getConclusion()).isEqualTo(REJECTED);
    assertThat(batch.getDelegateSelectionLogs().get(0).getMessage())
        .isEqualTo("The delegate selector tags are not part of the task selector tags");
    assertThat(batch.getDelegateSelectionLogs().get(0).getEventTimestamp()).isNotNull();
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void shouldLogDisconnectedDelegate() {
    String taskId = generateUuid();
    String accountId = generateUuid();
    String delegate1Id = generateUuid();
    String delegate2Id = generateUuid();
    Set<String> disconnectedDelegates = new HashSet<>();
    disconnectedDelegates.add(delegate1Id);
    disconnectedDelegates.add(delegate2Id);

    BatchDelegateSelectionLog batch = BatchDelegateSelectionLog.builder().taskId(taskId).build();

    delegateSelectionLogsService.logDisconnectedDelegate(batch, accountId, disconnectedDelegates);

    assertThat(batch.getDelegateSelectionLogs()).isNotEmpty();
    assertThat(batch.getDelegateSelectionLogs().get(0).getDelegateIds().size()).isEqualTo(2);
    assertThat(
        batch.getDelegateSelectionLogs().get(0).getDelegateIds().containsAll(Arrays.asList(delegate1Id, delegate2Id)))
        .isTrue();
    assertThat(batch.getDelegateSelectionLogs().get(0).getAccountId()).isEqualTo(accountId);
    assertThat(batch.getDelegateSelectionLogs().get(0).getTaskId()).isEqualTo(taskId);
    assertThat(batch.getDelegateSelectionLogs().get(0).getConclusion()).isEqualTo(INFO);
    assertThat(batch.getDelegateSelectionLogs().get(0).getMessage())
        .isEqualTo("Not broadcasting to delegate(s) since disconnected");
    assertThat(batch.getDelegateSelectionLogs().get(0).getEventTimestamp()).isNotNull();
  }

  @Test
  @Owner(developers = VUK)
  @Category(UnitTests.class)
  public void shouldFetchTaskSelectionLogs() {
    String taskId = generateUuid();
    String accountId = generateUuid();
    String delegate1Id = generateUuid();
    String delegate2Id = generateUuid();
    String message = generateUuid();

    Set<String> delegateIds1 = new HashSet<>();
    delegateIds1.add(delegate1Id);

    Set<String> delegateIds2 = new HashSet<>();
    delegateIds2.add(delegate1Id);
    delegateIds2.add(delegate2Id);

    DelegateSelectionLog delegateSelectionLog1 = DelegateSelectionLog.builder()
                                                     .taskId(taskId)
                                                     .accountId(accountId)
                                                     .message(message)
                                                     .delegateIds(delegateIds1)
                                                     .eventTimestamp(System.currentTimeMillis())
                                                     .build();

    DelegateSelectionLog delegateSelectionLog2 = DelegateSelectionLog.builder()
                                                     .taskId(taskId)
                                                     .accountId(accountId)
                                                     .message(message)
                                                     .delegateIds(delegateIds2)
                                                     .eventTimestamp(System.currentTimeMillis())
                                                     .build();
    wingsPersistence.save(delegateSelectionLog1);
    wingsPersistence.save(delegateSelectionLog2);

    List<DelegateSelectionLogParams> delegateSelectionLogParams =
        delegateSelectionLogsService.fetchTaskSelectionLogs(accountId, taskId);

    assertThat(delegateSelectionLogParams.size()).isEqualTo(3);
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void shouldFetchTaskSelectionLogsAndMapEntitiesCorrectly() {
    String taskId = generateUuid();
    String accountId = generateUuid();

    DelegateProfile delegateProfile =
        DelegateProfile.builder().accountId(accountId).uuid(generateUuid()).name("selectionLogProfile").build();

    Delegate delegate = Delegate.builder()
                            .accountId(accountId)
                            .uuid(generateUuid())
                            .delegateType("dummyType")
                            .delegateName("name")
                            .hostName("hostname")
                            .delegateProfileId(delegateProfile.getUuid())
                            .build();

    Set<String> delegateIds = new HashSet<>();
    delegateIds.add(delegate.getUuid());

    ImmutableSet<String> scopingRulesDescriptions = ImmutableSet.<String>builder().add("desc1").add("desc2").build();

    Map<String, DelegateSelectionLogMetadata> metadataMap = new HashMap<>();
    metadataMap.put(delegate.getUuid(),
        DelegateSelectionLogMetadata.builder()
            .profileScopingRulesMetadata(ProfileScopingRulesMetadata.builder()
                                             .profileId(delegateProfile.getUuid())
                                             .scopingRulesDescriptions(scopingRulesDescriptions)
                                             .build())
            .build());

    DelegateSelectionLog delegateSelectionLog = DelegateSelectionLog.builder()
                                                    .taskId(taskId)
                                                    .accountId(accountId)
                                                    .message("message")
                                                    .conclusion(SELECTED)
                                                    .delegateIds(delegateIds)
                                                    .eventTimestamp(System.currentTimeMillis())
                                                    .delegateMetadata(metadataMap)
                                                    .build();

    wingsPersistence.save(delegateProfile);
    wingsPersistence.save(delegate);
    wingsPersistence.save(delegateSelectionLog);

    List<DelegateSelectionLogParams> delegateSelectionLogParams =
        delegateSelectionLogsService.fetchTaskSelectionLogs(accountId, taskId);

    assertThat(delegateSelectionLogParams.size()).isEqualTo(1);
    assertThat(delegateSelectionLogParams.get(0).getDelegateId()).isEqualTo(delegate.getUuid());
    assertThat(delegateSelectionLogParams.get(0).getDelegateType()).isEqualTo(delegate.getDelegateType());
    assertThat(delegateSelectionLogParams.get(0).getDelegateName()).isEqualTo(delegate.getDelegateName());
    assertThat(delegateSelectionLogParams.get(0).getDelegateHostName()).isEqualTo(delegate.getHostName());
    assertThat(delegateSelectionLogParams.get(0).getDelegateProfileName()).isEqualTo(delegateProfile.getName());
    assertThat(delegateSelectionLogParams.get(0).getConclusion()).isEqualTo(delegateSelectionLog.getConclusion());
    assertThat(delegateSelectionLogParams.get(0).getMessage()).isEqualTo(delegateSelectionLog.getMessage());
    assertThat(delegateSelectionLogParams.get(0).getEventTimestamp())
        .isEqualTo(delegateSelectionLog.getEventTimestamp());
    assertThat(delegateSelectionLogParams.get(0).getProfileScopingRulesDetails()).isNotNull();
    assertThat(delegateSelectionLogParams.get(0).getProfileScopingRulesDetails().getProfileId())
        .isEqualTo(delegateProfile.getUuid());
    assertThat(delegateSelectionLogParams.get(0).getProfileScopingRulesDetails().getProfileName())
        .isEqualTo(delegateProfile.getName());
    assertThat(delegateSelectionLogParams.get(0).getProfileScopingRulesDetails().getScopingRulesDescriptions())
        .containsExactlyInAnyOrderElementsOf(scopingRulesDescriptions);
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void shouldFetchTaskSelectionLogsForNonExistingDelegate() {
    String taskId = generateUuid();
    String accountId = generateUuid();
    String delegateId = generateUuid();

    Set<String> delegateIds = new HashSet<>();
    delegateIds.add(delegateId);

    DelegateSelectionLog delegateSelectionLog = DelegateSelectionLog.builder()
                                                    .taskId(taskId)
                                                    .accountId(accountId)
                                                    .message("message")
                                                    .conclusion(SELECTED)
                                                    .delegateIds(delegateIds)
                                                    .eventTimestamp(System.currentTimeMillis())
                                                    .build();

    wingsPersistence.save(delegateSelectionLog);

    List<DelegateSelectionLogParams> delegateSelectionLogParams =
        delegateSelectionLogsService.fetchTaskSelectionLogs(accountId, taskId);

    assertThat(delegateSelectionLogParams.size()).isEqualTo(1);
    assertThat(delegateSelectionLogParams.get(0).getDelegateId()).isEqualTo(delegateId);
    assertThat(delegateSelectionLogParams.get(0).getDelegateName()).isEqualTo(delegateId);
    assertThat(delegateSelectionLogParams.get(0).getDelegateHostName()).isEqualTo("");
    assertThat(delegateSelectionLogParams.get(0).getDelegateProfileName()).isEqualTo("");
    assertThat(delegateSelectionLogParams.get(0).getConclusion()).isEqualTo(delegateSelectionLog.getConclusion());
    assertThat(delegateSelectionLogParams.get(0).getMessage()).isEqualTo(delegateSelectionLog.getMessage());
    assertThat(delegateSelectionLogParams.get(0).getEventTimestamp())
        .isEqualTo(delegateSelectionLog.getEventTimestamp());
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void shouldFetchTaskSelectionLogsForNonExistingDelegateProfile() {
    String taskId = generateUuid();
    String accountId = generateUuid();
    String delegateProfileId = generateUuid();

    Delegate delegate = Delegate.builder()
                            .accountId(accountId)
                            .uuid(generateUuid())
                            .delegateName("name")
                            .hostName("hostname")
                            .delegateProfileId(delegateProfileId)
                            .build();

    Set<String> delegateIds = new HashSet<>();
    delegateIds.add(delegate.getUuid());

    ImmutableSet<String> scopingRulesDescriptions = ImmutableSet.<String>builder().add("desc1").add("desc2").build();

    Map<String, DelegateSelectionLogMetadata> metadataMap = new HashMap<>();
    metadataMap.put(delegate.getUuid(),
        DelegateSelectionLogMetadata.builder()
            .profileScopingRulesMetadata(ProfileScopingRulesMetadata.builder()
                                             .profileId(delegateProfileId)
                                             .scopingRulesDescriptions(scopingRulesDescriptions)
                                             .build())
            .build());

    DelegateSelectionLog delegateSelectionLog = DelegateSelectionLog.builder()
                                                    .taskId(taskId)
                                                    .accountId(accountId)
                                                    .message("message")
                                                    .conclusion(SELECTED)
                                                    .delegateIds(delegateIds)
                                                    .eventTimestamp(System.currentTimeMillis())
                                                    .delegateMetadata(metadataMap)
                                                    .build();

    wingsPersistence.save(delegate);
    wingsPersistence.save(delegateSelectionLog);

    List<DelegateSelectionLogParams> delegateSelectionLogParams =
        delegateSelectionLogsService.fetchTaskSelectionLogs(accountId, taskId);

    assertThat(delegateSelectionLogParams.size()).isEqualTo(1);
    assertThat(delegateSelectionLogParams.get(0).getDelegateId()).isEqualTo(delegate.getUuid());
    assertThat(delegateSelectionLogParams.get(0).getDelegateName()).isEqualTo(delegate.getDelegateName());
    assertThat(delegateSelectionLogParams.get(0).getDelegateHostName()).isEqualTo(delegate.getHostName());
    assertThat(delegateSelectionLogParams.get(0).getDelegateProfileName()).isEqualTo("");
    assertThat(delegateSelectionLogParams.get(0).getConclusion()).isEqualTo(delegateSelectionLog.getConclusion());
    assertThat(delegateSelectionLogParams.get(0).getMessage()).isEqualTo(delegateSelectionLog.getMessage());
    assertThat(delegateSelectionLogParams.get(0).getEventTimestamp())
        .isEqualTo(delegateSelectionLog.getEventTimestamp());
    assertThat(delegateSelectionLogParams.get(0).getProfileScopingRulesDetails()).isNotNull();
    assertThat(delegateSelectionLogParams.get(0).getProfileScopingRulesDetails().getProfileId())
        .isEqualTo(delegateProfileId);
    assertThat(delegateSelectionLogParams.get(0).getProfileScopingRulesDetails().getProfileName()).isNull();
    assertThat(delegateSelectionLogParams.get(0).getProfileScopingRulesDetails().getScopingRulesDescriptions())
        .containsExactlyInAnyOrderElementsOf(scopingRulesDescriptions);
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void shouldFetchSelectedDelegateForTaskWithEmptyOptional() {
    Optional<DelegateSelectionLogParams> logParamsOptional =
        delegateSelectionLogsService.fetchSelectedDelegateForTask(generateUuid(), generateUuid());

    assertThat(logParamsOptional.isPresent()).isFalse();
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void shouldFetchSelectedDelegateForTask() {
    String taskId = generateUuid();
    String accountId = generateUuid();
    String delegateId = generateUuid();
    String message = generateUuid();

    Set<String> delegateIds = new HashSet<>();
    delegateIds.add(delegateId);

    DelegateSelectionLog delegateSelectionLog1 = DelegateSelectionLog.builder()
                                                     .taskId(taskId)
                                                     .accountId(accountId)
                                                     .message(message)
                                                     .delegateIds(delegateIds)
                                                     .conclusion(SELECTED)
                                                     .eventTimestamp(System.currentTimeMillis())
                                                     .build();

    DelegateSelectionLog delegateSelectionLog2 = DelegateSelectionLog.builder()
                                                     .taskId(taskId)
                                                     .accountId(accountId)
                                                     .message(message)
                                                     .delegateIds(delegateIds)
                                                     .conclusion(ACCEPTED)
                                                     .eventTimestamp(System.currentTimeMillis())
                                                     .build();
    wingsPersistence.save(delegateSelectionLog1);
    wingsPersistence.save(delegateSelectionLog2);

    Optional<DelegateSelectionLogParams> logParamsOptional =
        delegateSelectionLogsService.fetchSelectedDelegateForTask(accountId, taskId);

    assertThat(logParamsOptional.isPresent()).isTrue();
    assertThat(logParamsOptional.get().getConclusion()).isEqualTo(delegateSelectionLog1.getConclusion());
    assertThat(logParamsOptional.get().getDelegateId()).isEqualTo(delegateId);
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void testProcessSetupAbstractions() {
    // Test empty setupAbstractions
    assertThat(delegateSelectionLogsService.processSetupAbstractions(null)).isNull();

    // Test filled setupAbstractions
    Map<String, String> taskSetupAbstractions = obtainTaskSetupAbstractions();
    String infraMappingId = generateUuid();
    taskSetupAbstractions.put(Cd1SetupFields.INFRASTRUCTURE_MAPPING_ID_FIELD, infraMappingId);

    Map<String, String> processedSetupAbstractions =
        delegateSelectionLogsService.processSetupAbstractions(taskSetupAbstractions);

    assertThat(processedSetupAbstractions.get(Cd1SetupFields.APPLICATION))
        .isEqualTo("app-" + taskSetupAbstractions.get(Cd1SetupFields.APP_ID_FIELD));
    assertThat(processedSetupAbstractions.get(Cd1SetupFields.ENVIRONMENT))
        .isEqualTo("env-" + taskSetupAbstractions.get(Cd1SetupFields.ENV_ID_FIELD));
    assertThat(processedSetupAbstractions.get(Cd1SetupFields.SERVICE))
        .isEqualTo("srv-" + taskSetupAbstractions.get(Cd1SetupFields.SERVICE_ID_FIELD));
    assertThat(processedSetupAbstractions.get(Cd1SetupFields.INFRASTRUCTURE_MAPPING_ID_FIELD))
        .isEqualTo(infraMappingId);
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void shouldNotLogMustExecuteOnDelegateMatched() {
    assertThatCode(() -> delegateSelectionLogsService.logMustExecuteOnDelegateMatched(null, null, ""))
        .doesNotThrowAnyException();
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void shouldFetchTaskSelectionLogsData() {
    String taskId = generateUuid();
    String accountId = generateUuid();

    Set<String> delegateIds1 = new HashSet<>();
    delegateIds1.add(generateUuid());

    DelegateSelectionLog delegateSelectionLog1 = DelegateSelectionLog.builder()
                                                     .taskId(taskId)
                                                     .accountId(accountId)
                                                     .message(generateUuid())
                                                     .delegateIds(delegateIds1)
                                                     .eventTimestamp(System.currentTimeMillis())
                                                     .build();

    wingsPersistence.save(delegateSelectionLog1);

    Map<String, String> taskSetupAbstractions = new HashMap<>();
    taskSetupAbstractions.put(Cd1SetupFields.APPLICATION, generateUuid());
    taskSetupAbstractions.put(Cd1SetupFields.SERVICE, generateUuid());
    taskSetupAbstractions.put(Cd1SetupFields.ENVIRONMENT, generateUuid());
    taskSetupAbstractions.put(Cd1SetupFields.ENVIRONMENT_TYPE, generateUuid());
    taskSetupAbstractions.put(Cd1SetupFields.INFRASTRUCTURE_MAPPING_ID_FIELD, generateUuid());

    DelegateSelectionLogTaskMetadata taskMetadata = DelegateSelectionLogTaskMetadata.builder()
                                                        .taskId(taskId)
                                                        .accountId(accountId)
                                                        .setupAbstractions(taskSetupAbstractions)
                                                        .build();

    wingsPersistence.save(taskMetadata);

    DelegateSelectionLogResponse delegateSelectionLogResponse =
        delegateSelectionLogsService.fetchTaskSelectionLogsData(accountId, taskId);

    assertThat(delegateSelectionLogResponse).isNotNull();
    assertThat(delegateSelectionLogResponse.getDelegateSelectionLogs()).isNotNull();
    assertThat(delegateSelectionLogResponse.getDelegateSelectionLogs().size()).isEqualTo(1);
    assertThat(delegateSelectionLogResponse.getTaskSetupAbstractions()).isNotNull();
    assertThat(delegateSelectionLogResponse.getTaskSetupAbstractions().containsKey(Cd1SetupFields.APPLICATION))
        .isTrue();
    assertThat(delegateSelectionLogResponse.getTaskSetupAbstractions().containsKey(Cd1SetupFields.SERVICE)).isTrue();
    assertThat(delegateSelectionLogResponse.getTaskSetupAbstractions().containsKey(Cd1SetupFields.ENVIRONMENT))
        .isTrue();
    assertThat(delegateSelectionLogResponse.getTaskSetupAbstractions().containsKey(Cd1SetupFields.ENVIRONMENT_TYPE))
        .isTrue();
    assertThat(delegateSelectionLogResponse.getTaskSetupAbstractions().containsKey(
                   Cd1SetupFields.INFRASTRUCTURE_MAPPING_ID_FIELD))
        .isFalse();

    // Test backward compatibility for selection logs without setup abstractions
    wingsPersistence.delete(taskMetadata);
    delegateSelectionLogResponse = delegateSelectionLogsService.fetchTaskSelectionLogsData(accountId, taskId);

    assertThat(delegateSelectionLogResponse).isNotNull();
    assertThat(delegateSelectionLogResponse.getDelegateSelectionLogs()).isNotNull();
    assertThat(delegateSelectionLogResponse.getDelegateSelectionLogs().size()).isEqualTo(1);
    assertThat(delegateSelectionLogResponse.getTaskSetupAbstractions()).isNullOrEmpty();
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void shouldLogMustExecuteOnDelegateMatched() {
    String taskId = generateUuid();
    String accountId = generateUuid();
    String delegate1Id = generateUuid();

    BatchDelegateSelectionLog batch = BatchDelegateSelectionLog.builder().taskId(taskId).build();

    delegateSelectionLogsService.logMustExecuteOnDelegateMatched(batch, accountId, delegate1Id);

    assertThat(batch.getDelegateSelectionLogs()).isNotEmpty();
    assertThat(batch.getDelegateSelectionLogs().get(0).getDelegateIds().size()).isEqualTo(1);
    assertThat(batch.getDelegateSelectionLogs().get(0).getAccountId()).isEqualTo(accountId);
    assertThat(batch.getDelegateSelectionLogs().get(0).getTaskId()).isEqualTo(taskId);
    assertThat(batch.getDelegateSelectionLogs().get(0).getConclusion()).isEqualTo(ACCEPTED);
    assertThat(batch.getDelegateSelectionLogs().get(0).getMessage())
        .isEqualTo("Delegate was targeted for profile script execution");
    assertThat(batch.getDelegateSelectionLogs().get(0).getEventTimestamp()).isNotNull();
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void shouldNotLogMustExecuteOnDelegateNotMatched() {
    assertThatCode(() -> delegateSelectionLogsService.logMustExecuteOnDelegateNotMatched(null, null, ""))
        .doesNotThrowAnyException();
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void shouldLogMustExecuteOnDelegateNotMatched() {
    String taskId = generateUuid();
    String accountId = generateUuid();
    String delegate1Id = generateUuid();

    BatchDelegateSelectionLog batch = BatchDelegateSelectionLog.builder().taskId(taskId).build();

    delegateSelectionLogsService.logMustExecuteOnDelegateNotMatched(batch, accountId, delegate1Id);

    assertThat(batch.getDelegateSelectionLogs()).isNotEmpty();
    assertThat(batch.getDelegateSelectionLogs().get(0).getDelegateIds().size()).isEqualTo(1);
    assertThat(batch.getDelegateSelectionLogs().get(0).getAccountId()).isEqualTo(accountId);
    assertThat(batch.getDelegateSelectionLogs().get(0).getTaskId()).isEqualTo(taskId);
    assertThat(batch.getDelegateSelectionLogs().get(0).getConclusion()).isEqualTo(REJECTED);
    assertThat(batch.getDelegateSelectionLogs().get(0).getMessage())
        .isEqualTo("Delegate was not targeted for profile script execution");
    assertThat(batch.getDelegateSelectionLogs().get(0).getEventTimestamp()).isNotNull();
  }

  @Test
  @Owner(developers = ARVIND)
  @Category(UnitTests.class)
  public void shouldNotLogOwnerRuleNotMatched() {
    assertThatCode(() -> delegateSelectionLogsService.logOwnerRuleNotMatched(null, null, null, null))
        .doesNotThrowAnyException();
    assertThatCode(()
                       -> delegateSelectionLogsService.logOwnerRuleNotMatched(
                           BatchDelegateSelectionLog.builder().build(), null, null, null))
        .doesNotThrowAnyException();
  }

  @Test
  @Owner(developers = ARVIND)
  @Category(UnitTests.class)
  public void testLogOwnerRuleNotMatched() {
    BatchDelegateSelectionLog batch = BatchDelegateSelectionLog.builder().taskId(generateUuid()).build();
    String accountId = generateUuid();
    String delegateId = generateUuid();

    DelegateEntityOwner owner = DelegateEntityOwner.builder().identifier("orgId/projectId").build();

    delegateSelectionLogsService.logOwnerRuleNotMatched(batch, accountId, Sets.newHashSet(delegateId), owner);
    assertThat(batch.getDelegateSelectionLogs()).hasSize(1);
    assertThat(batch.getDelegateSelectionLogs().get(0).getMessage()).isEqualTo("No matching owner: orgId/projectId");
    assertThat(batch.getDelegateSelectionLogs().get(0).getConclusion()).isEqualTo(REJECTED);
  }

  @Test
  @Owner(developers = JENNY)
  @Category(UnitTests.class)
  public void shouldLogNoEligibleDelegates() {
    String taskId = generateUuid();
    String accountId = generateUuid();
    BatchDelegateSelectionLog batch = BatchDelegateSelectionLog.builder().taskId(taskId).build();

    delegateSelectionLogsService.logNoEligibleDelegatesToExecuteTask(batch, accountId);

    assertThat(batch.getDelegateSelectionLogs()).isNotEmpty();
    assertThat(batch.getDelegateSelectionLogs().get(0).getDelegateIds().size()).isEqualTo(0);
    assertThat(batch.getDelegateSelectionLogs().get(0).getAccountId()).isEqualTo(accountId);
    assertThat(batch.getDelegateSelectionLogs().get(0).getTaskId()).isEqualTo(taskId);
    assertThat(batch.getDelegateSelectionLogs().get(0).getConclusion()).isEqualTo(REJECTED);
    assertThat(batch.getDelegateSelectionLogs().get(0).getMessage())
        .isEqualTo("No eligible delegates in account to execute task");
    assertThat(batch.getDelegateSelectionLogs().get(0).getEventTimestamp()).isNotNull();
  }

  @Test
  @Owner(developers = JENNY)
  @Category(UnitTests.class)
  public void shouldNotLogNoEligibleDelegates() {
    assertThatCode(() -> delegateSelectionLogsService.logNoEligibleDelegatesToExecuteTask(null, null))
        .doesNotThrowAnyException();
    assertThatCode(()
                       -> delegateSelectionLogsService.logNoEligibleDelegatesToExecuteTask(
                           BatchDelegateSelectionLog.builder().build(), null))
        .doesNotThrowAnyException();
  }

  @Test
  @Owner(developers = JENNY)
  @Category(UnitTests.class)
  public void shouldLogNoEligibleAvailableDelegates() {
    String taskId = generateUuid();
    String accountId = generateUuid();

    BatchDelegateSelectionLog batch = BatchDelegateSelectionLog.builder().taskId(taskId).build();

    delegateSelectionLogsService.logNoEligibleDelegatesAvailableToExecuteTask(batch, accountId);

    assertThat(batch.getDelegateSelectionLogs()).isNotEmpty();
    assertThat(batch.getDelegateSelectionLogs().get(0).getDelegateIds().size()).isEqualTo(0);
    assertThat(batch.getDelegateSelectionLogs().get(0).getAccountId()).isEqualTo(accountId);
    assertThat(batch.getDelegateSelectionLogs().get(0).getTaskId()).isEqualTo(taskId);
    assertThat(batch.getDelegateSelectionLogs().get(0).getConclusion()).isEqualTo(REJECTED);
    assertThat(batch.getDelegateSelectionLogs().get(0).getMessage())
        .isEqualTo("No eligible delegates in account available to execute task");
    assertThat(batch.getDelegateSelectionLogs().get(0).getEventTimestamp()).isNotNull();
  }

  @Test
  @Owner(developers = JENNY)
  @Category(UnitTests.class)
  public void shouldNotLogNoEligibleAvailableDelegates() {
    assertThatCode(() -> delegateSelectionLogsService.logNoEligibleDelegatesAvailableToExecuteTask(null, null))
        .doesNotThrowAnyException();
    assertThatCode(()
                       -> delegateSelectionLogsService.logNoEligibleDelegatesAvailableToExecuteTask(
                           BatchDelegateSelectionLog.builder().build(), null))
        .doesNotThrowAnyException();
  }

  @Test
  @Owner(developers = JENNY)
  @Category(UnitTests.class)
  public void shouldLogEligibleDelegatesToExecuteTask() {
    String taskId = generateUuid();
    String accountId = generateUuid();
    String delegateId = generateUuid();

    BatchDelegateSelectionLog batch = BatchDelegateSelectionLog.builder().taskId(taskId).build();

    delegateSelectionLogsService.logEligibleDelegatesToExecuteTask(batch, Sets.newHashSet(delegateId), accountId);

    assertThat(batch.getDelegateSelectionLogs()).isNotEmpty();
    assertThat(batch.getDelegateSelectionLogs().get(0).getDelegateIds().size()).isEqualTo(1);
    assertThat(batch.getDelegateSelectionLogs().get(0).getAccountId()).isEqualTo(accountId);
    assertThat(batch.getDelegateSelectionLogs().get(0).getTaskId()).isEqualTo(taskId);
    assertThat(batch.getDelegateSelectionLogs().get(0).getConclusion()).isEqualTo(INFO);
    assertThat(batch.getDelegateSelectionLogs().get(0).getMessage()).isEqualTo("Delegate eligible to execute task");
    assertThat(batch.getDelegateSelectionLogs().get(0).getEventTimestamp()).isNotNull();
  }

  @Test
  @Owner(developers = JENNY)
  @Category(UnitTests.class)
  public void shouldNotLogEligibleDelegatesToExecuteTask() {
    assertThatCode(() -> delegateSelectionLogsService.logEligibleDelegatesToExecuteTask(null, null, null))
        .doesNotThrowAnyException();
    assertThatCode(()
                       -> delegateSelectionLogsService.logEligibleDelegatesToExecuteTask(
                           BatchDelegateSelectionLog.builder().build(), null, null))
        .doesNotThrowAnyException();
  }

  @Test
  @Owner(developers = JENNY)
  @Category(UnitTests.class)
  public void shouldLogBroadcastToDelegate() {
    String taskId = generateUuid();
    String accountId = generateUuid();
    String delegateId = generateUuid();

    BatchDelegateSelectionLog batch = BatchDelegateSelectionLog.builder().taskId(taskId).build();

    delegateSelectionLogsService.logBroadcastToDelegate(batch, Sets.newHashSet(delegateId), accountId);

    assertThat(batch.getDelegateSelectionLogs()).isNotEmpty();
    assertThat(batch.getDelegateSelectionLogs().get(0).getDelegateIds().size()).isEqualTo(1);
    assertThat(batch.getDelegateSelectionLogs().get(0).getAccountId()).isEqualTo(accountId);
    assertThat(batch.getDelegateSelectionLogs().get(0).getTaskId()).isEqualTo(taskId);
    assertThat(batch.getDelegateSelectionLogs().get(0).getConclusion()).isEqualTo(INFO);
    assertThat(batch.getDelegateSelectionLogs().get(0).getMessage()).isEqualTo("Broadcasting to delegate");
    assertThat(batch.getDelegateSelectionLogs().get(0).getEventTimestamp()).isNotNull();
  }

  @Test
  @Owner(developers = JENNY)
  @Category(UnitTests.class)
  public void shouldNotLogBroadcastToDelegate() {
    assertThatCode(() -> delegateSelectionLogsService.logBroadcastToDelegate(null, null, null))
        .doesNotThrowAnyException();
    assertThatCode(()
                       -> delegateSelectionLogsService.logBroadcastToDelegate(
                           BatchDelegateSelectionLog.builder().build(), null, null))
        .doesNotThrowAnyException();
  }

  @Test
  @Owner(developers = JENNY)
  @Category(UnitTests.class)
  public void shouldNotGenerateSelectionLog() {
    String accountId = generateUuid();
    String taskId = generateUuid();
    when(featureFlagService.isEnabled(any(), anyString())).thenReturn(true);
    BatchDelegateSelectionLog batch = BatchDelegateSelectionLog.builder().taskId(taskId).build();
    when(featureFlagService.isEnabled(DELEGATE_SELECTION_LOGS_DISABLED, accountId)).thenReturn(true);
    delegateSelectionLogsService.save(batch);
    assertThat(persistence.get(DelegateSelectionLog.class, batch.getTaskId())).isNull();
  }

  private DelegateSelectionLogBuilder createDelegateSelectionLogBuilder() {
    return DelegateSelectionLog.builder().accountId(generateUuid()).taskId(generateUuid());
  }

  private List<DelegateSelectionLog> createDelegateSelectionLogs(String taskId, String accountId) {
    DelegateSelectionLog selectionLog1 = createDelegateSelectionLogBuilder()
                                             .taskId(taskId)
                                             .accountId(accountId)
                                             .message(MISSING_SELECTOR_MESSAGE)
                                             .build();

    DelegateSelectionLog selectionLog2 = createDelegateSelectionLogBuilder()
                                             .taskId(taskId)
                                             .accountId(accountId)
                                             .message(MISSING_SELECTOR_MESSAGE)
                                             .build();

    return Arrays.asList(selectionLog1, selectionLog2);
  }
}
