package software.wings.delegatetasks;

import static io.harness.delegate.beans.TaskData.DEFAULT_ASYNC_CALL_TIMEOUT;
import static io.harness.rule.OwnerRule.SATYAM;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.joor.Reflect.on;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static software.wings.beans.HostConnectionAttributes.Builder.aHostConnectionAttributes;
import static software.wings.beans.HostValidationResponse.Builder.aHostValidationResponse;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.beans.TaskType.CONNECTIVITY_VALIDATION;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;

import io.harness.beans.DelegateTask;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.TaskData;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.WinRmConnectionAttributes;
import software.wings.helpers.ext.mail.Mailer;
import software.wings.helpers.ext.mail.SmtpConfig;
import software.wings.settings.validation.ConnectivityValidationDelegateRequest;
import software.wings.settings.validation.SmtpConnectivityValidationAttributes;
import software.wings.settings.validation.SshConnectionConnectivityValidationAttributes;
import software.wings.settings.validation.WinRmConnectivityValidationAttributes;
import software.wings.utils.HostValidationService;

public class ConnectivityValidationTaskTest extends WingsBaseTest {
  @Mock private HostValidationService mockHostValidationService;
  @Mock private Mailer mockMailer;

  @InjectMocks
  private ConnectivityValidationTask task =
      (ConnectivityValidationTask) CONNECTIVITY_VALIDATION.getDelegateRunnableTask("delegateid",
          DelegateTask.builder()
              .data(TaskData.builder().async(true).timeout(DEFAULT_ASYNC_CALL_TIMEOUT).build())
              .build(),
          notifyResponseData -> {}, () -> true);

  @Before
  public void setUp() throws Exception {
    on(task).set("hostValidationService", mockHostValidationService);
  }

  @Test
  @Owner(developers = SATYAM)
  @Category(UnitTests.class)
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
    request = ConnectivityValidationDelegateRequest.builder()
                  .encryptedDataDetails(emptyList())
                  .settingAttribute(aSettingAttribute()
                                        .withAccountId(ACCOUNT_ID)
                                        .withValue(WinRmConnectionAttributes.builder().build())
                                        .withConnectivityValidationAttributes(
                                            WinRmConnectivityValidationAttributes.builder().hostName("host").build())
                                        .build())
                  .build();
    task.run(new Object[] {request});
    verify(mockHostValidationService, times(2)).validateHost(anyList(), any(), anyList(), any());
    request =
        ConnectivityValidationDelegateRequest.builder()
            .encryptedDataDetails(emptyList())
            .settingAttribute(aSettingAttribute()
                                  .withAccountId(ACCOUNT_ID)
                                  .withValue(SmtpConfig.builder().build())
                                  .withConnectivityValidationAttributes(SmtpConnectivityValidationAttributes.builder()
                                                                            .to("harness@harness.io")
                                                                            .body("body")
                                                                            .subject("subject")
                                                                            .build())
                                  .build())
            .build();
    task.run(new Object[] {request});
    verify(mockMailer).send(any(), anyList(), any());
  }
}