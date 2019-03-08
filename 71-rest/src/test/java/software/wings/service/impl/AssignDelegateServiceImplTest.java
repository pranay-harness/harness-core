package software.wings.service.impl;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static software.wings.beans.Base.ACCOUNT_ID_KEY;
import static software.wings.beans.Delegate.Builder.aDelegate;
import static software.wings.beans.Delegate.Status.ENABLED;
import static software.wings.beans.DelegateTask.DEFAULT_ASYNC_CALL_TIMEOUT;
import static software.wings.beans.Environment.Builder.anEnvironment;
import static software.wings.beans.Environment.EnvironmentType.NON_PROD;
import static software.wings.beans.Environment.EnvironmentType.PROD;
import static software.wings.common.Constants.MAX_DELEGATE_LAST_HEARTBEAT;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.DELEGATE_ID;
import static software.wings.utils.WingsTestConstants.ENV_ID;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;

import io.harness.delegate.task.http.HttpTaskParameters;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.Delegate;
import software.wings.beans.DelegateScope;
import software.wings.beans.DelegateTask;
import software.wings.beans.Environment;
import software.wings.beans.TaskType;
import software.wings.delegatetasks.validation.DelegateConnectionResult;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.AssignDelegateService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.EnvironmentService;

import java.time.Clock;
import java.util.List;

/**
 * Created by brett on 7/26/17
 */
public class AssignDelegateServiceImplTest extends WingsBaseTest {
  @Mock private EnvironmentService environmentService;
  @Mock private DelegateService delegateService;

  @Inject @InjectMocks private AssignDelegateService assignDelegateService;

  @Inject private WingsPersistence wingsPersistence;
  @Inject private Clock clock;

  @Before
  public void setUp() {
    Environment environment = anEnvironment().withUuid(ENV_ID).withAppId(APP_ID).withEnvironmentType(PROD).build();
    when(environmentService.get(APP_ID, ENV_ID, false)).thenReturn(environment);
  }

  @Test
  public void shouldAssignDelegateWithNoScope() {
    DelegateTask delegateTask = DelegateTask.builder()
                                    .async(true)
                                    .accountId(ACCOUNT_ID)
                                    .appId(APP_ID)
                                    .envId(ENV_ID)
                                    .timeout(DEFAULT_ASYNC_CALL_TIMEOUT)
                                    .build();
    Delegate delegate = aDelegate()
                            .withAccountId(ACCOUNT_ID)
                            .withUuid(DELEGATE_ID)
                            .withIncludeScopes(emptyList())
                            .withExcludeScopes(emptyList())
                            .build();
    when(delegateService.get(ACCOUNT_ID, DELEGATE_ID, false)).thenReturn(delegate);
    assertThat(assignDelegateService.canAssign(DELEGATE_ID, delegateTask)).isTrue();
  }

  @Test
  public void shouldAssignDelegateWithMatchingIncludeScopes() {
    DelegateTask delegateTask = DelegateTask.builder()
                                    .async(true)
                                    .accountId(ACCOUNT_ID)
                                    .appId(APP_ID)
                                    .envId(ENV_ID)
                                    .timeout(DEFAULT_ASYNC_CALL_TIMEOUT)
                                    .build();
    Delegate delegate = aDelegate()
                            .withAccountId(ACCOUNT_ID)
                            .withUuid(DELEGATE_ID)
                            .withIncludeScopes(ImmutableList.of(
                                DelegateScope.builder().environmentTypes(ImmutableList.of(PROD)).build()))
                            .withExcludeScopes(emptyList())
                            .build();
    when(delegateService.get(ACCOUNT_ID, DELEGATE_ID, false)).thenReturn(delegate);
    assertThat(assignDelegateService.canAssign(DELEGATE_ID, delegateTask)).isTrue();
  }

  @Test
  public void shouldNotAssignDelegateWithoutMatchingIncludeScopes() {
    DelegateTask delegateTask = DelegateTask.builder()
                                    .async(true)
                                    .accountId(ACCOUNT_ID)
                                    .appId(APP_ID)
                                    .envId(ENV_ID)
                                    .timeout(DEFAULT_ASYNC_CALL_TIMEOUT)
                                    .build();
    Delegate delegate = aDelegate()
                            .withAccountId(ACCOUNT_ID)
                            .withUuid(DELEGATE_ID)
                            .withIncludeScopes(ImmutableList.of(
                                DelegateScope.builder().environmentTypes(ImmutableList.of(NON_PROD)).build()))
                            .withExcludeScopes(emptyList())
                            .build();
    when(delegateService.get(ACCOUNT_ID, DELEGATE_ID, false)).thenReturn(delegate);
    assertThat(assignDelegateService.canAssign(DELEGATE_ID, delegateTask)).isFalse();
  }

  @Test
  public void shouldAssignDelegateWithoutMatchingExcludeScopes() {
    DelegateTask delegateTask = DelegateTask.builder()
                                    .async(true)
                                    .accountId(ACCOUNT_ID)
                                    .appId(APP_ID)
                                    .envId(ENV_ID)
                                    .timeout(DEFAULT_ASYNC_CALL_TIMEOUT)
                                    .build();
    Delegate delegate = aDelegate()
                            .withAccountId(ACCOUNT_ID)
                            .withUuid(DELEGATE_ID)
                            .withIncludeScopes(emptyList())
                            .withExcludeScopes(ImmutableList.of(
                                DelegateScope.builder().environmentTypes(ImmutableList.of(NON_PROD)).build()))
                            .build();
    when(delegateService.get(ACCOUNT_ID, DELEGATE_ID, false)).thenReturn(delegate);
    assertThat(assignDelegateService.canAssign(DELEGATE_ID, delegateTask)).isTrue();
  }

  @Test
  public void shouldNotAssignDelegateWithMatchingExcludeScopes() {
    DelegateTask delegateTask = DelegateTask.builder()
                                    .async(true)
                                    .accountId(ACCOUNT_ID)
                                    .appId(APP_ID)
                                    .envId(ENV_ID)
                                    .timeout(DEFAULT_ASYNC_CALL_TIMEOUT)
                                    .build();
    Delegate delegate = aDelegate()
                            .withAccountId(ACCOUNT_ID)
                            .withUuid(DELEGATE_ID)
                            .withIncludeScopes(emptyList())
                            .withExcludeScopes(ImmutableList.of(
                                DelegateScope.builder().environmentTypes(ImmutableList.of(PROD)).build()))
                            .build();
    when(delegateService.get(ACCOUNT_ID, DELEGATE_ID, false)).thenReturn(delegate);
    assertThat(assignDelegateService.canAssign(DELEGATE_ID, delegateTask)).isFalse();
  }

  @Test
  public void shouldAssignTaskWithAllMatchingTags() {
    DelegateTask delegateTask = DelegateTask.builder()
                                    .async(true)
                                    .accountId(ACCOUNT_ID)
                                    .appId(APP_ID)
                                    .taskType(TaskType.SCRIPT.name())
                                    .tags(ImmutableList.of("a", "b"))
                                    .timeout(DEFAULT_ASYNC_CALL_TIMEOUT)
                                    .build();
    Delegate delegate = aDelegate()
                            .withAccountId(ACCOUNT_ID)
                            .withUuid(DELEGATE_ID)
                            .withIncludeScopes(emptyList())
                            .withExcludeScopes(emptyList())
                            .withTags(ImmutableList.of("a", "b", "c"))
                            .build();
    when(delegateService.get(ACCOUNT_ID, DELEGATE_ID, false)).thenReturn(delegate);
    assertThat(assignDelegateService.canAssign(DELEGATE_ID, delegateTask)).isTrue();
  }

  @Test
  public void shouldNotAssignTaskWithPartialMatchingTags() {
    DelegateTask delegateTask = DelegateTask.builder()
                                    .async(true)
                                    .accountId(ACCOUNT_ID)
                                    .appId(APP_ID)
                                    .taskType(TaskType.SCRIPT.name())
                                    .tags(ImmutableList.of("a", "b"))
                                    .timeout(DEFAULT_ASYNC_CALL_TIMEOUT)
                                    .build();
    Delegate delegate = aDelegate()
                            .withAccountId(ACCOUNT_ID)
                            .withUuid(DELEGATE_ID)
                            .withIncludeScopes(emptyList())
                            .withExcludeScopes(emptyList())
                            .withTags(ImmutableList.of("b", "c"))
                            .build();
    when(delegateService.get(ACCOUNT_ID, DELEGATE_ID, false)).thenReturn(delegate);
    assertThat(assignDelegateService.canAssign(DELEGATE_ID, delegateTask)).isFalse();
  }

  @Test
  public void shouldNotAssignTaskWithNoMatchingTags() {
    DelegateTask delegateTask = DelegateTask.builder()
                                    .async(true)
                                    .accountId(ACCOUNT_ID)
                                    .appId(APP_ID)
                                    .taskType(TaskType.SCRIPT.name())
                                    .tags(ImmutableList.of("a", "b"))
                                    .timeout(DEFAULT_ASYNC_CALL_TIMEOUT)
                                    .build();
    Delegate delegate = aDelegate()
                            .withAccountId(ACCOUNT_ID)
                            .withUuid(DELEGATE_ID)
                            .withIncludeScopes(emptyList())
                            .withExcludeScopes(emptyList())
                            .withTags(ImmutableList.of("c", "d"))
                            .build();
    when(delegateService.get(ACCOUNT_ID, DELEGATE_ID, false)).thenReturn(delegate);
    assertThat(assignDelegateService.canAssign(DELEGATE_ID, delegateTask)).isFalse();
  }

  @Test
  public void shouldAssignTaskWithEmptyDelegateTaskTags() {
    DelegateTask delegateTask = DelegateTask.builder()
                                    .async(true)
                                    .accountId(ACCOUNT_ID)
                                    .appId(APP_ID)
                                    .taskType(TaskType.SCRIPT.name())
                                    .tags(null)
                                    .timeout(DEFAULT_ASYNC_CALL_TIMEOUT)
                                    .build();
    Delegate delegate = aDelegate()
                            .withAccountId(ACCOUNT_ID)
                            .withUuid(DELEGATE_ID)
                            .withIncludeScopes(emptyList())
                            .withExcludeScopes(emptyList())
                            .withTags(ImmutableList.of("a", "b", "c"))
                            .build();
    when(delegateService.get(ACCOUNT_ID, DELEGATE_ID, false)).thenReturn(delegate);
    assertThat(assignDelegateService.canAssign(DELEGATE_ID, delegateTask)).isTrue();
  }

  @Test
  public void shouldNotAssignTaskWithEmptyDelegateTags() {
    DelegateTask delegateTask = DelegateTask.builder()
                                    .async(true)
                                    .accountId(ACCOUNT_ID)
                                    .appId(APP_ID)
                                    .taskType(TaskType.SCRIPT.name())
                                    .tags(ImmutableList.of("a", "b"))
                                    .timeout(DEFAULT_ASYNC_CALL_TIMEOUT)
                                    .build();
    Delegate delegate = aDelegate()
                            .withAccountId(ACCOUNT_ID)
                            .withUuid(DELEGATE_ID)
                            .withIncludeScopes(emptyList())
                            .withExcludeScopes(emptyList())
                            .withTags(null)
                            .build();
    when(delegateService.get(ACCOUNT_ID, DELEGATE_ID, false)).thenReturn(delegate);
    assertThat(assignDelegateService.canAssign(DELEGATE_ID, delegateTask)).isFalse();
  }

  @Test
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
  public void shouldBeWhitelisted() {
    wingsPersistence.save(DelegateConnectionResult.builder()
                              .accountId(ACCOUNT_ID)
                              .delegateId(DELEGATE_ID)
                              .criteria("criteria")
                              .validated(true)
                              .build());

    Object[] params = {HttpTaskParameters.builder().url("criteria").build()};

    DelegateTask delegateTask = DelegateTask.builder()
                                    .async(true)
                                    .accountId(ACCOUNT_ID)
                                    .taskType(TaskType.HTTP.name())
                                    .parameters(params)
                                    .timeout(DEFAULT_ASYNC_CALL_TIMEOUT)
                                    .build();

    assertThat(assignDelegateService.isWhitelisted(delegateTask, DELEGATE_ID)).isTrue();
  }

  @Test
  public void shouldNotBeWhitelistedDiffCriteria() {
    wingsPersistence.save(DelegateConnectionResult.builder()
                              .accountId(ACCOUNT_ID)
                              .delegateId(DELEGATE_ID)
                              .criteria("criteria")
                              .validated(true)
                              .build());
    Object[] params = {HttpTaskParameters.builder().url("criteria-other").build()};

    DelegateTask delegateTask = DelegateTask.builder()
                                    .async(true)
                                    .accountId(ACCOUNT_ID)
                                    .taskType(TaskType.HTTP.name())
                                    .parameters(params)
                                    .timeout(DEFAULT_ASYNC_CALL_TIMEOUT)
                                    .build();

    assertThat(assignDelegateService.isWhitelisted(delegateTask, DELEGATE_ID)).isFalse();
  }

  @Test
  public void shouldNotBeWhitelistedWhenNotValidated() {
    wingsPersistence.save(DelegateConnectionResult.builder()
                              .accountId(ACCOUNT_ID)
                              .delegateId(DELEGATE_ID)
                              .criteria("criteria")
                              .validated(false)
                              .build());
    Object[] params = {HttpTaskParameters.builder().url("criteria").build()};

    DelegateTask delegateTask = DelegateTask.builder()
                                    .async(true)
                                    .accountId(ACCOUNT_ID)
                                    .taskType(TaskType.HTTP.name())
                                    .parameters(params)
                                    .timeout(DEFAULT_ASYNC_CALL_TIMEOUT)
                                    .build();

    assertThat(assignDelegateService.isWhitelisted(delegateTask, DELEGATE_ID)).isFalse();
  }

  @Test
  public void shouldGetConnectedWhitelistedDelegates() {
    Delegate delegate = aDelegate()
                            .withAccountId(ACCOUNT_ID)
                            .withUuid(DELEGATE_ID)
                            .withStatus(ENABLED)
                            .withLastHeartBeat(clock.millis())
                            .withConnected(true)
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
                                    .async(true)
                                    .accountId(ACCOUNT_ID)
                                    .taskType(TaskType.HTTP.name())
                                    .parameters(params)
                                    .timeout(DEFAULT_ASYNC_CALL_TIMEOUT)
                                    .build();

    List<String> delegateIds = assignDelegateService.connectedWhitelistedDelegates(delegateTask);

    assertThat(delegateIds.size()).isEqualTo(1);
    assertThat(delegateIds.get(0)).isEqualTo(DELEGATE_ID);
  }

  @Test
  public void shouldNotGetConnectedWhitelistedDelegatesNotValidated() {
    Delegate delegate = aDelegate()
                            .withAccountId(ACCOUNT_ID)
                            .withUuid(DELEGATE_ID)
                            .withStatus(ENABLED)
                            .withLastHeartBeat(clock.millis())
                            .withConnected(true)
                            .build();
    wingsPersistence.save(delegate);
    wingsPersistence.save(DelegateConnectionResult.builder()
                              .accountId(ACCOUNT_ID)
                              .delegateId(DELEGATE_ID)
                              .criteria("criteria")
                              .validated(false)
                              .build());
    when(delegateService.get(ACCOUNT_ID, DELEGATE_ID, false)).thenReturn(delegate);

    Object[] params = {HttpTaskParameters.builder().url("criteria").build()};
    DelegateTask delegateTask = DelegateTask.builder()
                                    .async(true)
                                    .accountId(ACCOUNT_ID)
                                    .taskType(TaskType.HTTP.name())
                                    .parameters(params)
                                    .timeout(DEFAULT_ASYNC_CALL_TIMEOUT)
                                    .build();

    List<String> delegateIds = assignDelegateService.connectedWhitelistedDelegates(delegateTask);

    assertThat(delegateIds.size()).isEqualTo(0);
  }

  @Test
  public void shouldNotGetConnectedWhitelistedDelegatesOldHeartbeat() {
    Delegate delegate = aDelegate()
                            .withAccountId(ACCOUNT_ID)
                            .withUuid(DELEGATE_ID)
                            .withStatus(ENABLED)
                            .withLastHeartBeat(clock.millis() - MAX_DELEGATE_LAST_HEARTBEAT - 1000)
                            .withConnected(true)
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
                                    .async(true)
                                    .accountId(ACCOUNT_ID)
                                    .taskType(TaskType.HTTP.name())
                                    .parameters(params)
                                    .timeout(DEFAULT_ASYNC_CALL_TIMEOUT)
                                    .build();

    List<String> delegateIds = assignDelegateService.connectedWhitelistedDelegates(delegateTask);

    assertThat(delegateIds.size()).isEqualTo(0);
  }

  @Test
  public void shouldNotGetConnectedWhitelistedDelegatesOtherCriteria() {
    Delegate delegate = aDelegate()
                            .withAccountId(ACCOUNT_ID)
                            .withUuid(DELEGATE_ID)
                            .withStatus(ENABLED)
                            .withLastHeartBeat(clock.millis())
                            .withConnected(true)
                            .build();
    wingsPersistence.save(delegate);
    wingsPersistence.save(DelegateConnectionResult.builder()
                              .accountId(ACCOUNT_ID)
                              .delegateId(DELEGATE_ID)
                              .criteria("criteria")
                              .validated(true)
                              .build());
    when(delegateService.get(ACCOUNT_ID, DELEGATE_ID, false)).thenReturn(delegate);

    Object[] params = {HttpTaskParameters.builder().url("criteria-other").build()};
    DelegateTask delegateTask = DelegateTask.builder()
                                    .async(true)
                                    .accountId(ACCOUNT_ID)
                                    .taskType(TaskType.HTTP.name())
                                    .parameters(params)
                                    .timeout(DEFAULT_ASYNC_CALL_TIMEOUT)
                                    .build();

    List<String> delegateIds = assignDelegateService.connectedWhitelistedDelegates(delegateTask);

    assertThat(delegateIds.size()).isEqualTo(0);
  }

  @Test
  public void shouldGetNullFirstAttemptDelegate() {
    Object[] params = {HttpTaskParameters.builder().url("criteria-other").build()};
    DelegateTask delegateTask = DelegateTask.builder()
                                    .async(true)
                                    .accountId(ACCOUNT_ID)
                                    .taskType(TaskType.HTTP.name())
                                    .parameters(params)
                                    .timeout(DEFAULT_ASYNC_CALL_TIMEOUT)
                                    .build();

    String delegateId = assignDelegateService.pickFirstAttemptDelegate(delegateTask);

    assertThat(delegateId).isEqualTo(null);
  }

  @Test
  public void shouldGetFirstAttemptDelegate() {
    Delegate delegate = aDelegate()
                            .withAccountId(ACCOUNT_ID)
                            .withUuid(DELEGATE_ID)
                            .withStatus(ENABLED)
                            .withLastHeartBeat(clock.millis())
                            .withConnected(true)
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
                                    .async(true)
                                    .accountId(ACCOUNT_ID)
                                    .taskType(TaskType.HTTP.name())
                                    .parameters(params)
                                    .timeout(DEFAULT_ASYNC_CALL_TIMEOUT)
                                    .build();

    String delegateId = assignDelegateService.pickFirstAttemptDelegate(delegateTask);

    assertThat(delegateId).isEqualTo(DELEGATE_ID);
  }
}
