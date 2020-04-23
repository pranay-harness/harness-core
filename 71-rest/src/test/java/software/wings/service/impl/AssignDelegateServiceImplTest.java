package software.wings.service.impl;

import static io.harness.delegate.beans.TaskData.DEFAULT_ASYNC_CALL_TIMEOUT;
import static io.harness.rule.OwnerRule.ANSHUL;
import static io.harness.rule.OwnerRule.BRETT;
import static io.harness.rule.OwnerRule.GEORGE;
import static io.harness.rule.OwnerRule.PRASHANT;
import static io.harness.rule.OwnerRule.PUNEET;
import static io.harness.rule.OwnerRule.VUK;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.beans.Base.ACCOUNT_ID_KEY;
import static software.wings.beans.Delegate.Status.ENABLED;
import static software.wings.beans.Environment.Builder.anEnvironment;
import static software.wings.beans.Environment.EnvironmentType.NON_PROD;
import static software.wings.beans.Environment.EnvironmentType.PROD;
import static software.wings.beans.FeatureName.DELEGATE_TAGS_EXTENDED;
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
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.task.http.HttpTaskParameters;
import io.harness.rule.Owner;
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
import java.util.List;

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

  @Test
  @Owner(developers = BRETT)
  @Category(UnitTests.class)
  public void shouldAssignDelegateWithNoScope() {
    DelegateTask delegateTask = DelegateTask.builder()
                                    .accountId(ACCOUNT_ID)
                                    .appId(APP_ID)
                                    .envId(ENV_ID)
                                    .data(TaskData.builder().async(true).timeout(DEFAULT_ASYNC_CALL_TIMEOUT).build())
                                    .build();
    Delegate delegate = Delegate.builder()
                            .accountId(ACCOUNT_ID)
                            .uuid(DELEGATE_ID)
                            .includeScopes(emptyList())
                            .excludeScopes(emptyList())
                            .build();
    BatchDelegateSelectionLog batch = BatchDelegateSelectionLog.builder().taskId(delegateTask.getUuid()).build();
    when(delegateService.get(ACCOUNT_ID, DELEGATE_ID, false)).thenReturn(delegate);
    assertThat(assignDelegateService.canAssign(batch, DELEGATE_ID, delegateTask)).isTrue();

    verify(delegateSelectionLogsService, never())
        .logExcludeScopeMatched(eq(batch), anyString(), anyString(), any(DelegateScope.class));
    verify(delegateSelectionLogsService, never()).logNoIncludeScopeMatched(eq(batch), anyString(), anyString());
  }

  @Test
  @Owner(developers = BRETT)
  @Category(UnitTests.class)
  public void shouldAssignDelegateWithMatchingIncludeScopes() {
    DelegateTask delegateTask = DelegateTask.builder()
                                    .accountId(ACCOUNT_ID)
                                    .appId(APP_ID)
                                    .envId(ENV_ID)
                                    .data(TaskData.builder().async(true).timeout(DEFAULT_ASYNC_CALL_TIMEOUT).build())
                                    .build();

    List<DelegateScope> includeScopes =
        ImmutableList.of(DelegateScope.builder().environmentTypes(ImmutableList.of(PROD)).build());
    Delegate delegate = Delegate.builder()
                            .accountId(ACCOUNT_ID)
                            .uuid(DELEGATE_ID)
                            .includeScopes(includeScopes)
                            .excludeScopes(emptyList())
                            .build();
    BatchDelegateSelectionLog batch = BatchDelegateSelectionLog.builder().taskId(delegateTask.getUuid()).build();
    when(delegateService.get(ACCOUNT_ID, DELEGATE_ID, false)).thenReturn(delegate);
    assertThat(assignDelegateService.canAssign(batch, DELEGATE_ID, delegateTask)).isTrue();

    verify(delegateSelectionLogsService, never())
        .logExcludeScopeMatched(eq(batch), anyString(), anyString(), any(DelegateScope.class));
    verify(delegateSelectionLogsService, never()).logNoIncludeScopeMatched(eq(batch), anyString(), anyString());
  }

  @Test
  @Owner(developers = VUK)
  @Category(UnitTests.class)
  public void shouldAssignDelegateWithMatchingIncludeScopesAndWithoutMatchingExcludeScope() {
    DelegateTask delegateTask = DelegateTask.builder()
                                    .accountId(ACCOUNT_ID)
                                    .appId(APP_ID)
                                    .envId(ENV_ID)
                                    .data(TaskData.builder().async(true).timeout(DEFAULT_ASYNC_CALL_TIMEOUT).build())
                                    .build();

    List<DelegateScope> includeScopes =
        ImmutableList.of(DelegateScope.builder().environmentTypes(ImmutableList.of(PROD)).build());
    List<DelegateScope> excludeScopes =
        ImmutableList.of(DelegateScope.builder().environmentTypes(ImmutableList.of(NON_PROD)).build());
    Delegate delegate = Delegate.builder()
                            .accountId(ACCOUNT_ID)
                            .uuid(DELEGATE_ID)
                            .includeScopes(includeScopes)
                            .excludeScopes(excludeScopes)
                            .build();
    BatchDelegateSelectionLog batch = BatchDelegateSelectionLog.builder().taskId(delegateTask.getUuid()).build();
    when(delegateService.get(ACCOUNT_ID, DELEGATE_ID, false)).thenReturn(delegate);
    assertThat(assignDelegateService.canAssign(batch, DELEGATE_ID, delegateTask)).isTrue();

    verify(delegateSelectionLogsService, never())
        .logExcludeScopeMatched(batch, ACCOUNT_ID, DELEGATE_ID, excludeScopes.get(0));
    verify(delegateSelectionLogsService, never()).logNoIncludeScopeMatched(eq(batch), anyString(), anyString());
  }

  @Test
  @Owner(developers = VUK)
  @Category(UnitTests.class)
  public void shouldNotAssignDelegateWithMatchingIncludeScopesAndWithMatchingExcludeScope() {
    DelegateTask delegateTask = DelegateTask.builder()
                                    .accountId(ACCOUNT_ID)
                                    .appId(APP_ID)
                                    .envId(ENV_ID)
                                    .data(TaskData.builder().async(true).timeout(DEFAULT_ASYNC_CALL_TIMEOUT).build())
                                    .build();

    List<DelegateScope> includeScopes =
        ImmutableList.of(DelegateScope.builder().environmentTypes(ImmutableList.of(PROD)).build());
    List<DelegateScope> excludeScopes =
        ImmutableList.of(DelegateScope.builder().environmentTypes(ImmutableList.of(PROD)).build());
    Delegate delegate = Delegate.builder()
                            .accountId(ACCOUNT_ID)
                            .uuid(DELEGATE_ID)
                            .includeScopes(includeScopes)
                            .excludeScopes(excludeScopes)
                            .build();
    BatchDelegateSelectionLog batch = BatchDelegateSelectionLog.builder().taskId(delegateTask.getUuid()).build();
    when(delegateService.get(ACCOUNT_ID, DELEGATE_ID, false)).thenReturn(delegate);
    assertThat(assignDelegateService.canAssign(batch, DELEGATE_ID, delegateTask)).isFalse();

    verify(delegateSelectionLogsService).logExcludeScopeMatched(batch, ACCOUNT_ID, DELEGATE_ID, excludeScopes.get(0));
    verify(delegateSelectionLogsService, never()).logNoIncludeScopeMatched(eq(batch), anyString(), anyString());
  }

  @Test
  @Owner(developers = BRETT)
  @Category(UnitTests.class)
  public void shouldNotAssignDelegateWithoutMatchingIncludeScopes() {
    DelegateTask delegateTask = DelegateTask.builder()
                                    .accountId(ACCOUNT_ID)
                                    .appId(APP_ID)
                                    .envId(ENV_ID)
                                    .data(TaskData.builder().async(true).timeout(DEFAULT_ASYNC_CALL_TIMEOUT).build())
                                    .build();

    List<DelegateScope> includeScopes =
        ImmutableList.of(DelegateScope.builder().environmentTypes(ImmutableList.of(NON_PROD)).build());
    Delegate delegate = Delegate.builder()
                            .accountId(ACCOUNT_ID)
                            .uuid(DELEGATE_ID)
                            .includeScopes(includeScopes)
                            .excludeScopes(emptyList())
                            .build();
    BatchDelegateSelectionLog batch = BatchDelegateSelectionLog.builder().taskId(delegateTask.getUuid()).build();
    when(delegateService.get(ACCOUNT_ID, DELEGATE_ID, false)).thenReturn(delegate);
    assertThat(assignDelegateService.canAssign(batch, DELEGATE_ID, delegateTask)).isFalse();

    verify(delegateSelectionLogsService, never())
        .logExcludeScopeMatched(eq(batch), anyString(), anyString(), any(DelegateScope.class));
    verify(delegateSelectionLogsService).logNoIncludeScopeMatched(eq(batch), anyString(), anyString());
  }

  @Test
  @Owner(developers = BRETT)
  @Category(UnitTests.class)
  public void shouldAssignDelegateWithoutMatchingExcludeScopes() {
    DelegateTask delegateTask = DelegateTask.builder()
                                    .accountId(ACCOUNT_ID)
                                    .appId(APP_ID)
                                    .envId(ENV_ID)
                                    .data(TaskData.builder().async(true).timeout(DEFAULT_ASYNC_CALL_TIMEOUT).build())
                                    .build();

    List<DelegateScope> excludeScopes =
        ImmutableList.of(DelegateScope.builder().environmentTypes(ImmutableList.of(NON_PROD)).build());
    Delegate delegate = Delegate.builder()
                            .accountId(ACCOUNT_ID)
                            .uuid(DELEGATE_ID)
                            .includeScopes(emptyList())
                            .excludeScopes(excludeScopes)
                            .build();
    BatchDelegateSelectionLog batch = BatchDelegateSelectionLog.builder().taskId(delegateTask.getUuid()).build();
    when(delegateService.get(ACCOUNT_ID, DELEGATE_ID, false)).thenReturn(delegate);
    assertThat(assignDelegateService.canAssign(batch, DELEGATE_ID, delegateTask)).isTrue();

    verify(delegateSelectionLogsService, never())
        .logExcludeScopeMatched(batch, ACCOUNT_ID, DELEGATE_ID, excludeScopes.get(0));
    verify(delegateSelectionLogsService, never()).logNoIncludeScopeMatched(eq(batch), anyString(), anyString());
  }

  @Test
  @Owner(developers = BRETT)
  @Category(UnitTests.class)
  public void shouldNotAssignDelegateWithMatchingExcludeScopes() {
    DelegateTask delegateTask = DelegateTask.builder()
                                    .accountId(ACCOUNT_ID)
                                    .appId(APP_ID)
                                    .envId(ENV_ID)
                                    .data(TaskData.builder().async(true).timeout(DEFAULT_ASYNC_CALL_TIMEOUT).build())
                                    .build();

    List<DelegateScope> excludeScopes =
        ImmutableList.of(DelegateScope.builder().environmentTypes(ImmutableList.of(PROD)).build());
    Delegate delegate =
        Delegate.builder()
            .accountId(ACCOUNT_ID)
            .uuid(DELEGATE_ID)
            .includeScopes(emptyList())
            .excludeScopes(ImmutableList.of(DelegateScope.builder().environmentTypes(ImmutableList.of(PROD)).build()))
            .build();
    BatchDelegateSelectionLog batch = BatchDelegateSelectionLog.builder().taskId(delegateTask.getUuid()).build();
    when(delegateService.get(ACCOUNT_ID, DELEGATE_ID, false)).thenReturn(delegate);
    assertThat(assignDelegateService.canAssign(batch, DELEGATE_ID, delegateTask)).isFalse();

    verify(delegateSelectionLogsService).logExcludeScopeMatched(batch, ACCOUNT_ID, DELEGATE_ID, excludeScopes.get(0));
    verify(delegateSelectionLogsService, never()).logNoIncludeScopeMatched(eq(batch), anyString(), anyString());
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
                                                  .appId(APP_ID)
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

    for (TagTestData test : tests) {
      Delegate delegate = delegateBuilder.tags(test.getDelegateTags()).build();
      when(delegateService.get(ACCOUNT_ID, DELEGATE_ID, false)).thenReturn(delegate);

      DelegateTask delegateTask = delegateTaskBuilder.tags(test.getTaskTags()).build();
      BatchDelegateSelectionLog batch = BatchDelegateSelectionLog.builder().taskId(delegateTask.getUuid()).build();
      assertThat(assignDelegateService.canAssign(batch, DELEGATE_ID, delegateTask)).isEqualTo(test.isAssignable());

      verify(delegateSelectionLogsService, Mockito.times(test.getNumOfMissingAllSelectorsInvocations()))
          .logMissingAllSelectors(batch, ACCOUNT_ID, DELEGATE_ID);
      verify(delegateSelectionLogsService, Mockito.times(test.getNumOfMissingSelectorInvocations()))
          .logMissingSelector(eq(batch), eq(ACCOUNT_ID), eq(DELEGATE_ID), anyString());
    }

    delegateTaskBuilder.envId(ENV_ID);
    delegateBuilder.excludeScopes(
        ImmutableList.of(DelegateScope.builder().environmentTypes(ImmutableList.of(PROD)).build()));

    for (TagTestData test : tests) {
      Delegate delegate = delegateBuilder.tags(test.getDelegateTags()).build();
      when(delegateService.get(ACCOUNT_ID, DELEGATE_ID, false)).thenReturn(delegate);

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
    when(featureFlagService.isEnabled(DELEGATE_TAGS_EXTENDED, ACCOUNT_ID)).thenReturn(true);

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
                                                  .appId(APP_ID)
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

    List<DelegateConnectionResult> saved =
        wingsPersistence.createQuery(DelegateConnectionResult.class).filter(ACCOUNT_ID_KEY, ACCOUNT_ID).asList();
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
                                    .appId(APP_ID)
                                    .envId(ENV_ID)
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
                                    .appId(APP_ID)
                                    .envId(ENV_ID)
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
                                    .appId(APP_ID)
                                    .envId(ENV_ID)
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

    DelegateTask delegateTask = DelegateTask.builder()
                                    .accountId(ACCOUNT_ID)
                                    .appId(APP_ID)
                                    .envId(ENV_ID)
                                    .infrastructureMappingId(infrastructureMapping.getUuid())
                                    .data(TaskData.builder().async(true).timeout(DEFAULT_ASYNC_CALL_TIMEOUT).build())
                                    .build();

    DelegateTask delegateTask2 = DelegateTask.builder()
                                     .accountId(ACCOUNT_ID)
                                     .appId(APP_ID)
                                     .envId(ENV_ID)
                                     .infrastructureMappingId(WRONG_INFRA_MAPPING_ID)
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
}
