package software.wings.service.impl;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.MARKO;
import static io.harness.rule.OwnerRule.SANJA;
import static io.harness.rule.OwnerRule.VUK;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.beans.DelegateTask;
import io.harness.beans.FeatureName;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.DelegateProfile;
import io.harness.delegate.beans.DelegateSelectionLogParams;
import io.harness.rule.Owner;
import io.harness.selection.log.BatchDelegateSelectionLog;
import io.harness.selection.log.DelegateSelectionLog;
import io.harness.selection.log.DelegateSelectionLog.DelegateSelectionLogBuilder;
import io.harness.selection.log.DelegateSelectionLog.DelegateSelectionLogKeys;
import io.harness.threading.Concurrent;

import software.wings.WingsBaseTest;
import software.wings.beans.Delegate;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.DelegateSelectionLogsService;
import software.wings.service.intfc.FeatureFlagService;

import com.google.inject.Inject;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;

public class DelegateSelectionLogsServiceImplTest extends WingsBaseTest {
  private static final String WAITING_FOR_APPROVAL = "Waiting for Approval";
  private static final String DISCONNECTED = "Disconnected";
  private static final String REJECTED = "Rejected";
  private static final String SELECTED = "Selected";
  private static final String ACCEPTED = "Accepted";

  private static final String CAN_ASSIGN_GROUP_ID = "CAN_ASSIGN_GROUP_ID";
  private static final String NO_INCLUDE_SCOPE_MATCHED_GROUP_ID = "NO_INCLUDE_SCOPE_MATCHED_GROUP_ID";
  private static final String EXCLUDE_SCOPE_MATCHED_GROUP_ID = "EXCLUDE_SCOPE_MATCHED_GROUP_ID";
  private static final String MISSING_SELECTOR_GROUP_ID = "MISSING_SELECTOR_GROUP_ID";
  private static final String MISSING_ALL_SELECTORS_GROUP_ID = "MISSING_ALL_SELECTORS_GROUP_ID";
  private static final String DISCONNECTED_GROUP_ID = "DISCONNECTED_GROUP_ID";
  private static final String WAITING_ON_APPROVAL_GROUP_ID = "WAITING_ON_APPROVAL_GROUP_ID";
  private static final String PROFILE_SCOPE_RULE_NOT_MATCHED_GROUP_ID = "PROFILE_SCOPE_RULE_NOT_MATCHED_GROUP_ID";
  private static final String TASK_ASSIGNED_GROUP_ID = "TASK_ASSIGNED_GROUP_ID";
  private static final String MISSING_SELECTOR_MESSAGE = "missing selector";

  @Mock private FeatureFlagService featureFlagService;
  @Inject protected WingsPersistence wingsPersistence;
  @InjectMocks @Inject DelegateSelectionLogsService delegateSelectionLogsService;

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void shouldNotSaveWhenBatchIsNullOrNoLogs() {
    delegateSelectionLogsService.save(null);
    delegateSelectionLogsService.save(BatchDelegateSelectionLog.builder().build());

    verify(featureFlagService, never()).isNotEnabled(eq(FeatureName.DISABLE_DELEGATE_SELECTION_LOG), anyString());
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void shouldNotSaveWhenFFDisabled() {
    DelegateSelectionLog selectionLog = createDelegateSelectionLogBuilder().uuid(generateUuid()).build();
    BatchDelegateSelectionLog batch =
        BatchDelegateSelectionLog.builder().delegateSelectionLogs(Arrays.asList(selectionLog)).build();
    when(featureFlagService.isEnabled(FeatureName.DISABLE_DELEGATE_SELECTION_LOG, selectionLog.getAccountId()))
        .thenReturn(true);

    delegateSelectionLogsService.save(batch);

    assertThat(wingsPersistence.get(DelegateSelectionLog.class, selectionLog.getUuid())).isNull();
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void shouldSaveWhenFFEnabled() {
    DelegateSelectionLog selectionLog =
        createDelegateSelectionLogBuilder().uuid(generateUuid()).message("ffenabled").groupId(generateUuid()).build();
    BatchDelegateSelectionLog batch =
        BatchDelegateSelectionLog.builder().delegateSelectionLogs(Arrays.asList(selectionLog)).build();
    when(featureFlagService.isNotEnabled(FeatureName.DISABLE_DELEGATE_SELECTION_LOG, selectionLog.getAccountId()))
        .thenReturn(true);

    delegateSelectionLogsService.save(batch);

    assertThat(wingsPersistence.get(DelegateSelectionLog.class, selectionLog.getUuid())).isNotNull();
  }

  @Test
  @Owner(developers = VUK)
  @Category(UnitTests.class)
  public void shouldNotSave_OnlyLogWhenFFEnabled() {
    DelegateSelectionLog selectionLog =
        createDelegateSelectionLogBuilder().uuid(generateUuid()).message("testMessage").groupId(generateUuid()).build();
    BatchDelegateSelectionLog batch =
        BatchDelegateSelectionLog.builder().delegateSelectionLogs(Arrays.asList(selectionLog)).build();
    when(featureFlagService.isEnabled(FeatureName.DISABLE_DELEGATE_SELECTION_LOG, selectionLog.getAccountId()))
        .thenReturn(true);

    delegateSelectionLogsService.save(batch);

    assertThat(wingsPersistence.get(DelegateSelectionLog.class, selectionLog.getUuid())).isNull();
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void shouldSaveWithoutDuplicates() {
    String taskId = generateUuid();
    String accountId = generateUuid();

    wingsPersistence.ensureIndexForTesting(DelegateSelectionLog.class);
    when(featureFlagService.isNotEnabled(FeatureName.DISABLE_DELEGATE_SELECTION_LOG, accountId)).thenReturn(true);

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
        .isEqualTo(1L);
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
    DelegateTask task = DelegateTask.builder().uuid(generateUuid()).selectionLogsTrackingEnabled(true).build();
    BatchDelegateSelectionLog batchDelegateSelectionLog = delegateSelectionLogsService.createBatch(task);

    assertThat(batchDelegateSelectionLog).isNotNull();
    assertThat(batchDelegateSelectionLog.getDelegateSelectionLogs()).isNotNull();
    assertThat(batchDelegateSelectionLog.getDelegateSelectionLogs()).isEmpty();
    assertThat(batchDelegateSelectionLog.getTaskId()).isEqualTo(task.getUuid());
  }

  @Test
  @Owner(developers = VUK)
  @Category(UnitTests.class)
  public void shouldNotLogCanAssign() {
    assertThatCode(() -> delegateSelectionLogsService.logNoIncludeScopeMatched(null, null, null))
        .doesNotThrowAnyException();
  }

  @Test
  @Owner(developers = VUK)
  @Category(UnitTests.class)
  public void shouldLogCanAssign() {
    String taskId = generateUuid();
    String accountId = generateUuid();
    String delegate1Id = generateUuid();

    BatchDelegateSelectionLog batch = BatchDelegateSelectionLog.builder().taskId(taskId).build();

    delegateSelectionLogsService.logCanAssign(batch, accountId, delegate1Id);

    assertThat(batch.getDelegateSelectionLogs()).isNotEmpty();
    assertThat(batch.getDelegateSelectionLogs().get(0).getDelegateIds().size()).isEqualTo(1);
    assertThat(batch.getDelegateSelectionLogs().get(0).getAccountId()).isEqualTo(accountId);
    assertThat(batch.getDelegateSelectionLogs().get(0).getTaskId()).isEqualTo(taskId);
    assertThat(batch.getDelegateSelectionLogs().get(0).getConclusion()).isEqualTo(ACCEPTED);
    assertThat(batch.getDelegateSelectionLogs().get(0).getMessage())
        .isEqualTo("Successfully matched required delegate capabilities");
    assertThat(batch.getDelegateSelectionLogs().get(0).getEventTimestamp()).isNotNull();
    assertThat(batch.getDelegateSelectionLogs().get(0).getGroupId()).isEqualTo(CAN_ASSIGN_GROUP_ID);
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void shouldNotLogTaskAssigned() {
    assertThatCode(() -> delegateSelectionLogsService.logTaskAssigned(null, null, null)).doesNotThrowAnyException();
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
    assertThat(batch.getDelegateSelectionLogs().get(0).getGroupId()).isEqualTo(TASK_ASSIGNED_GROUP_ID);
  }

  @Test
  @Owner(developers = VUK)
  @Category(UnitTests.class)
  public void shouldNotLogNoIncludeScopeMatched() {
    assertThatCode(() -> delegateSelectionLogsService.logNoIncludeScopeMatched(null, null, null))
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
    assertThat(batch.getDelegateSelectionLogs().get(0).getMessage()).isEqualTo("No matching include scope");
    assertThat(batch.getDelegateSelectionLogs().get(0).getEventTimestamp()).isNotNull();
    assertThat(batch.getDelegateSelectionLogs().get(0).getGroupId()).isEqualTo(NO_INCLUDE_SCOPE_MATCHED_GROUP_ID);

    delegateSelectionLogsService.logNoIncludeScopeMatched(batch, accountId, delegate2Id);

    assertThat(batch.getDelegateSelectionLogs()).isNotEmpty();
    assertThat(batch.getDelegateSelectionLogs().get(0).getDelegateIds().size()).isEqualTo(2);
    assertThat(batch.getDelegateSelectionLogs().get(0).getAccountId()).isEqualTo(accountId);
    assertThat(batch.getDelegateSelectionLogs().get(0).getTaskId()).isEqualTo(taskId);
    assertThat(batch.getDelegateSelectionLogs().get(0).getConclusion()).isEqualTo(REJECTED);
    assertThat(batch.getDelegateSelectionLogs().get(0).getMessage()).isEqualTo("No matching include scope");
    assertThat(batch.getDelegateSelectionLogs().get(0).getEventTimestamp()).isNotNull();
    assertThat(batch.getDelegateSelectionLogs().get(0).getGroupId()).isEqualTo(NO_INCLUDE_SCOPE_MATCHED_GROUP_ID);
  }

  @Test
  @Owner(developers = VUK)
  @Category(UnitTests.class)
  public void shouldNotLogExcludeScopeMatched() {
    assertThatCode(() -> delegateSelectionLogsService.logExcludeScopeMatched(null, null, null, null))
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
    assertThat(batch.getDelegateSelectionLogs().get(0).getMessage()).isEqualTo("Matched exclude scope testScope");
    assertThat(batch.getDelegateSelectionLogs().get(0).getEventTimestamp()).isNotNull();
    assertThat(batch.getDelegateSelectionLogs().get(0).getGroupId()).isEqualTo(EXCLUDE_SCOPE_MATCHED_GROUP_ID);

    delegateSelectionLogsService.logExcludeScopeMatched(batch, accountId, delegate2Id, "testScope");

    assertThat(batch.getDelegateSelectionLogs()).isNotEmpty();
    assertThat(batch.getDelegateSelectionLogs().get(0).getDelegateIds().size()).isEqualTo(2);
    assertThat(batch.getDelegateSelectionLogs().get(0).getAccountId()).isEqualTo(accountId);
    assertThat(batch.getDelegateSelectionLogs().get(0).getTaskId()).isEqualTo(taskId);
    assertThat(batch.getDelegateSelectionLogs().get(0).getConclusion()).isEqualTo(REJECTED);
    assertThat(batch.getDelegateSelectionLogs().get(0).getMessage()).isEqualTo("Matched exclude scope testScope");
    assertThat(batch.getDelegateSelectionLogs().get(0).getEventTimestamp()).isNotNull();
    assertThat(batch.getDelegateSelectionLogs().get(0).getGroupId()).isEqualTo(EXCLUDE_SCOPE_MATCHED_GROUP_ID);
  }

  @Test
  @Owner(developers = SANJA)
  @Category(UnitTests.class)
  public void shouldNotLogProfileScopeRuleNotMatched() {
    assertThatCode(() -> delegateSelectionLogsService.logProfileScopeRuleNotMatched(null, null, null, null))
        .doesNotThrowAnyException();
  }

  @Test
  @Owner(developers = SANJA)
  @Category(UnitTests.class)
  public void shouldLogProfileScopeMatched() {
    String taskId = generateUuid();
    String accountId = generateUuid();
    String delegate1Id = generateUuid();
    String delegate2Id = generateUuid();
    String scopingRuleDescription = "rule description";

    BatchDelegateSelectionLog batch = BatchDelegateSelectionLog.builder().taskId(taskId).build();

    delegateSelectionLogsService.logProfileScopeRuleNotMatched(batch, accountId, delegate1Id, scopingRuleDescription);

    assertThat(batch.getDelegateSelectionLogs()).isNotEmpty();
    assertThat(batch.getDelegateSelectionLogs().get(0).getDelegateIds().size()).isEqualTo(1);
    assertThat(batch.getDelegateSelectionLogs().get(0).getAccountId()).isEqualTo(accountId);
    assertThat(batch.getDelegateSelectionLogs().get(0).getTaskId()).isEqualTo(taskId);
    assertThat(batch.getDelegateSelectionLogs().get(0).getConclusion()).isEqualTo(REJECTED);
    assertThat(batch.getDelegateSelectionLogs().get(0).getMessage())
        .isEqualTo("Delegate profile scoping rule not matched: rule description");
    assertThat(batch.getDelegateSelectionLogs().get(0).getEventTimestamp()).isNotNull();
    assertThat(batch.getDelegateSelectionLogs().get(0).getGroupId()).isEqualTo(PROFILE_SCOPE_RULE_NOT_MATCHED_GROUP_ID);

    delegateSelectionLogsService.logProfileScopeRuleNotMatched(batch, accountId, delegate2Id, scopingRuleDescription);

    assertThat(batch.getDelegateSelectionLogs()).isNotEmpty();
    assertThat(batch.getDelegateSelectionLogs().get(0).getDelegateIds().size()).isEqualTo(2);
    assertThat(batch.getDelegateSelectionLogs().get(0).getAccountId()).isEqualTo(accountId);
    assertThat(batch.getDelegateSelectionLogs().get(0).getTaskId()).isEqualTo(taskId);
    assertThat(batch.getDelegateSelectionLogs().get(0).getConclusion()).isEqualTo(REJECTED);
    assertThat(batch.getDelegateSelectionLogs().get(0).getMessage())
        .isEqualTo("Delegate profile scoping rule not matched: rule description");
    assertThat(batch.getDelegateSelectionLogs().get(0).getEventTimestamp()).isNotNull();
    assertThat(batch.getDelegateSelectionLogs().get(0).getGroupId()).isEqualTo(PROFILE_SCOPE_RULE_NOT_MATCHED_GROUP_ID);
  }

  @Test
  @Owner(developers = VUK)
  @Category(UnitTests.class)
  public void shouldNotLogMissingAllSelectors() {
    DelegateSelectionLogsServiceImpl delegateSelectionLogsService = new DelegateSelectionLogsServiceImpl();

    assertThatCode(() -> delegateSelectionLogsService.logMissingAllSelectors(null, null, null))
        .doesNotThrowAnyException();
  }

  @Test
  @Owner(developers = VUK)
  @Category(UnitTests.class)
  public void shouldLogMissingAllSelectors() {
    String taskId = generateUuid();
    String accountId = generateUuid();
    String delegate1Id = generateUuid();
    String delegate2Id = generateUuid();

    BatchDelegateSelectionLog batch = BatchDelegateSelectionLog.builder().taskId(taskId).build();

    delegateSelectionLogsService.logMissingAllSelectors(batch, accountId, delegate1Id);

    assertThat(batch.getDelegateSelectionLogs()).isNotEmpty();
    assertThat(batch.getDelegateSelectionLogs().get(0).getDelegateIds().size()).isEqualTo(1);
    assertThat(batch.getDelegateSelectionLogs().get(0).getAccountId()).isEqualTo(accountId);
    assertThat(batch.getDelegateSelectionLogs().get(0).getTaskId()).isEqualTo(taskId);
    assertThat(batch.getDelegateSelectionLogs().get(0).getConclusion()).isEqualTo(REJECTED);
    assertThat(batch.getDelegateSelectionLogs().get(0).getMessage()).isEqualTo("Missing all selectors");
    assertThat(batch.getDelegateSelectionLogs().get(0).getEventTimestamp()).isNotNull();
    assertThat(batch.getDelegateSelectionLogs().get(0).getGroupId()).isEqualTo(MISSING_ALL_SELECTORS_GROUP_ID);

    delegateSelectionLogsService.logMissingAllSelectors(batch, accountId, delegate2Id);

    assertThat(batch.getDelegateSelectionLogs()).isNotEmpty();
    assertThat(batch.getDelegateSelectionLogs().get(0).getDelegateIds().size()).isEqualTo(2);
    assertThat(batch.getDelegateSelectionLogs().get(0).getAccountId()).isEqualTo(accountId);
    assertThat(batch.getDelegateSelectionLogs().get(0).getTaskId()).isEqualTo(taskId);
    assertThat(batch.getDelegateSelectionLogs().get(0).getConclusion()).isEqualTo(REJECTED);
    assertThat(batch.getDelegateSelectionLogs().get(0).getMessage()).isEqualTo("Missing all selectors");
    assertThat(batch.getDelegateSelectionLogs().get(0).getEventTimestamp()).isNotNull();
    assertThat(batch.getDelegateSelectionLogs().get(0).getGroupId()).isEqualTo(MISSING_ALL_SELECTORS_GROUP_ID);
  }

  @Test
  @Owner(developers = VUK)
  @Category(UnitTests.class)
  public void shouldNotLogMissingSelector() {
    assertThatCode(() -> delegateSelectionLogsService.logMissingSelector(null, null, null, null, null))
        .doesNotThrowAnyException();
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

    delegateSelectionLogsService.logMissingSelector(batch, accountId, delegate1Id, selector, selectorOrigin);

    assertThat(batch.getDelegateSelectionLogs()).isNotEmpty();
    assertThat(batch.getDelegateSelectionLogs().get(0).getDelegateIds().size()).isEqualTo(1);
    assertThat(batch.getDelegateSelectionLogs().get(0).getAccountId()).isEqualTo(accountId);
    assertThat(batch.getDelegateSelectionLogs().get(0).getTaskId()).isEqualTo(taskId);
    assertThat(batch.getDelegateSelectionLogs().get(0).getConclusion()).isEqualTo(REJECTED);
    assertThat(batch.getDelegateSelectionLogs().get(0).getMessage())
        .isEqualTo("The selector " + selector + " is configured in " + selectorOrigin
            + ", but is not attached to this Delegate.");
    assertThat(batch.getDelegateSelectionLogs().get(0).getEventTimestamp()).isNotNull();
    assertThat(batch.getDelegateSelectionLogs().get(0).getGroupId()).isEqualTo(MISSING_SELECTOR_GROUP_ID);

    delegateSelectionLogsService.logMissingSelector(batch, accountId, delegate2Id, selector, selectorOrigin);

    assertThat(batch.getDelegateSelectionLogs()).isNotEmpty();
    assertThat(batch.getDelegateSelectionLogs().get(0).getDelegateIds().size()).isEqualTo(2);
    assertThat(batch.getDelegateSelectionLogs().get(0).getAccountId()).isEqualTo(accountId);
    assertThat(batch.getDelegateSelectionLogs().get(0).getTaskId()).isEqualTo(taskId);
    assertThat(batch.getDelegateSelectionLogs().get(0).getConclusion()).isEqualTo(REJECTED);
    assertThat(batch.getDelegateSelectionLogs().get(0).getMessage())
        .isEqualTo("The selector " + selector + " is configured in " + selectorOrigin
            + ", but is not attached to this Delegate.");
    assertThat(batch.getDelegateSelectionLogs().get(0).getEventTimestamp()).isNotNull();
    assertThat(batch.getDelegateSelectionLogs().get(0).getGroupId()).isEqualTo(MISSING_SELECTOR_GROUP_ID);
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
    assertThat(batch.getDelegateSelectionLogs().get(0).getConclusion()).isEqualTo(DISCONNECTED);
    assertThat(batch.getDelegateSelectionLogs().get(0).getMessage()).isEqualTo("Delegate was disconnected");
    assertThat(batch.getDelegateSelectionLogs().get(0).getEventTimestamp()).isNotNull();
    assertThat(batch.getDelegateSelectionLogs().get(0).getGroupId()).isEqualTo(DISCONNECTED_GROUP_ID);
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void shouldLogWaitingForApprovalDelegate() {
    String taskId = generateUuid();
    String accountId = generateUuid();
    String delegate1Id = generateUuid();
    String delegate2Id = generateUuid();
    Set<String> wapprDelegates = new HashSet<>();
    wapprDelegates.add(delegate1Id);
    wapprDelegates.add(delegate2Id);

    BatchDelegateSelectionLog batch = BatchDelegateSelectionLog.builder().taskId(taskId).build();

    delegateSelectionLogsService.logWaitingForApprovalDelegate(batch, accountId, wapprDelegates);

    assertThat(batch.getDelegateSelectionLogs()).isNotEmpty();
    assertThat(batch.getDelegateSelectionLogs().get(0).getDelegateIds().size()).isEqualTo(2);
    assertThat(
        batch.getDelegateSelectionLogs().get(0).getDelegateIds().containsAll(Arrays.asList(delegate1Id, delegate2Id)))
        .isTrue();
    assertThat(batch.getDelegateSelectionLogs().get(0).getAccountId()).isEqualTo(accountId);
    assertThat(batch.getDelegateSelectionLogs().get(0).getTaskId()).isEqualTo(taskId);
    assertThat(batch.getDelegateSelectionLogs().get(0).getConclusion()).isEqualTo(WAITING_FOR_APPROVAL);
    assertThat(batch.getDelegateSelectionLogs().get(0).getMessage()).isEqualTo("Delegate was waiting for approval");
    assertThat(batch.getDelegateSelectionLogs().get(0).getEventTimestamp()).isNotNull();
    assertThat(batch.getDelegateSelectionLogs().get(0).getGroupId()).isEqualTo(WAITING_ON_APPROVAL_GROUP_ID);
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
                            .delegateName("name")
                            .hostName("hostname")
                            .delegateProfileId(delegateProfile.getUuid())
                            .build();

    Set<String> delegateIds = new HashSet<>();
    delegateIds.add(delegate.getUuid());

    DelegateSelectionLog delegateSelectionLog = DelegateSelectionLog.builder()
                                                    .taskId(taskId)
                                                    .accountId(accountId)
                                                    .message("message")
                                                    .conclusion(SELECTED)
                                                    .delegateIds(delegateIds)
                                                    .eventTimestamp(System.currentTimeMillis())
                                                    .build();

    wingsPersistence.save(delegateProfile);
    wingsPersistence.save(delegate);
    wingsPersistence.save(delegateSelectionLog);

    List<DelegateSelectionLogParams> delegateSelectionLogParams =
        delegateSelectionLogsService.fetchTaskSelectionLogs(accountId, taskId);

    assertThat(delegateSelectionLogParams.size()).isEqualTo(1);
    assertThat(delegateSelectionLogParams.get(0).getDelegateId()).isEqualTo(delegate.getUuid());
    assertThat(delegateSelectionLogParams.get(0).getDelegateName()).isEqualTo(delegate.getDelegateName());
    assertThat(delegateSelectionLogParams.get(0).getDelegateHostName()).isEqualTo(delegate.getHostName());
    assertThat(delegateSelectionLogParams.get(0).getDelegateProfileName()).isEqualTo(delegateProfile.getName());
    assertThat(delegateSelectionLogParams.get(0).getConclusion()).isEqualTo(delegateSelectionLog.getConclusion());
    assertThat(delegateSelectionLogParams.get(0).getMessage()).isEqualTo(delegateSelectionLog.getMessage());
    assertThat(delegateSelectionLogParams.get(0).getEventTimestamp())
        .isEqualTo(delegateSelectionLog.getEventTimestamp());
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

    Delegate delegate = Delegate.builder()
                            .accountId(accountId)
                            .uuid(generateUuid())
                            .delegateName("name")
                            .hostName("hostname")
                            .delegateProfileId(generateUuid())
                            .build();

    Set<String> delegateIds = new HashSet<>();
    delegateIds.add(delegate.getUuid());

    DelegateSelectionLog delegateSelectionLog = DelegateSelectionLog.builder()
                                                    .taskId(taskId)
                                                    .accountId(accountId)
                                                    .message("message")
                                                    .conclusion(SELECTED)
                                                    .delegateIds(delegateIds)
                                                    .eventTimestamp(System.currentTimeMillis())
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
  @Owner(developers = VUK)
  @Category(UnitTests.class)
  public void shouldLogDisconnectedScalingGroup() {
    String taskId = generateUuid();
    String accountId = generateUuid();
    String delegate1 = generateUuid();
    String delegate2 = generateUuid();
    String groupName = generateUuid();

    Set<String> disconnectedScalingGroup = new HashSet<>();
    disconnectedScalingGroup.add(delegate1);
    disconnectedScalingGroup.add(delegate2);

    BatchDelegateSelectionLog batch = BatchDelegateSelectionLog.builder().taskId(taskId).build();

    delegateSelectionLogsService.logDisconnectedScalingGroup(batch, accountId, disconnectedScalingGroup, groupName);

    assertThat(batch.getDelegateSelectionLogs()).isNotEmpty();
    assertThat(batch.getDelegateSelectionLogs().get(0).getDelegateIds().size()).isEqualTo(2);
    assertThat(
        batch.getDelegateSelectionLogs().get(0).getDelegateIds().containsAll(Arrays.asList(delegate1, delegate2)))
        .isTrue();
    assertThat(batch.getDelegateSelectionLogs().get(0).getAccountId()).isEqualTo(accountId);
    assertThat(batch.getDelegateSelectionLogs().get(0).getTaskId()).isEqualTo(taskId);
    assertThat(batch.getDelegateSelectionLogs().get(0).getConclusion()).isEqualTo(DISCONNECTED);
    assertThat(batch.getDelegateSelectionLogs().get(0).getMessage())
        .isEqualTo("Delegate scaling group: " + groupName + " was disconnected");
    assertThat(batch.getDelegateSelectionLogs().get(0).getGroupId()).isEqualTo(DISCONNECTED_GROUP_ID);
  }

  private DelegateSelectionLogBuilder createDelegateSelectionLogBuilder() {
    return DelegateSelectionLog.builder().accountId(generateUuid()).taskId(generateUuid());
  }

  private List<DelegateSelectionLog> createDelegateSelectionLogs(String taskId, String accountId) {
    DelegateSelectionLog selectionLog1 = createDelegateSelectionLogBuilder()
                                             .taskId(taskId)
                                             .accountId(accountId)
                                             .message(MISSING_SELECTOR_MESSAGE)
                                             .groupId(MISSING_SELECTOR_GROUP_ID)
                                             .build();

    DelegateSelectionLog selectionLog2 = createDelegateSelectionLogBuilder()
                                             .taskId(taskId)
                                             .accountId(accountId)
                                             .message(MISSING_SELECTOR_MESSAGE)
                                             .groupId(MISSING_SELECTOR_GROUP_ID)
                                             .build();

    return Arrays.asList(selectionLog1, selectionLog2);
  }
}
