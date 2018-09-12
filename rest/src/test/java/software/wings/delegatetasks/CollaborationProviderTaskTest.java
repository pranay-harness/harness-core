package software.wings.delegatetasks;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static software.wings.beans.DelegateTask.Builder.aDelegateTask;

import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import software.wings.WingsBaseTest;
import software.wings.beans.TaskType;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;
import software.wings.helpers.ext.external.comm.CollaborationProviderRequest;
import software.wings.helpers.ext.external.comm.CollaborationProviderRequest.CommunicationType;
import software.wings.helpers.ext.external.comm.CollaborationProviderResponse;
import software.wings.helpers.ext.external.comm.EmailRequest;
import software.wings.helpers.ext.external.comm.handlers.EmailHandler;
import software.wings.helpers.ext.mail.EmailData;
import software.wings.helpers.ext.mail.Mailer;
import software.wings.helpers.ext.mail.SmtpConfig;
import software.wings.waitnotify.NotifyResponseData;

import java.util.List;

public class CollaborationProviderTaskTest extends WingsBaseTest {
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

  @Mock Mailer mailer;

  @InjectMocks EmailHandler emailHandler;

  @InjectMocks
  private CollaborationProviderTask collaborationProviderTask =
      (CollaborationProviderTask) TaskType.COLLABORATION_PROVIDER_TASK.getDelegateRunnableTask(
          "delid1", aDelegateTask().build(), notifyResponseData -> {}, () -> true);

  @Test
  public void testEmailNotification() {
    EmailRequest emailRequest = new EmailRequest();
    collaborationProviderTask.run(new Object[] {emailRequest});
  }

  @Test
  public void testFailure() {
    doThrow(new WingsException(ErrorCode.EMAIL_FAILED))
        .when(mailer)
        .send(any(SmtpConfig.class), any(List.class), any(EmailData.class));
    EmailRequest emailRequest = new EmailRequest();
    NotifyResponseData response = collaborationProviderTask.run(new Object[] {emailRequest});
    assertThat(response).isInstanceOf(CollaborationProviderResponse.class);
    assertThat(((CollaborationProviderResponse) response).getStatus()).isEqualTo(CommandExecutionStatus.FAILURE);
  }

  @Test
  public void testAllSupportedCommunicationTypes() {
    CollaborationProviderRequest request = Mockito.mock(CollaborationProviderRequest.class);
    for (CommunicationType type : CommunicationType.values()) {
      when(request.getCommunicationType()).thenReturn(type);
      collaborationProviderTask.run(new Object[] {request});
    }
  }
}
