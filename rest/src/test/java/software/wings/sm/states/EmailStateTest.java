package software.wings.sm.states;

import static org.assertj.core.api.Assertions.assertThat;
import static org.joor.Reflect.on;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static software.wings.api.EmailStateExecutionData.Builder.anEmailStateExecutionData;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.helpers.ext.mail.EmailData.Builder.anEmailData;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;

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
import software.wings.helpers.ext.mail.EmailData;
import software.wings.service.intfc.EmailNotificationService;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.WorkflowStandardParams;

import java.io.IOException;
import javax.inject.Inject;

/**
 * The Class EmailStateTest.
 *
 * @author paggarwal.
 */
public class EmailStateTest extends WingsBaseTest {
  private static final String stateName = "emailState1";
  private static final EmailStateExecutionData.Builder expected =
      anEmailStateExecutionData().withToAddress("to1,to2").withCcAddress("cc1,cc2").withSubject("subject").withBody(
          "body");
  private final EmailData emailData = anEmailData()
                                          .withAccountId(ACCOUNT_ID)
                                          .withTo(Lists.newArrayList("to1", "to2"))
                                          .withCc(Lists.newArrayList("cc1", "cc2"))
                                          .withSubject("subject")
                                          .withBody("body")
                                          .withSystem(true)
                                          .build();
  @Inject private Injector injector;
  @Mock private EmailNotificationService emailNotificationService;
  @InjectMocks private EmailState emailState = new EmailState(stateName);
  private ExecutionContextImpl context;

  /**
   * Sets the up context and state.
   */
  @Before
  public void setUpContextAndState() {
    StateExecutionInstance stateExecutionInstance = new StateExecutionInstance();
    stateExecutionInstance.setUuid(UUIDGenerator.getUuid());
    stateExecutionInstance.setStateName(stateName);

    context = new ExecutionContextImpl(stateExecutionInstance, null, injector);
    WorkflowStandardParams workflowStandardParams = new WorkflowStandardParams();
    on(workflowStandardParams).set("app", anApplication().withAccountId(ACCOUNT_ID).build());
    context.pushContextElement(workflowStandardParams);

    HostElement host = new HostElement();
    host.setHostName("app123.application.com");
    context.pushContextElement(host);

    emailState.setToAddress("to1,to2");
    emailState.setCcAddress("cc1,cc2");
  }

  /**
   * Should send email.
   *
   * @throws EmailException    the email exception
   * @throws TemplateException the template exception
   * @throws IOException       Signals that an I/O exception has occurred.
   */
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

    verify(emailNotificationService).send(emailData);
  }

  /**
   * Should evaluate context elements for email subject and body.
   *
   * @throws EmailException    the email exception
   * @throws TemplateException the template exception
   * @throws IOException       Signals that an I/O exception has occurred.
   */
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
        .send(

            anEmailData()
                .withTo(Lists.newArrayList("to1", "to2"))
                .withAccountId(ACCOUNT_ID)
                .withCc(Lists.newArrayList("cc1", "cc2"))
                .withSubject("Deployed app123.application.com")
                .withBody("Deployed to host app123.application.com")
                .withSystem(true)
                .build());
  }

  /**
   * Should capture error message when failed to send email.
   *
   * @throws EmailException    the email exception
   * @throws TemplateException the template exception
   * @throws IOException       Signals that an I/O exception has occurred.
   */
  @Test
  public void shouldCaptureErrorMessageWhenFailedToSendEmail() throws EmailException, TemplateException, IOException {
    doThrow(new EmailException("Test exception")).when(emailNotificationService).send(emailData);

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

    verify(emailNotificationService).send(emailData);
  }

  /**
   * Should return execution result as error when not ignored.
   *
   * @throws EmailException    the email exception
   * @throws TemplateException the template exception
   * @throws IOException       Signals that an I/O exception has occurred.
   */
  @Test
  public void shouldReturnExecutionResultAsErrorWhenNotIgnored() throws EmailException, TemplateException, IOException {
    doThrow(new EmailException("Test exception")).when(emailNotificationService).send(emailData);

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

    verify(emailNotificationService).send(emailData);
  }
}
