package software.wings.delegatetasks;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.joor.Reflect.on;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static software.wings.beans.DelegateTask.Builder.aDelegateTask;
import static software.wings.beans.HostConnectionAttributes.Builder.aHostConnectionAttributes;
import static software.wings.beans.HostValidationResponse.Builder.aHostValidationResponse;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.beans.TaskType.CONNECTIVITY_VALIDATION;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.settings.validation.ConnectivityValidationDelegateRequest;
import software.wings.settings.validation.SshConnectionConnectivityValidationAttributes;
import software.wings.utils.HostValidationService;

public class ConnectivityValidationTaskTest extends WingsBaseTest {
  @Mock private HostValidationService mockHostValidationService;

  @InjectMocks
  private ConnectivityValidationTask task =
      (ConnectivityValidationTask) CONNECTIVITY_VALIDATION.getDelegateRunnableTask(
          "delegateid", aDelegateTask().build(), notifyResponseData -> {}, () -> true);

  @Before
  public void setUp() throws Exception {
    on(task).set("hostValidationService", mockHostValidationService);
  }

  @Test
  public void testRun() {
    ConnectivityValidationDelegateRequest request =
        ConnectivityValidationDelegateRequest.builder()
            .encryptedDataDetails(emptyList())
            .settingAttribute(aSettingAttribute()
                                  .withAccountId(ACCOUNT_ID)
                                  .withValue(aHostConnectionAttributes().build())
                                  .withConnectivityValidationAttributes(
                                      SshConnectionConnectivityValidationAttributes.builder().hostName("host").build())
                                  .build())
            .build();
    doReturn(singletonList(aHostValidationResponse().withHostName("host").withStatus("SUCCESS").build()))
        .when(mockHostValidationService)
        .validateHost(anyList(), any(), anyList(), any());
    task.run(new Object[] {request});
    verify(mockHostValidationService).validateHost(anyList(), any(), anyList(), any());
  }
}