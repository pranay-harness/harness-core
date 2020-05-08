package software.wings.service.impl;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.MARKO;
import static io.harness.rule.OwnerRule.VUK;
import static junit.framework.TestCase.assertEquals;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.DelegateSelectionLogParams;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import software.wings.WingsBaseTest;
import software.wings.beans.BatchDelegateSelectionLog;
import software.wings.beans.DelegateScope;
import software.wings.beans.DelegateSelectionLog;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.DelegateSelectionLogsService;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DelegateSelectionLogsServiceImplTest extends WingsBaseTest {
  private static final String WAITING_FOR_APPROVAL = "Waiting for Approval";
  private static final String DISCONNECTED = "Disconnected";
  private static final String REJECTED = "Rejected";
  private static final String SELECTED = "Selected";

  @Inject protected WingsPersistence wingsPersistence;
  @InjectMocks @Inject DelegateSelectionLogsService delegateSelectionLogsService;

  @Test
  @Owner(developers = VUK)
  @Category(UnitTests.class)
  public void shouldNotCreateBatch() {
    BatchDelegateSelectionLog result = delegateSelectionLogsService.createBatch(null);

    assertThat(result).isNull();
  }

  @Test
  @Owner(developers = VUK)
  @Category(UnitTests.class)
  public void shouldCreateBatch() {
    String taskId = generateUuid();
    BatchDelegateSelectionLog batchDelegateSelectionLog = delegateSelectionLogsService.createBatch(taskId);

    assertThat(batchDelegateSelectionLog).isNotNull();
    assertThat(batchDelegateSelectionLog.getDelegateSelectionLogs()).isNotNull();
    assertThat(batchDelegateSelectionLog.getDelegateSelectionLogs()).isEmpty();
    assertEquals(batchDelegateSelectionLog.getTaskId(), taskId);
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
    assertThat(batch.getDelegateSelectionLogs().get(0).getConclusion()).isEqualTo(SELECTED);
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

    delegateSelectionLogsService.logNoIncludeScopeMatched(batch, accountId, delegate2Id);

    assertThat(batch.getDelegateSelectionLogs()).isNotEmpty();
    assertThat(batch.getDelegateSelectionLogs().get(0).getDelegateIds().size()).isEqualTo(2);
    assertThat(batch.getDelegateSelectionLogs().get(0).getAccountId()).isEqualTo(accountId);
    assertThat(batch.getDelegateSelectionLogs().get(0).getTaskId()).isEqualTo(taskId);
    assertThat(batch.getDelegateSelectionLogs().get(0).getConclusion()).isEqualTo(REJECTED);
    assertThat(batch.getDelegateSelectionLogs().get(0).getMessage()).isEqualTo("No matching include scope");
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

    DelegateScope scope = DelegateScope.builder().build();
    BatchDelegateSelectionLog batch = BatchDelegateSelectionLog.builder().taskId(taskId).build();

    delegateSelectionLogsService.logExcludeScopeMatched(batch, accountId, delegate1Id, scope);

    assertThat(batch.getDelegateSelectionLogs()).isNotEmpty();
    assertThat(batch.getDelegateSelectionLogs().get(0).getDelegateIds().size()).isEqualTo(1);
    assertThat(batch.getDelegateSelectionLogs().get(0).getAccountId()).isEqualTo(accountId);
    assertThat(batch.getDelegateSelectionLogs().get(0).getTaskId()).isEqualTo(taskId);
    assertThat(batch.getDelegateSelectionLogs().get(0).getConclusion()).isEqualTo(REJECTED);
    assertThat(batch.getDelegateSelectionLogs().get(0).getMessage())
        .isEqualTo("Matched exclude scope " + scope.getName());

    delegateSelectionLogsService.logExcludeScopeMatched(batch, accountId, delegate2Id, scope);

    assertThat(batch.getDelegateSelectionLogs()).isNotEmpty();
    assertThat(batch.getDelegateSelectionLogs().get(0).getDelegateIds().size()).isEqualTo(2);
    assertThat(batch.getDelegateSelectionLogs().get(0).getAccountId()).isEqualTo(accountId);
    assertThat(batch.getDelegateSelectionLogs().get(0).getTaskId()).isEqualTo(taskId);
    assertThat(batch.getDelegateSelectionLogs().get(0).getConclusion()).isEqualTo(REJECTED);
    assertThat(batch.getDelegateSelectionLogs().get(0).getMessage())
        .isEqualTo("Matched exclude scope " + scope.getName());
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

    delegateSelectionLogsService.logMissingAllSelectors(batch, accountId, delegate2Id);

    assertThat(batch.getDelegateSelectionLogs()).isNotEmpty();
    assertThat(batch.getDelegateSelectionLogs().get(0).getDelegateIds().size()).isEqualTo(2);
    assertThat(batch.getDelegateSelectionLogs().get(0).getAccountId()).isEqualTo(accountId);
    assertThat(batch.getDelegateSelectionLogs().get(0).getTaskId()).isEqualTo(taskId);
    assertThat(batch.getDelegateSelectionLogs().get(0).getConclusion()).isEqualTo(REJECTED);
    assertThat(batch.getDelegateSelectionLogs().get(0).getMessage()).isEqualTo("Missing all selectors");
  }

  @Test
  @Owner(developers = VUK)
  @Category(UnitTests.class)
  public void shouldNotLogMissingSelector() {
    assertThatCode(() -> delegateSelectionLogsService.logMissingSelector(null, null, null, null))
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

    BatchDelegateSelectionLog batch = BatchDelegateSelectionLog.builder().taskId(taskId).build();

    delegateSelectionLogsService.logMissingSelector(batch, accountId, delegate1Id, selector);

    assertThat(batch.getDelegateSelectionLogs()).isNotEmpty();
    assertThat(batch.getDelegateSelectionLogs().get(0).getDelegateIds().size()).isEqualTo(1);
    assertThat(batch.getDelegateSelectionLogs().get(0).getAccountId()).isEqualTo(accountId);
    assertThat(batch.getDelegateSelectionLogs().get(0).getTaskId()).isEqualTo(taskId);
    assertThat(batch.getDelegateSelectionLogs().get(0).getConclusion()).isEqualTo(REJECTED);
    assertThat(batch.getDelegateSelectionLogs().get(0).getMessage()).isEqualTo("Missing selector " + selector);

    delegateSelectionLogsService.logMissingSelector(batch, accountId, delegate2Id, selector);

    assertThat(batch.getDelegateSelectionLogs()).isNotEmpty();
    assertThat(batch.getDelegateSelectionLogs().get(0).getDelegateIds().size()).isEqualTo(2);
    assertThat(batch.getDelegateSelectionLogs().get(0).getAccountId()).isEqualTo(accountId);
    assertThat(batch.getDelegateSelectionLogs().get(0).getTaskId()).isEqualTo(taskId);
    assertThat(batch.getDelegateSelectionLogs().get(0).getConclusion()).isEqualTo(REJECTED);
    assertThat(batch.getDelegateSelectionLogs().get(0).getMessage()).isEqualTo("Missing selector " + selector);
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
    assertThat(batch.getDelegateSelectionLogs().get(0).getMessage().startsWith("Delegate was disconnected at"))
        .isTrue();
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
    assertThat(batch.getDelegateSelectionLogs().get(0).getMessage().startsWith("Delegate was waiting for approval at"))
        .isTrue();
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
                                                     .build();

    DelegateSelectionLog delegateSelectionLog2 = DelegateSelectionLog.builder()
                                                     .taskId(taskId)
                                                     .accountId(accountId)
                                                     .message(message)
                                                     .delegateIds(delegateIds2)
                                                     .build();
    wingsPersistence.save(delegateSelectionLog1);
    wingsPersistence.save(delegateSelectionLog2);

    List<DelegateSelectionLogParams> delegateSelectionLogs =
        delegateSelectionLogsService.fetchTaskSelectionLogs(accountId, taskId);

    assertThat(delegateSelectionLogs.size()).isEqualTo(3);
  }
}