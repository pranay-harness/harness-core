package software.wings.service.impl;

import static com.google.common.truth.Truth.assertThat;
import static java.util.Collections.emptyList;
import static org.mockito.Mockito.when;
import static software.wings.beans.Delegate.Builder.aDelegate;
import static software.wings.beans.DelegateTask.Builder.aDelegateTask;
import static software.wings.beans.Environment.Builder.anEnvironment;
import static software.wings.beans.Environment.EnvironmentType.NON_PROD;
import static software.wings.beans.Environment.EnvironmentType.PROD;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.DELEGATE_ID;
import static software.wings.utils.WingsTestConstants.ENV_ID;

import com.google.common.collect.ImmutableList;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.Delegate;
import software.wings.beans.DelegateScope;
import software.wings.beans.DelegateTask;
import software.wings.beans.Environment;
import software.wings.service.intfc.AssignDelegateService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.EnvironmentService;

import javax.inject.Inject;

/**
 * Created by brett on 7/26/17
 */
public class AssignDelegateServiceImplTest extends WingsBaseTest {
  @Mock private EnvironmentService environmentService;
  @Mock private DelegateService delegateService;

  @Inject @InjectMocks private AssignDelegateService assignDelegateService;

  @Before
  public void setUp() {
    Environment environment = anEnvironment().withUuid(ENV_ID).withAppId(APP_ID).withEnvironmentType(PROD).build();
    when(environmentService.get(APP_ID, ENV_ID, false)).thenReturn(environment);
  }

  @Test
  public void shouldAssignDelegateWithNoScope() {
    DelegateTask delegateTask = aDelegateTask().withAccountId(ACCOUNT_ID).withAppId(APP_ID).withEnvId(ENV_ID).build();
    Delegate delegate = aDelegate()
                            .withAccountId(ACCOUNT_ID)
                            .withUuid(DELEGATE_ID)
                            .withIncludeScopes(emptyList())
                            .withExcludeScopes(emptyList())
                            .build();
    when(delegateService.get(ACCOUNT_ID, DELEGATE_ID)).thenReturn(delegate);
    assertThat(assignDelegateService.canAssign(delegateTask, DELEGATE_ID)).isTrue();
  }

  @Test
  public void shouldAssignDelegateWithMatchingIncludeScopes() {
    DelegateTask delegateTask = aDelegateTask().withAccountId(ACCOUNT_ID).withAppId(APP_ID).withEnvId(ENV_ID).build();
    Delegate delegate = aDelegate()
                            .withAccountId(ACCOUNT_ID)
                            .withUuid(DELEGATE_ID)
                            .withIncludeScopes(ImmutableList.of(
                                DelegateScope.builder().environmentTypes(ImmutableList.of(PROD)).build()))
                            .withExcludeScopes(emptyList())
                            .build();
    when(delegateService.get(ACCOUNT_ID, DELEGATE_ID)).thenReturn(delegate);
    assertThat(assignDelegateService.canAssign(delegateTask, DELEGATE_ID)).isTrue();
  }

  @Test
  public void shouldNotAssignDelegateWithoutMatchingIncludeScopes() {
    DelegateTask delegateTask = aDelegateTask().withAccountId(ACCOUNT_ID).withAppId(APP_ID).withEnvId(ENV_ID).build();
    Delegate delegate = aDelegate()
                            .withAccountId(ACCOUNT_ID)
                            .withUuid(DELEGATE_ID)
                            .withIncludeScopes(ImmutableList.of(
                                DelegateScope.builder().environmentTypes(ImmutableList.of(NON_PROD)).build()))
                            .withExcludeScopes(emptyList())
                            .build();
    when(delegateService.get(ACCOUNT_ID, DELEGATE_ID)).thenReturn(delegate);
    assertThat(assignDelegateService.canAssign(delegateTask, DELEGATE_ID)).isFalse();
  }

  @Test
  public void shouldAssignDelegateWithoutMatchingExcludeScopes() {
    DelegateTask delegateTask = aDelegateTask().withAccountId(ACCOUNT_ID).withAppId(APP_ID).withEnvId(ENV_ID).build();
    Delegate delegate = aDelegate()
                            .withAccountId(ACCOUNT_ID)
                            .withUuid(DELEGATE_ID)
                            .withIncludeScopes(emptyList())
                            .withExcludeScopes(ImmutableList.of(
                                DelegateScope.builder().environmentTypes(ImmutableList.of(NON_PROD)).build()))
                            .build();
    when(delegateService.get(ACCOUNT_ID, DELEGATE_ID)).thenReturn(delegate);
    assertThat(assignDelegateService.canAssign(delegateTask, DELEGATE_ID)).isTrue();
  }

  @Test
  public void shouldNotAssignDelegateWithMatchingExcludeScopes() {
    DelegateTask delegateTask = aDelegateTask().withAccountId(ACCOUNT_ID).withAppId(APP_ID).withEnvId(ENV_ID).build();
    Delegate delegate = aDelegate()
                            .withAccountId(ACCOUNT_ID)
                            .withUuid(DELEGATE_ID)
                            .withIncludeScopes(emptyList())
                            .withExcludeScopes(ImmutableList.of(
                                DelegateScope.builder().environmentTypes(ImmutableList.of(PROD)).build()))
                            .build();
    when(delegateService.get(ACCOUNT_ID, DELEGATE_ID)).thenReturn(delegate);
    assertThat(assignDelegateService.canAssign(delegateTask, DELEGATE_ID)).isFalse();
  }
}
