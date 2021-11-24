package software.wings.service.impl;

import static io.harness.beans.EnvironmentType.PROD;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.ARVIND;
import static io.harness.rule.OwnerRule.MARKO;
import static io.harness.rule.OwnerRule.SANJA;
import static io.harness.rule.OwnerRule.VUK;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

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
  private static final String TARGETED_DELEGATE_MATCHED_GROUP_ID = "TARGETED_DELEGATE_MATCHED_GROUP_ID";
  private static final String TARGETED_DELEGATE_NOT_MATCHED_GROUP_ID = "TARGETED_DELEGATE_NOT_MATCHED_GROUP_ID";

  @Inject protected WingsPersistence wingsPersistence;
  @Mock protected FeatureFlagService featureFlagService;
  @InjectMocks @Inject DelegateSelectionLogsServiceImpl delegateSelectionLogsService;

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

    DelegateSelectionLogTaskMetadata taskMetadata = DelegateSelectionLogTaskMetadata.builder()
                                                        .uuid(generateUuid())
                                                        .taskId(generateUuid())
                                                        .accountId(generateUuid())
                                                        .setupAbstractions(obtainTaskSetupAbstractions())
                                                        .build();

    BatchDelegateSelectionLog batch = BatchDelegateSelectionLog.builder()
                                          .delegateSelectionLogs(Arrays.asList(selectionLog))
                                          .taskMetadata(taskMetadata)
                                          .build();

    delegateSelectionLogsService.save(batch);

    assertThat(wingsPersistence.get(DelegateSelectionLog.class, selectionLog.getUuid())).isNotNull();

    DelegateSelectionLogTaskMetadata savedTaskMetadata =
        wingsPersistence.get(DelegateSelectionLogTaskMetadata.class, taskMetadata.getUuid());

    assertThat(savedTaskMetadata).isNotNull();
    assertThat(savedTaskMetadata.getSetupAbstractions()).isNotNull();
    assertThat(savedTaskMetadata.getSetupAbstractions().get(Cd1SetupFields.APPLICATION))
        .isEqualTo(taskMetadata.getSetupAbstractions().get(Cd1SetupFields.APPLICATION));
    assertThat(savedTaskMetadata.getSetupAbstractions().get(Cd1SetupFields.SERVICE))
        .isEqualTo(taskMetadata.getSetupAbstractions().get(Cd1SetupFields.SERVICE));
    assertThat(savedTaskMetadata.getSetupAbstractions().get(Cd1SetupFields.ENVIRONMENT))
        .isEqualTo(taskMetadata.getSetupAbstractions().get(Cd1SetupFields.ENVIRONMENT));
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void shouldSaveWithoutDuplicates() {
    String taskId = generateUuid();
    String accountId = generateUuid();

    wingsPersistence.ensureIndexForTesting(DelegateSelectionLog.class);
    wingsPersistence.ensureIndexForTesting(DelegateSelectionLogTaskMetadata.class);

    DelegateSelectionLogTaskMetadata taskMetadata = DelegateSelectionLogTaskMetadata.builder()
                                                        .taskId(taskId)
                                                        .accountId(accountId)
                                                        .setupAbstractions(obtainTaskSetupAbstractions())
                                                        .build();

    Concurrent.test(10, n -> {
      BatchDelegateSelectionLog batch = BatchDelegateSelectionLog.builder()
                                            .delegateSelectionLogs(createDelegateSelectionLogs(taskId, accountId))
                                            .taskMetadata(taskMetadata)
                                            .build();
      delegateSelectionLogsService.save(batch);
    });

    assertThat(wingsPersistence.createQuery(DelegateSelectionLog.class)
                   .filter(DelegateSelectionLogKeys.taskId, taskId)
                   .filter(DelegateSelectionLogKeys.accountId, accountId)
                   .count())
        .isEqualTo(1L);

    assertThat(wingsPersistence.createQuery(DelegateSelectionLogTaskMetadata.class)
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
    assertThat(batch.getDelegateSelectionLogs().get(0).getGroupId()).isEqualTo(TASK_ASSIGNED_GROUP_ID);
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
    assertThat(batch.getDelegateSelectionLogs().get(0).getGroupId()).isEqualTo(PROFILE_SCOPE_RULE_NOT_MATCHED_GROUP_ID);
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
    assertThat(batch.getDelegateSelectionLogs().get(0).getGroupId()).isEqualTo(PROFILE_SCOPE_RULE_NOT_MATCHED_GROUP_ID);
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
    assertThat(batch.getDelegateSelectionLogs().get(0).getDelegateMetadata()).isNotEmpty();
    assertThat(batch.getDelegateSelectionLogs().get(0).getDelegateMetadata().size()).isEqualTo(2);
  }

  @Test
  @Owner(developers = VUK)
  @Category(UnitTests.class)
  public void shouldNotLogMissingAllSelectors() {
    DelegateSelectionLogsServiceImpl delegateSelectionLogsService = new DelegateSelectionLogsServiceImpl();

    assertThatCode(() -> delegateSelectionLogsService.logMissingAllSelectors(null, null, ""))
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
    assertThatCode(() -> delegateSelectionLogsService.logMissingSelector(null, null, "", null, null))
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
    assertThat(batch.getDelegateSelectionLogs().get(0).getGroupId()).isEqualTo(TARGETED_DELEGATE_MATCHED_GROUP_ID);
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
    assertThat(batch.getDelegateSelectionLogs().get(0).getGroupId()).isEqualTo(TARGETED_DELEGATE_NOT_MATCHED_GROUP_ID);
  }

  @Test
  @Owner(developers = ARVIND)
  @Category(UnitTests.class)
  public void shouldNotLogOwnerRuleNotMatched() {
    assertThatCode(() -> delegateSelectionLogsService.logOwnerRuleNotMatched(null, null, "", null))
        .doesNotThrowAnyException();
    assertThatCode(()
                       -> delegateSelectionLogsService.logOwnerRuleNotMatched(
                           BatchDelegateSelectionLog.builder().build(), null, "", null))
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

    delegateSelectionLogsService.logOwnerRuleNotMatched(batch, accountId, delegateId, owner);
    assertThat(batch.getDelegateSelectionLogs()).hasSize(1);
    assertThat(batch.getDelegateSelectionLogs().get(0).getMessage()).isEqualTo("No matching owner: orgId/projectId");
    assertThat(batch.getDelegateSelectionLogs().get(0).getConclusion()).isEqualTo(REJECTED);
    assertThat(batch.getDelegateSelectionLogs().get(0).getGroupId()).isEqualTo("TARGETED_OWNER_MATCHED_GROUP_ID");
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
