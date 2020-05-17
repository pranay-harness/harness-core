package software.wings.delegatetasks;

import static io.harness.delegate.beans.TaskData.DEFAULT_SYNC_CALL_TIMEOUT;
import static io.harness.rule.OwnerRule.ROHITKARELIA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.joor.Reflect.on;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.beans.HostConnectionAttributes.Builder.aHostConnectionAttributes;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.beans.TaskType.HOST_VALIDATION;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.DELEGATE_ID;

import io.harness.beans.DelegateTask;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.TaskData;
import io.harness.rule.Owner;
import jodd.exception.UncheckedException;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.DelegateTaskPackage;
import software.wings.beans.HostValidationTaskParameters;
import software.wings.settings.validation.SshConnectionConnectivityValidationAttributes;
import software.wings.utils.HostValidationService;

import java.util.Arrays;
import java.util.Collections;

public class HostValidationTaskTest extends WingsBaseTest {
  private DelegateTask delegateTask = prepareDelegateTask();
  @Mock private HostValidationService mockHostValidationService;

  @InjectMocks
  private HostValidationTask hostValidationTask = (HostValidationTask) HOST_VALIDATION.getDelegateRunnableTask(
      DelegateTaskPackage.builder().delegateId(DELEGATE_ID).delegateTask(delegateTask).build(),
      notifyResponseData -> {}, () -> true);

  @Before
  public void setUp() throws Exception {
    on(hostValidationTask).set("hostValidationService", mockHostValidationService);
  }

  @Test
  @Owner(developers = ROHITKARELIA)
  @Category(UnitTests.class)
  public void testRun() {
    Object methodReturnValue = hostValidationTask.run(new Object[] {getTaskParameters()});
    verify(mockHostValidationService, times(1)).validateHost(any(), any(), any(), any());
    assertThat(methodReturnValue).isNotNull();
  }

  @Test
  @Owner(developers = ROHITKARELIA)
  @Category(UnitTests.class)
  public void testRunthrowsException() {
    when(mockHostValidationService.validateHost(any(), any(), any(), any())).thenThrow(new UncheckedException());
    hostValidationTask.run(new Object[] {getTaskParameters()});
    verify(mockHostValidationService, times(1)).validateHost(any(), any(), any(), any());
  }

  @Test
  @Owner(developers = ROHITKARELIA)
  @Category(UnitTests.class)
  public void testRunWithObjectParameters() {
    Object[] objectParams = {any(), any(), Arrays.asList("host1"), any(), Collections.emptyList(), any()};
    Object methodReturnValue = hostValidationTask.run(objectParams);
    verify(mockHostValidationService, times(1)).validateHost(any(), any(), any(), any());
    assertThat(methodReturnValue).isNotNull();
  }

  private HostValidationTaskParameters getTaskParameters() {
    return HostValidationTaskParameters.builder()
        .hostNames(Arrays.asList("host1"))
        .connectionSetting(aSettingAttribute()
                               .withAccountId(ACCOUNT_ID)
                               .withValue(aHostConnectionAttributes().build())
                               .withConnectivityValidationAttributes(
                                   SshConnectionConnectivityValidationAttributes.builder().hostName("host1").build())
                               .build())
        .encryptionDetails(Collections.emptyList())
        .build();
  }

  private DelegateTask prepareDelegateTask() {
    return DelegateTask.builder()
        .data(TaskData.builder()
                  .async(true)
                  .timeout(DEFAULT_SYNC_CALL_TIMEOUT)
                  .parameters(new Object[] {getTaskParameters()})
                  .build())
        .build();
  }
}