package software.wings.service.impl;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.delegate.beans.TaskData.DEFAULT_ASYNC_CALL_TIMEOUT;
import static io.harness.rule.OwnerRule.ANSHUL;
import static io.harness.rule.OwnerRule.BRETT;
import static io.harness.rule.OwnerRule.GEORGE;
import static io.harness.rule.OwnerRule.MARKO;
import static io.harness.rule.OwnerRule.PRASHANT;
import static io.harness.rule.OwnerRule.PUNEET;
import static io.harness.rule.OwnerRule.VUK;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.beans.Delegate.Status.ENABLED;
import static software.wings.beans.Environment.Builder.anEnvironment;
import static software.wings.beans.Environment.EnvironmentType.NON_PROD;
import static software.wings.beans.Environment.EnvironmentType.PROD;
import static software.wings.beans.FeatureName.INFRA_MAPPING_REFACTOR;
import static software.wings.beans.GcpKubernetesInfrastructureMapping.Builder.aGcpKubernetesInfrastructureMapping;
import static software.wings.service.impl.AssignDelegateServiceImpl.MAX_DELEGATE_LAST_HEARTBEAT;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.COMPUTE_PROVIDER_SETTING_ID;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.INFRA_MAPPING_ID;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.SERVICE_ID;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.DELEGATE_ID;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.INFRA_DEFINITION_ID;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;

import io.harness.beans.DelegateTask;
import io.harness.beans.DelegateTask.DelegateTaskBuilder;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.DelegateProfile;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.HttpConnectionExecutionCapability;
import io.harness.delegate.beans.executioncapability.SelectorCapability;
import io.harness.delegate.task.http.HttpTaskParameters;
import io.harness.rule.Owner;
import io.harness.tasks.Cd1SetupFields;
import lombok.Builder;
import lombok.Value;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import software.wings.WingsBaseTest;
import software.wings.beans.BatchDelegateSelectionLog;
import software.wings.beans.Delegate;
import software.wings.beans.Delegate.DelegateBuilder;
import software.wings.beans.DelegateScope;
import software.wings.beans.Environment;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.InfrastructureMappingType;
import software.wings.beans.TaskType;
import software.wings.delegatetasks.validation.DelegateConnectionResult;
import software.wings.delegatetasks.validation.DelegateConnectionResult.DelegateConnectionResultKeys;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.instance.InstanceSyncTestConstants;
import software.wings.service.intfc.AssignDelegateService;
import software.wings.service.intfc.DelegateSelectionLogsService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.InfrastructureMappingService;

import java.time.Clock;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by brett on 7/26/17
 */
public class AssignDelegateServiceImplTest extends WingsBaseTest {
  @Mock private EnvironmentService environmentService;
  @Mock private DelegateService delegateService;
  @Mock private InfrastructureMappingService infrastructureMappingService;
  @Mock private FeatureFlagService featureFlagService;
  @Mock private DelegateSelectionLogsService delegateSelectionLogsService;

  @Inject @InjectMocks private AssignDelegateService assignDelegateService;

  @Inject private WingsPersistence wingsPersistence;
  @Inject private Clock clock;

  private static final String WRONG_INFRA_MAPPING_ID = "WRONG_INFRA_MAPPING_ID";

  @Before
  public void setUp() {
    Environment environment = anEnvironment().uuid(ENV_ID).appId(APP_ID).environmentType(PROD).build();
    when(environmentService.get(APP_ID, ENV_ID, false)).thenReturn(environment);
  }

  private DelegateBuilder createDelegateBuilder() {
    return Delegate.builder().status(Delegate.Status.ENABLED).lastHeartBeat(System.currentTimeMillis());
  }

  @Value
  @Builder
  public static class ScopeTestData {
    List<DelegateScope> excludeScopes;
    List<DelegateScope> includeScopes;
    boolean assignable;
    int numOfNoIncludeScopeMatchedInvocations;
    int numOfExcludeScopeMatchedInvocations;
  }

  @Test
  @Owner(developers = VUK)
  @Category(UnitTests.class)
  public void assignByScopes() {
    List<ScopeTestData> tests =
        ImmutableList.<ScopeTestData>builder()
            .add(ScopeTestData.builder()
                     .excludeScopes(emptyList())
                     .includeScopes(emptyList())
                     .assignable(true)
                     .numOfNoIncludeScopeMatchedInvocations(0)
                     .numOfExcludeScopeMatchedInvocations(0)
                     .build())
            .add(ScopeTestData.builder()
                     .excludeScopes(emptyList())
                     .includeScopes(
                         ImmutableList.of(DelegateScope.builder().environmentTypes(ImmutableList.of(PROD)).build()))
                     .assignable(true)
                     .numOfNoIncludeScopeMatchedInvocations(0)
                     .numOfExcludeScopeMatchedInvocations(0)
                     .build())
            .add(ScopeTestData.builder()
                     .excludeScopes(
                         ImmutableList.of(DelegateScope.builder().environmentTypes(ImmutableList.of(NON_PROD)).build()))
                     .includeScopes(
                         ImmutableList.of(DelegateScope.builder().environmentTypes(ImmutableList.of(PROD)).build()))
                     .assignable(true)
                     .numOfNoIncludeScopeMatchedInvocations(0)
                     .numOfExcludeScopeMatchedInvocations(0)
                     .build())
            .add(ScopeTestData.builder()
                     .excludeScopes(
                         ImmutableList.of(DelegateScope.builder().environmentTypes(ImmutableList.of(PROD)).build()))
                     .includeScopes(
                         ImmutableList.of(DelegateScope.builder().environmentTypes(ImmutableList.of(PROD)).build()))
                     .assignable(false)
                     .numOfNoIncludeScopeMatchedInvocations(0)
                     .numOfExcludeScopeMatchedInvocations(1)
                     .build())
            .add(ScopeTestData.builder()
                     .excludeScopes(
                         ImmutableList.of(DelegateScope.builder().environmentTypes(ImmutableList.of(NON_PROD)).build()))
                     .includeScopes(emptyList())
                     .assignable(true)
                     .numOfNoIncludeScopeMatchedInvocations(0)
                     .numOfExcludeScopeMatchedInvocations(1)
                     .build())
            .add(ScopeTestData.builder()
                     .excludeScopes(
                         ImmutableList.of(DelegateScope.builder().environmentTypes(ImmutableList.of(PROD)).build()))
                     .includeScopes(emptyList())
                     .assignable(false)
                     .numOfNoIncludeScopeMatchedInvocations(0)
                     .numOfExcludeScopeMatchedInvocations(2)
                     .build())
            .add(ScopeTestData.builder()
                     .excludeScopes(emptyList())
                     .includeScopes(
                         ImmutableList.of(DelegateScope.builder().environmentTypes(ImmutableList.of(NON_PROD)).build()))
                     .assignable(false)
                     .numOfNoIncludeScopeMatchedInvocations(1)
                     .numOfExcludeScopeMatchedInvocations(2)
                     .build())
            .build();

    DelegateTaskBuilder delegateTaskBuilder =
        DelegateTask.builder()
            .accountId(ACCOUNT_ID)
            .setupAbstraction(Cd1SetupFields.APP_ID_FIELD, APP_ID)
            .setupAbstraction(Cd1SetupFields.ENV_ID_FIELD, ENV_ID)
            .data(TaskData.builder().async(true).timeout(DEFAULT_ASYNC_CALL_TIMEOUT).build());

    DelegateBuilder delegateBuilder = Delegate.builder().accountId(ACCOUNT_ID).uuid(DELEGATE_ID);

    for (ScopeTestData test : tests) {
      Delegate delegate =
          delegateBuilder.includeScopes(test.getIncludeScopes()).excludeScopes(test.getExcludeScopes()).build();
      when(delegateService.get(ACCOUNT_ID, DELEGATE_ID, false)).thenReturn(delegate);

      BatchDelegateSelectionLog batch =
          BatchDelegateSelectionLog.builder().taskId(delegateTaskBuilder.build().getUuid()).build();
      assertThat(assignDelegateService.canAssign(batch, DELEGATE_ID, delegateTaskBuilder.build()))
          .isEqualTo(test.isAssignable());

      verify(delegateSelectionLogsService, Mockito.times(test.getNumOfNoIncludeScopeMatchedInvocations()))
          .logNoIncludeScopeMatched(eq(batch), anyString(), anyString());
      verify(delegateSelectionLogsService, Mockito.times(test.getNumOfExcludeScopeMatchedInvocations()))
          .logExcludeScopeMatched(eq(batch), anyString(), anyString(), any(DelegateScope.class));
    }
  }

  @Value
  @Builder
  public static class TagTestData {
    List<String> taskTags;
    List<String> delegateTags;
    boolean assignable;
    int numOfMissingAllSelectorsInvocations;
    int numOfMissingSelectorInvocations;
  }

  @Test
  @Owner(developers = BRETT)
  @Category(UnitTests.class)
  public void assignByTags() {
    List<TagTestData> tests = ImmutableList.<TagTestData>builder()
                                  .add(TagTestData.builder()
                                           .taskTags(null)
                                           .delegateTags(null)
                                           .assignable(true)
                                           .numOfMissingAllSelectorsInvocations(0)
                                           .numOfMissingSelectorInvocations(0)
                                           .build())
                                  .add(TagTestData.builder()
                                           .taskTags(null)
                                           .delegateTags(emptyList())
                                           .assignable(true)
                                           .numOfMissingAllSelectorsInvocations(0)
                                           .numOfMissingSelectorInvocations(0)
                                           .build())
                                  .add(TagTestData.builder()
                                           .taskTags(emptyList())
                                           .delegateTags(null)
                                           .assignable(true)
                                           .numOfMissingAllSelectorsInvocations(0)
                                           .numOfMissingSelectorInvocations(0)
                                           .build())
                                  .add(TagTestData.builder()
                                           .taskTags(emptyList())
                                           .delegateTags(emptyList())
                                           .assignable(true)
                                           .numOfMissingAllSelectorsInvocations(0)
                                           .numOfMissingSelectorInvocations(0)
                                           .build())
                                  .add(TagTestData.builder()
                                           .taskTags(ImmutableList.of("a"))
                                           .delegateTags(null)
                                           .assignable(false)
                                           .numOfMissingAllSelectorsInvocations(1)
                                           .numOfMissingSelectorInvocations(0)
                                           .build())
                                  .add(TagTestData.builder()
                                           .taskTags(ImmutableList.of("a"))
                                           .delegateTags(emptyList())
                                           .assignable(false)
                                           .numOfMissingAllSelectorsInvocations(2)
                                           .numOfMissingSelectorInvocations(0)
                                           .build())
                                  .add(TagTestData.builder()
                                           .taskTags(null)
                                           .delegateTags(ImmutableList.of("a"))
                                           .assignable(true)
                                           .numOfMissingAllSelectorsInvocations(2)
                                           .numOfMissingSelectorInvocations(0)
                                           .build())
                                  .add(TagTestData.builder()
                                           .taskTags(emptyList())
                                           .delegateTags(ImmutableList.of("a"))
                                           .assignable(true)
                                           .numOfMissingAllSelectorsInvocations(2)
                                           .numOfMissingSelectorInvocations(0)
                                           .build())
                                  .add(TagTestData.builder()
                                           .taskTags(ImmutableList.of("a", "b"))
                                           .delegateTags(ImmutableList.of("a", "c", "b"))
                                           .assignable(true)
                                           .numOfMissingAllSelectorsInvocations(2)
                                           .numOfMissingSelectorInvocations(0)
                                           .build())
                                  .add(TagTestData.builder()
                                           .taskTags(ImmutableList.of("a", "b", "c"))
                                           .delegateTags(ImmutableList.of("a", "b"))
                                           .assignable(false)
                                           .numOfMissingAllSelectorsInvocations(2)
                                           .numOfMissingSelectorInvocations(1)
                                           .build())
                                  .add(TagTestData.builder()
                                           .taskTags(ImmutableList.of("a", "b"))
                                           .delegateTags(ImmutableList.of("c", "a"))
                                           .assignable(false)
                                           .numOfMissingAllSelectorsInvocations(2)
                                           .numOfMissingSelectorInvocations(2)
                                           .build())
                                  .add(TagTestData.builder()
                                           .taskTags(ImmutableList.of("a", "b"))
                                           .delegateTags(ImmutableList.of("c", "d"))
                                           .assignable(false)
                                           .numOfMissingAllSelectorsInvocations(2)
                                           .numOfMissingSelectorInvocations(4)
                                           .build())
                                  .add(TagTestData.builder()
                                           .taskTags(ImmutableList.of("a ", " B "))
                                           .delegateTags(ImmutableList.of("A", " b"))
                                           .assignable(true)
                                           .numOfMissingAllSelectorsInvocations(2)
                                           .numOfMissingSelectorInvocations(4)
                                           .build())
                                  .add(TagTestData.builder()
                                           .taskTags(ImmutableList.of("a-b"))
                                           .delegateTags(ImmutableList.of("a", " b"))
                                           .assignable(false)
                                           .numOfMissingAllSelectorsInvocations(2)
                                           .numOfMissingSelectorInvocations(5)
                                           .build())
                                  .add(TagTestData.builder()
                                           .taskTags(ImmutableList.of("a"))
                                           .delegateTags(ImmutableList.of("a-b"))
                                           .assignable(false)
                                           .numOfMissingAllSelectorsInvocations(2)
                                           .numOfMissingSelectorInvocations(6)
                                           .build())
                                  .add(TagTestData.builder()
                                           .taskTags(ImmutableList.of("", " "))
                                           .delegateTags(ImmutableList.of("a"))
                                           .assignable(true)
                                           .numOfMissingAllSelectorsInvocations(2)
                                           .numOfMissingSelectorInvocations(6)
                                           .build())
                                  .build();

    DelegateTaskBuilder delegateTaskBuilder = DelegateTask.builder()
                                                  .accountId(ACCOUNT_ID)
                                                  .setupAbstraction(Cd1SetupFields.APP_ID_FIELD, APP_ID)
                                                  .data(TaskData.builder()
                                                            .async(true)
                                                            .taskType(TaskType.SCRIPT.name())
                                                            .timeout(DEFAULT_ASYNC_CALL_TIMEOUT)
                                                            .build());

    DelegateProfile delegateProfile =
        DelegateProfile.builder().uuid(generateUuid()).accountId(ACCOUNT_ID).name("testProfileName").build();

    DelegateBuilder delegateBuilder = Delegate.builder()
                                          .accountId(ACCOUNT_ID)
                                          .uuid(DELEGATE_ID)
                                          .hostName("a.b.c.")
                                          .delegateName("testDelegateName")
                                          .delegateProfileId(delegateProfile.getUuid())
                                          .includeScopes(emptyList())
                                          .excludeScopes(emptyList());

    for (TagTestData test : tests) {
      Delegate delegate = delegateBuilder.tags(test.getDelegateTags()).build();
      when(delegateService.get(ACCOUNT_ID, DELEGATE_ID, false)).thenReturn(delegate);
      when(delegateService.retrieveDelegateSelectors(delegate))
          .thenReturn(delegate.getTags() == null ? new HashSet<>() : new HashSet<>(test.getDelegateTags()));

      DelegateTask delegateTask = delegateTaskBuilder.tags(test.getTaskTags()).build();
      BatchDelegateSelectionLog batch = BatchDelegateSelectionLog.builder().taskId(delegateTask.getUuid()).build();
      assertThat(assignDelegateService.canAssign(batch, DELEGATE_ID, delegateTask)).isEqualTo(test.isAssignable());

      verify(delegateSelectionLogsService, Mockito.times(test.getNumOfMissingAllSelectorsInvocations()))
          .logMissingAllSelectors(batch, ACCOUNT_ID, DELEGATE_ID);
      verify(delegateSelectionLogsService, Mockito.times(test.getNumOfMissingSelectorInvocations()))
          .logMissingSelector(eq(batch), eq(ACCOUNT_ID), eq(DELEGATE_ID), anyString());
    }

    delegateTaskBuilder.setupAbstraction(Cd1SetupFields.ENV_ID_FIELD, ENV_ID);
    delegateBuilder.excludeScopes(
        ImmutableList.of(DelegateScope.builder().environmentTypes(ImmutableList.of(PROD)).build()));

    for (TagTestData test : tests) {
      Delegate delegate = delegateBuilder.tags(test.getDelegateTags()).build();
      when(delegateService.get(ACCOUNT_ID, DELEGATE_ID, false)).thenReturn(delegate);
      when(delegateService.retrieveDelegateSelectors(delegate))
          .thenReturn(delegate.getTags() == null ? new HashSet<>() : new HashSet<>(test.getDelegateTags()));

      DelegateTask delegateTask = delegateTaskBuilder.tags(test.getTaskTags()).build();
      BatchDelegateSelectionLog batch = BatchDelegateSelectionLog.builder().taskId(delegateTask.getUuid()).build();
      assertThat(assignDelegateService.canAssign(batch, DELEGATE_ID, delegateTask)).isFalse();
    }
  }

  @Value
  @Builder
  public static class NameTestData {
    List<String> taskTags;
    String delegateName;
    String hostName;
    boolean assignable;
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void assignByNames() {
    when(delegateService.retrieveDelegateSelectors(any(Delegate.class)))
        .thenReturn(emptySet())
        .thenReturn(new HashSet<>(Arrays.asList("A")))
        .thenReturn(new HashSet<>(Arrays.asList("a", "b")));

    List<NameTestData> tests =
        ImmutableList.<NameTestData>builder()
            .add(NameTestData.builder().taskTags(ImmutableList.of("a")).assignable(false).build())
            .add(NameTestData.builder().taskTags(ImmutableList.of("a")).delegateName("A").assignable(true).build())
            .add(NameTestData.builder().taskTags(ImmutableList.of("a")).hostName("A").assignable(true).build())
            .add(NameTestData.builder().taskTags(ImmutableList.of("a")).hostName("A").assignable(true).build())
            .add(NameTestData.builder()
                     .taskTags(ImmutableList.of("a", "b"))
                     .delegateName("A")
                     .hostName("b")
                     .assignable(true)
                     .build())
            .build();

    DelegateTaskBuilder delegateTaskBuilder = DelegateTask.builder()
                                                  .accountId(ACCOUNT_ID)
                                                  .setupAbstraction(Cd1SetupFields.APP_ID_FIELD, APP_ID)
                                                  .data(TaskData.builder()
                                                            .async(true)
                                                            .taskType(TaskType.SCRIPT.name())
                                                            .timeout(DEFAULT_ASYNC_CALL_TIMEOUT)
                                                            .build());

    DelegateBuilder delegateBuilder = Delegate.builder()
                                          .accountId(ACCOUNT_ID)
                                          .uuid(DELEGATE_ID)
                                          .includeScopes(emptyList())
                                          .excludeScopes(emptyList());

    for (NameTestData test : tests) {
      Delegate delegate = delegateBuilder.delegateName(test.getDelegateName()).hostName(test.getHostName()).build();
      when(delegateService.get(ACCOUNT_ID, DELEGATE_ID, false)).thenReturn(delegate);

      DelegateTask delegateTask = delegateTaskBuilder.tags(test.getTaskTags()).build();
      BatchDelegateSelectionLog batch = BatchDelegateSelectionLog.builder().taskId(delegateTask.getUuid()).build();
      assertThat(assignDelegateService.canAssign(batch, DELEGATE_ID, delegateTask)).isEqualTo(test.isAssignable());
    }
  }

  @Test
  @Owner(developers = BRETT)
  @Category(UnitTests.class)
  public void shouldSaveConnectionResults() {
    List<DelegateConnectionResult> results = singletonList(DelegateConnectionResult.builder()
                                                               .accountId(ACCOUNT_ID)
                                                               .delegateId(DELEGATE_ID)
                                                               .criteria("criteria")
                                                               .validated(true)
                                                               .build());

    assignDelegateService.saveConnectionResults(results);

    DelegateConnectionResult saved = wingsPersistence.createQuery(DelegateConnectionResult.class).get();
    assertThat(saved).isNotNull();
    assertThat(saved.getCriteria()).isEqualTo("criteria");
    assertThat(saved.isValidated()).isTrue();
  }

  @Test
  @Owner(developers = BRETT)
  @Category(UnitTests.class)
  public void shouldUpdateConnectionResults() {
    wingsPersistence.save(DelegateConnectionResult.builder()
                              .accountId(ACCOUNT_ID)
                              .delegateId(DELEGATE_ID)
                              .criteria("criteria")
                              .validated(false)
                              .build());

    List<DelegateConnectionResult> results = singletonList(DelegateConnectionResult.builder()
                                                               .accountId(ACCOUNT_ID)
                                                               .delegateId(DELEGATE_ID)
                                                               .criteria("criteria")
                                                               .validated(true)
                                                               .build());

    assignDelegateService.saveConnectionResults(results);

    List<DelegateConnectionResult> saved = wingsPersistence.createQuery(DelegateConnectionResult.class)
                                               .filter(DelegateConnectionResultKeys.accountId, ACCOUNT_ID)
                                               .asList();
    assertThat(saved).isNotNull();
    assertThat(saved.size()).isEqualTo(1);
    assertThat(saved.get(0).getCriteria()).isEqualTo("criteria");
    assertThat(saved.get(0).isValidated()).isTrue();
  }

  @Test
  @Owner(developers = BRETT)
  @Category(UnitTests.class)
  public void shouldBeWhitelisted() {
    wingsPersistence.save(DelegateConnectionResult.builder()
                              .accountId(ACCOUNT_ID)
                              .delegateId(DELEGATE_ID)
                              .criteria("criteria")
                              .validated(true)
                              .build());

    Object[] params = {HttpTaskParameters.builder().url("criteria").build()};

    DelegateTask delegateTask = DelegateTask.builder()
                                    .accountId(ACCOUNT_ID)
                                    .data(TaskData.builder()
                                              .async(true)
                                              .taskType(TaskType.HTTP.name())
                                              .parameters(params)
                                              .timeout(DEFAULT_ASYNC_CALL_TIMEOUT)
                                              .build())
                                    .build();

    assertThat(assignDelegateService.isWhitelisted(delegateTask, DELEGATE_ID)).isTrue();
  }

  @Test
  @Owner(developers = BRETT)
  @Category(UnitTests.class)
  public void shouldNotBeWhitelistedDiffCriteria() {
    wingsPersistence.save(DelegateConnectionResult.builder()
                              .accountId(ACCOUNT_ID)
                              .delegateId(DELEGATE_ID)
                              .criteria("criteria")
                              .validated(true)
                              .build());
    Object[] params = {HttpTaskParameters.builder().url("criteria-other").build()};

    DelegateTask delegateTask = DelegateTask.builder()
                                    .accountId(ACCOUNT_ID)
                                    .data(TaskData.builder()
                                              .async(true)
                                              .taskType(TaskType.HTTP.name())
                                              .parameters(params)
                                              .timeout(DEFAULT_ASYNC_CALL_TIMEOUT)
                                              .build())
                                    .build();

    assertThat(assignDelegateService.isWhitelisted(delegateTask, DELEGATE_ID)).isFalse();
  }

  @Test
  @Owner(developers = BRETT)
  @Category(UnitTests.class)
  public void shouldNotBeWhitelistedWhenNotValidated() {
    wingsPersistence.save(DelegateConnectionResult.builder()
                              .accountId(ACCOUNT_ID)
                              .delegateId(DELEGATE_ID)
                              .criteria("criteria")
                              .validated(false)
                              .build());
    Object[] params = {HttpTaskParameters.builder().url("criteria").build()};

    DelegateTask delegateTask = DelegateTask.builder()
                                    .accountId(ACCOUNT_ID)
                                    .data(TaskData.builder()
                                              .async(true)
                                              .taskType(TaskType.HTTP.name())
                                              .parameters(params)
                                              .timeout(DEFAULT_ASYNC_CALL_TIMEOUT)
                                              .build())
                                    .build();

    assertThat(assignDelegateService.isWhitelisted(delegateTask, DELEGATE_ID)).isFalse();
  }

  @Test
  @Owner(developers = BRETT)
  @Category(UnitTests.class)
  public void shouldGetConnectedWhitelistedDelegates() {
    DelegateTask delegateTask = createDelegateTask(true, "criteria");

    List<String> delegateIds = assignDelegateService.connectedWhitelistedDelegates(delegateTask);

    assertThat(delegateIds.size()).isEqualTo(1);
    assertThat(delegateIds.get(0)).isEqualTo(DELEGATE_ID);
  }

  private DelegateTask createDelegateTask(boolean b, String criteria) {
    Delegate delegate = Delegate.builder()
                            .accountId(ACCOUNT_ID)
                            .uuid(DELEGATE_ID)
                            .status(ENABLED)
                            .lastHeartBeat(clock.millis())
                            .build();
    wingsPersistence.save(delegate);
    wingsPersistence.save(DelegateConnectionResult.builder()
                              .accountId(ACCOUNT_ID)
                              .delegateId(DELEGATE_ID)
                              .criteria("criteria")
                              .validated(b)
                              .build());
    when(delegateService.get(ACCOUNT_ID, DELEGATE_ID, false)).thenReturn(delegate);

    Object[] params = {HttpTaskParameters.builder().url(criteria).build()};
    return DelegateTask.builder()
        .accountId(ACCOUNT_ID)
        .data(TaskData.builder()
                  .async(true)
                  .taskType(TaskType.HTTP.name())
                  .parameters(params)
                  .timeout(DEFAULT_ASYNC_CALL_TIMEOUT)
                  .build())
        .build();
  }

  @Test
  @Owner(developers = BRETT)
  @Category(UnitTests.class)
  public void shouldNotGetConnectedWhitelistedDelegatesNotValidated() {
    DelegateTask delegateTask = createDelegateTask(false, "criteria");

    List<String> delegateIds = assignDelegateService.connectedWhitelistedDelegates(delegateTask);

    assertThat(delegateIds.size()).isEqualTo(0);
  }

  @Test
  @Owner(developers = BRETT)
  @Category(UnitTests.class)
  public void shouldNotGetConnectedWhitelistedDelegatesOldHeartbeat() {
    Delegate delegate = Delegate.builder()
                            .accountId(ACCOUNT_ID)
                            .uuid(DELEGATE_ID)
                            .status(ENABLED)
                            .lastHeartBeat(clock.millis() - MAX_DELEGATE_LAST_HEARTBEAT - 1000)
                            .build();
    wingsPersistence.save(delegate);
    wingsPersistence.save(DelegateConnectionResult.builder()
                              .accountId(ACCOUNT_ID)
                              .delegateId(DELEGATE_ID)
                              .criteria("criteria")
                              .validated(true)
                              .build());
    when(delegateService.get(ACCOUNT_ID, DELEGATE_ID, false)).thenReturn(delegate);

    Object[] params = {HttpTaskParameters.builder().url("criteria").build()};
    DelegateTask delegateTask = DelegateTask.builder()
                                    .accountId(ACCOUNT_ID)
                                    .data(TaskData.builder()
                                              .async(true)
                                              .taskType(TaskType.HTTP.name())
                                              .parameters(params)
                                              .timeout(DEFAULT_ASYNC_CALL_TIMEOUT)
                                              .build())
                                    .build();

    List<String> delegateIds = assignDelegateService.connectedWhitelistedDelegates(delegateTask);

    assertThat(delegateIds.size()).isEqualTo(0);
  }

  @Test
  @Owner(developers = BRETT)
  @Category(UnitTests.class)
  public void shouldNotGetConnectedWhitelistedDelegatesOtherCriteria() {
    DelegateTask delegateTask = createDelegateTask(true, "criteria-other");

    List<String> delegateIds = assignDelegateService.connectedWhitelistedDelegates(delegateTask);

    assertThat(delegateIds.size()).isEqualTo(0);
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void shouldGetNullFirstAttemptDelegate() {
    Object[] params = {HttpTaskParameters.builder().url("criteria-other").build()};
    DelegateTask delegateTask = DelegateTask.builder()
                                    .accountId(ACCOUNT_ID)
                                    .data(TaskData.builder()
                                              .async(true)
                                              .taskType(TaskType.HTTP.name())
                                              .parameters(params)
                                              .timeout(DEFAULT_ASYNC_CALL_TIMEOUT)
                                              .build())
                                    .build();

    String delegateId = assignDelegateService.pickFirstAttemptDelegate(delegateTask);

    assertThat(delegateId).isEqualTo(null);
  }

  @Test
  @Owner(developers = PUNEET)
  @Category(UnitTests.class)
  public void shouldGetFirstAttemptDelegate() {
    DelegateTask delegateTask = createDelegateTask(true, "criteria");

    String delegateId = assignDelegateService.pickFirstAttemptDelegate(delegateTask);

    assertThat(delegateId).isEqualTo(DELEGATE_ID);
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void testAssignDelegateWithNullIncludeScope() {
    DelegateTask delegateTask = DelegateTask.builder()
                                    .accountId(ACCOUNT_ID)
                                    .setupAbstraction(Cd1SetupFields.APP_ID_FIELD, APP_ID)
                                    .setupAbstraction(Cd1SetupFields.ENV_ID_FIELD, ENV_ID)
                                    .data(TaskData.builder().async(true).timeout(DEFAULT_ASYNC_CALL_TIMEOUT).build())
                                    .build();
    Delegate delegate = Delegate.builder()
                            .accountId(ACCOUNT_ID)
                            .uuid(DELEGATE_ID)
                            .includeScopes(singletonList(null))
                            .excludeScopes(emptyList())
                            .build();
    BatchDelegateSelectionLog batch = BatchDelegateSelectionLog.builder().taskId(delegateTask.getUuid()).build();
    when(delegateService.get(ACCOUNT_ID, DELEGATE_ID, false)).thenReturn(delegate);
    assertThat(assignDelegateService.canAssign(batch, DELEGATE_ID, delegateTask)).isTrue();
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void testAssignDelegateWithNullExcludeScope() {
    DelegateTask delegateTask = DelegateTask.builder()
                                    .accountId(ACCOUNT_ID)
                                    .setupAbstraction(Cd1SetupFields.APP_ID_FIELD, APP_ID)
                                    .setupAbstraction(Cd1SetupFields.ENV_ID_FIELD, ENV_ID)
                                    .data(TaskData.builder().async(true).timeout(DEFAULT_ASYNC_CALL_TIMEOUT).build())
                                    .build();

    Delegate delegate = Delegate.builder()
                            .accountId(ACCOUNT_ID)
                            .uuid(DELEGATE_ID)
                            .includeScopes(emptyList())
                            .excludeScopes(singletonList(null))
                            .build();
    BatchDelegateSelectionLog batch = BatchDelegateSelectionLog.builder().taskId(delegateTask.getUuid()).build();
    when(delegateService.get(ACCOUNT_ID, DELEGATE_ID, false)).thenReturn(delegate);
    assertThat(assignDelegateService.canAssign(batch, DELEGATE_ID, delegateTask)).isTrue();
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testAssignDelegateWithMultipleIncludeScopes() {
    DelegateTask delegateTask = DelegateTask.builder()
                                    .accountId(ACCOUNT_ID)
                                    .setupAbstraction(Cd1SetupFields.APP_ID_FIELD, APP_ID)
                                    .setupAbstraction(Cd1SetupFields.ENV_ID_FIELD, ENV_ID)
                                    .data(TaskData.builder().async(true).timeout(DEFAULT_ASYNC_CALL_TIMEOUT).build())
                                    .build();

    List<DelegateScope> includeScopes = new ArrayList<>();
    includeScopes.add(null);
    includeScopes.add(DelegateScope.builder().environmentTypes(ImmutableList.of(PROD)).build());

    Delegate delegate = Delegate.builder()
                            .accountId(ACCOUNT_ID)
                            .uuid(DELEGATE_ID)
                            .includeScopes(includeScopes)
                            .excludeScopes(emptyList())
                            .build();
    BatchDelegateSelectionLog batch = BatchDelegateSelectionLog.builder().taskId(delegateTask.getUuid()).build();
    when(delegateService.get(ACCOUNT_ID, DELEGATE_ID, false)).thenReturn(delegate);
    assertThat(assignDelegateService.canAssign(batch, DELEGATE_ID, delegateTask)).isTrue();
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldAssignDelegateWithInfrastructureMappingScope() {
    InfrastructureMapping infrastructureMapping =
        aGcpKubernetesInfrastructureMapping()
            .withAppId(APP_ID)
            .withComputeProviderSettingId(COMPUTE_PROVIDER_SETTING_ID)
            .withUuid(INFRA_MAPPING_ID)
            .withClusterName("k")
            .withNamespace("default")
            .withEnvId(InstanceSyncTestConstants.ENV_ID)
            .withInfraMappingType(InfrastructureMappingType.GCP_KUBERNETES.getName())
            .withServiceId(SERVICE_ID)
            .withUuid(INFRA_MAPPING_ID)
            .withAccountId(InstanceSyncTestConstants.ACCOUNT_ID)
            .build();
    infrastructureMapping.setInfrastructureDefinitionId(INFRA_DEFINITION_ID);

    List<DelegateScope> scopes = new ArrayList<>();
    scopes.add(DelegateScope.builder()
                   .infrastructureDefinitions(ImmutableList.of(INFRA_DEFINITION_ID))
                   .services(ImmutableList.of(SERVICE_ID))
                   .build());

    DelegateTask delegateTask =
        DelegateTask.builder()
            .accountId(ACCOUNT_ID)
            .setupAbstraction(Cd1SetupFields.APP_ID_FIELD, APP_ID)
            .setupAbstraction(Cd1SetupFields.ENV_ID_FIELD, ENV_ID)
            .setupAbstraction(Cd1SetupFields.INFRASTRUCTURE_MAPPING_ID_FIELD, infrastructureMapping.getUuid())
            .data(TaskData.builder().async(true).timeout(DEFAULT_ASYNC_CALL_TIMEOUT).build())
            .build();

    DelegateTask delegateTask2 =
        DelegateTask.builder()
            .accountId(ACCOUNT_ID)
            .setupAbstraction(Cd1SetupFields.APP_ID_FIELD, APP_ID)
            .setupAbstraction(Cd1SetupFields.ENV_ID_FIELD, ENV_ID)
            .setupAbstraction(Cd1SetupFields.INFRASTRUCTURE_MAPPING_ID_FIELD, WRONG_INFRA_MAPPING_ID)
            .data(TaskData.builder().async(true).timeout(DEFAULT_ASYNC_CALL_TIMEOUT).build())
            .build();
    Delegate delegate = Delegate.builder()
                            .accountId(ACCOUNT_ID)
                            .uuid(DELEGATE_ID)
                            .includeScopes(scopes)
                            .excludeScopes(emptyList())
                            .build();
    BatchDelegateSelectionLog batch = BatchDelegateSelectionLog.builder().taskId(delegateTask.getUuid()).build();
    when(featureFlagService.isEnabled(eq(INFRA_MAPPING_REFACTOR), any())).thenReturn(true);
    when(infrastructureMappingService.get(APP_ID, INFRA_MAPPING_ID)).thenReturn(infrastructureMapping);
    when(delegateService.get(ACCOUNT_ID, DELEGATE_ID, false)).thenReturn(delegate);
    assertThat(assignDelegateService.canAssign(batch, DELEGATE_ID, delegateTask)).isTrue();

    assertThat(assignDelegateService.canAssign(batch, DELEGATE_ID, delegateTask2)).isFalse();
  }

  @Test
  @Owner(developers = VUK)
  @Category(UnitTests.class)
  public void shouldExtractSelectorsFromSelectorsCapability() {
    Set<String> selectors1 = Stream.of("a", "b").collect(Collectors.toSet());
    Set<String> selectors2 = Stream.of("a", "c").collect(Collectors.toSet());

    SelectorCapability selectorCapability1 = SelectorCapability.builder().selectors(selectors1).build();
    SelectorCapability selectorCapability2 = SelectorCapability.builder().selectors(selectors2).build();

    List<ExecutionCapability> executionCapabilityList = Arrays.asList(selectorCapability1, selectorCapability2);

    DelegateTask delegateTask =
        DelegateTask.builder().accountId(ACCOUNT_ID).executionCapabilities(executionCapabilityList).build();

    List<String> extractSelectorsList = assignDelegateService.extractSelectors(delegateTask);

    assertThat(extractSelectorsList).isNotNull();
    assertThat(extractSelectorsList).isNotEmpty();
    assertThat(extractSelectorsList).containsExactlyInAnyOrder("a", "b", "c");
  }

  @Test
  @Owner(developers = VUK)
  @Category(UnitTests.class)
  public void shouldNotExtractSelectorsFromSelectorsCapability() {
    HttpConnectionExecutionCapability httpConnectionExecutionCapability =
        HttpConnectionExecutionCapability.builder().port(80).build();

    List<ExecutionCapability> executionCapabilityList = Arrays.asList(httpConnectionExecutionCapability);

    DelegateTask delegateTask =
        DelegateTask.builder().accountId(ACCOUNT_ID).executionCapabilities(executionCapabilityList).build();

    List<String> extractSelectorsList = assignDelegateService.extractSelectors(delegateTask);

    assertThat(extractSelectorsList).isNullOrEmpty();
  }

  @Test
  @Owner(developers = VUK)
  @Category(UnitTests.class)
  public void shouldExtractSelectorFromTaskSelectors() {
    List<String> tagsList = Arrays.asList("a", "b", "c");

    DelegateTask delegateTask = DelegateTask.builder().accountId(ACCOUNT_ID).tags(tagsList).build();

    List<String> extractSelectorsList = assignDelegateService.extractSelectors(delegateTask);

    assertThat(extractSelectorsList).isNotNull();
    assertThat(extractSelectorsList).isNotEmpty();
    assertThat(extractSelectorsList).containsExactlyInAnyOrder("a", "b", "c");
  }

  @Test
  @Owner(developers = VUK)
  @Category(UnitTests.class)
  public void shouldExtractSelectorMerged() {
    Set<String> selectors = Stream.of("a", "d").collect(Collectors.toSet());
    List<String> tagsList = Arrays.asList("a", "b", "c");

    SelectorCapability selectorCapability = SelectorCapability.builder().selectors(selectors).build();

    List<ExecutionCapability> executionCapabilityList = Arrays.asList(selectorCapability);

    DelegateTask delegateTask = DelegateTask.builder()
                                    .accountId(ACCOUNT_ID)
                                    .tags(tagsList)
                                    .executionCapabilities(executionCapabilityList)
                                    .build();

    List<String> extractSelectorsList = assignDelegateService.extractSelectors(delegateTask);

    assertThat(extractSelectorsList).isNotNull();
    assertThat(extractSelectorsList).isNotEmpty();
    assertThat(extractSelectorsList).containsExactlyInAnyOrder("a", "b", "c", "d");
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void shouldRetrieveNoActiveDelegates() {
    String accountId = generateUuid();

    List<String> activeDelegates = assignDelegateService.retrieveActiveDelegates(accountId, null);
    assertThat(activeDelegates).isNotNull();
    assertThat(activeDelegates).isEmpty();
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void shouldRetrieveActiveDelegates() {
    String accountId = generateUuid();
    String activeDelegate1Id = generateUuid();
    String activeDelegate2Id = generateUuid();
    Delegate activeDelegate1 = createDelegateBuilder().accountId(accountId).uuid(activeDelegate1Id).build();

    Delegate activeDelegate2 = createDelegateBuilder().accountId(accountId).uuid(activeDelegate2Id).build();

    Delegate disconnectedDelegate = createDelegateBuilder()
                                        .accountId(accountId)
                                        .uuid(generateUuid())
                                        .lastHeartBeat(System.currentTimeMillis() - 500000L)
                                        .build();

    Delegate wapprDelegate = createDelegateBuilder()
                                 .accountId(accountId)
                                 .uuid(generateUuid())
                                 .status(Delegate.Status.WAITING_FOR_APPROVAL)
                                 .build();

    Delegate deletedDelegate =
        createDelegateBuilder().accountId(accountId).uuid(generateUuid()).status(Delegate.Status.DELETED).build();

    wingsPersistence.save(
        Arrays.asList(activeDelegate1, activeDelegate2, disconnectedDelegate, wapprDelegate, deletedDelegate));

    BatchDelegateSelectionLog batch = BatchDelegateSelectionLog.builder().taskId(generateUuid()).build();

    List<String> activeDelegates = assignDelegateService.retrieveActiveDelegates(accountId, batch);
    assertThat(activeDelegates).isNotNull();
    assertThat(activeDelegates.size()).isEqualTo(2);
    assertThat(activeDelegates.containsAll(Arrays.asList(activeDelegate1Id, activeDelegate2Id))).isTrue();

    Set<String> disconnectedDelegates = new HashSet<>();
    disconnectedDelegates.add(disconnectedDelegate.getUuid());
    verify(delegateSelectionLogsService).logDisconnectedDelegate(eq(batch), eq(accountId), eq(disconnectedDelegates));

    Set<String> wapprDelegates = new HashSet<>();
    wapprDelegates.add(wapprDelegate.getUuid());
    verify(delegateSelectionLogsService).logWaitingForApprovalDelegate(eq(batch), eq(accountId), eq(wapprDelegates));
  }
}
