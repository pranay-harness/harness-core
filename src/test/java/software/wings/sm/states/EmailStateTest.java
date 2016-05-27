package software.wings.sm.states;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static software.wings.api.EmailStateExecutionData.Builder.anEmailStateExecutionData;

import com.google.common.collect.Lists;
import com.google.inject.Injector;

import freemarker.template.TemplateException;
import org.apache.commons.mail.EmailException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.api.EmailStateExecutionData;
import software.wings.api.HostElement;
import software.wings.common.UUIDGenerator;
import software.wings.service.intfc.NotificationService;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.StateExecutionInstance;

import java.io.IOException;

import javax.inject.Inject;

/**
 * @author paggarwal.
 */
public class EmailStateTest extends WingsBaseTest {
  private static final String stateName = "emailState1";
  private static final EmailStateExecutionData.Builder expected =
      anEmailStateExecutionData().withToAddress("to1,to2").withCcAddress("cc1,cc2").withSubject("subject").withBody(
          "body");

  @Inject private Injector injector;

  @Mock private NotificationService<EmailState> emailNotificationService;

  @InjectMocks private EmailState emailState = new EmailState(stateName);

  private ExecutionContextImpl context;

  @Before
  public void setUpContextAndState() {
    StateExecutionInstance stateExecutionInstance = new StateExecutionInstance();
    stateExecutionInstance.setUuid(UUIDGenerator.getUuid());
    stateExecutionInstance.setStateName(stateName);

    context = new ExecutionContextImpl(stateExecutionInstance, null, injector);

    HostElement host = new HostElement();
    host.setHostName("app123.application.com");
    context.pushContextElement(host);

    emailState.setToAddress("to1,to2");
    emailState.setCcAddress("cc1,cc2");
  }

  @Test
  public void shouldSendEmail() throws EmailException, TemplateException, IOException {
    emailState.setBody("body");
    emailState.setSubject("subject");

    ExecutionResponse executionResponse = emailState.execute(context);
    assertThat(executionResponse)
        .extracting(ExecutionResponse::getExecutionStatus)
        .containsExactly(ExecutionStatus.SUCCESS);
    assertThat(executionResponse.getStateExecutionData())
        .isInstanceOf(EmailStateExecutionData.class)
        .isEqualTo(expected.but().build());
    assertThat(executionResponse.getErrorMessage()).isNull();

    verify(emailNotificationService)
        .send(Lists.newArrayList("to1", "to2"), Lists.newArrayList("cc1", "cc2"), "subject", "body");
  }

  @Test
  public void shouldEvaluateContextElementsForEmailSubjectAndBody()
      throws EmailException, TemplateException, IOException {
    emailState.setBody("Deployed to host ${host.hostName}");
    emailState.setSubject("Deployed ${host.hostName}");

    ExecutionResponse executionResponse = emailState.execute(context);
    assertThat(executionResponse)
        .extracting(ExecutionResponse::getExecutionStatus)
        .containsExactly(ExecutionStatus.SUCCESS);
    assertThat(executionResponse.getStateExecutionData())
        .isInstanceOf(EmailStateExecutionData.class)
        .isEqualTo(expected.but()
                       .withSubject("Deployed app123.application.com")
                       .withBody("Deployed to host app123.application.com")
                       .build());
    assertThat(executionResponse.getErrorMessage()).isNull();

    verify(emailNotificationService)
        .send(Lists.newArrayList("to1", "to2"), Lists.newArrayList("cc1", "cc2"), "Deployed app123.application.com",
            "Deployed to host app123.application.com");
  }

  @Test
  public void shouldCaptureErrorMessageWhenFailedToSendEmail() throws EmailException, TemplateException, IOException {
    doThrow(new EmailException("Test exception"))
        .when(emailNotificationService)
        .send(Lists.newArrayList("to1", "to2"), Lists.newArrayList("cc1", "cc2"), "subject", "body");

    emailState.setBody("body");
    emailState.setSubject("subject");

    ExecutionResponse executionResponse = emailState.execute(context);
    assertThat(executionResponse)
        .extracting(ExecutionResponse::getExecutionStatus)
        .containsExactly(ExecutionStatus.SUCCESS);
    assertThat(executionResponse.getStateExecutionData())
        .isInstanceOf(EmailStateExecutionData.class)
        .isEqualTo(expected.but().build());
    assertThat(executionResponse.getErrorMessage()).isNotNull().isEqualTo("Test exception");

    verify(emailNotificationService)
        .send(Lists.newArrayList("to1", "to2"), Lists.newArrayList("cc1", "cc2"), "subject", "body");
  }

  @Test
  public void shouldReturnExecutionResultAsErrorWhenNotIgnored() throws EmailException, TemplateException, IOException {
    doThrow(new EmailException("Test exception"))
        .when(emailNotificationService)
        .send(Lists.newArrayList("to1", "to2"), Lists.newArrayList("cc1", "cc2"), "subject", "body");

    emailState.setBody("body");
    emailState.setSubject("subject");
    emailState.setIgnoreDeliveryFailure(false);

    ExecutionResponse executionResponse = emailState.execute(context);
    assertThat(executionResponse)
        .extracting(ExecutionResponse::getExecutionStatus)
        .containsExactly(ExecutionStatus.ERROR);
    assertThat(executionResponse.getStateExecutionData())
        .isInstanceOf(EmailStateExecutionData.class)
        .isEqualTo(expected.but().build());
    assertThat(executionResponse.getErrorMessage()).isNotNull().isEqualTo("Test exception");

    verify(emailNotificationService)
        .send(Lists.newArrayList("to1", "to2"), Lists.newArrayList("cc1", "cc2"), "subject", "body");
  }
}
