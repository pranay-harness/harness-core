package software.wings.sm.states;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static org.assertj.core.api.Assertions.assertThat;
import static org.joor.Reflect.on;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.api.EmailStateExecutionData.Builder.anEmailStateExecutionData;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.Environment.Builder.anEnvironment;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ENV_ID;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Injector;

import io.harness.beans.EmbeddedUser;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.api.EmailStateExecutionData;
import software.wings.api.HostElement;
import software.wings.app.MainConfiguration;
import software.wings.app.PortalConfig;
import software.wings.helpers.ext.mail.EmailData;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.EmailNotificationService;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.WorkflowStandardParams;

/**
 * The Class EmailStateTest.
 *
 * @author paggarwal.
 */
public class EmailStateTest extends WingsBaseTest {
  private static final String BASE_URL = "https://env.harness.io/";
  private static final String stateName = "emailState1";
  private static final EmailStateExecutionData.Builder expected =
      anEmailStateExecutionData().withToAddress("to1,to2").withCcAddress("cc1,cc2").withSubject("subject").withBody(
          "body");
  private final EmailData emailData = EmailData.builder()
                                          .accountId(ACCOUNT_ID)
                                          .to(Lists.newArrayList("to1", "to2"))
                                          .cc(Lists.newArrayList("cc1", "cc2"))
                                          .subject("subject")
                                          .body("body")
                                          .build();
  @Inject private Injector injector;

  @Mock private EmailNotificationService emailNotificationService;
  @Mock private AccountService accountService;

  @InjectMocks private EmailState emailState = new EmailState(stateName);

  @Mock private MainConfiguration configuration;

  private ExecutionContextImpl context;

  /**
   * Sets the up context and state.
   */
  @Before
  public void setUpContextAndState() {
    StateExecutionInstance stateExecutionInstance = new StateExecutionInstance();
    stateExecutionInstance.setUuid(generateUuid());
    stateExecutionInstance.setDisplayName(stateName);

    context = new ExecutionContextImpl(stateExecutionInstance, null, injector);
    WorkflowStandardParams workflowStandardParams = new WorkflowStandardParams();
    on(workflowStandardParams).set("app", anApplication().withAccountId(ACCOUNT_ID).withUuid(APP_ID).build());
    on(workflowStandardParams).set("env", anEnvironment().withUuid(ENV_ID).build());
    on(workflowStandardParams).set("configuration", configuration);
    on(workflowStandardParams).set("currentUser", EmbeddedUser.builder().name("admin").build());
    on(workflowStandardParams).set("accountService", accountService);

    context.pushContextElement(workflowStandardParams);

    HostElement host = new HostElement();
    host.setHostName("app123.application.com");
    context.pushContextElement(host);

    emailState.setToAddress("to1,to2");
    emailState.setCcAddress("cc1,cc2");

    PortalConfig portalConfig = new PortalConfig();
    portalConfig.setUrl(BASE_URL);
    when(configuration.getPortal()).thenReturn(portalConfig);
  }

  /**
   * Should send email.
   */
  @Test
  public void shouldSendEmail() {
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
   */
  @Test
  public void shouldEvaluateContextElementsForEmailSubjectAndBody() {
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

            EmailData.builder()
                .to(Lists.newArrayList("to1", "to2"))
                .accountId(ACCOUNT_ID)
                .cc(Lists.newArrayList("cc1", "cc2"))
                .subject("Deployed app123.application.com")
                .body("Deployed to host app123.application.com")
                .build());
  }

  /**
   * Should capture error message when failed to send email.
   */
  @Test
  public void shouldCaptureErrorMessageWhenFailedToSendEmail() {
    doThrow(new RuntimeException("Test exception")).when(emailNotificationService).send(emailData);

    emailState.setBody("body");
    emailState.setSubject("subject");

    ExecutionResponse executionResponse = emailState.execute(context);
    assertThat(executionResponse)
        .extracting(ExecutionResponse::getExecutionStatus)
        .containsExactly(ExecutionStatus.SUCCESS);
    assertThat(executionResponse.getStateExecutionData())
        .isInstanceOf(EmailStateExecutionData.class)
        .isEqualTo(expected.but().build());
    assertThat(executionResponse.getErrorMessage()).isNotNull().isEqualTo("RuntimeException: Test exception");

    verify(emailNotificationService).send(emailData);
  }

  /**
   * Should return execution result as error when not ignored.
   */
  @Test
  public void shouldReturnExecutionResultAsErrorWhenNotIgnored() {
    doThrow(new RuntimeException("Test exception")).when(emailNotificationService).send(emailData);

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
    assertThat(executionResponse.getErrorMessage()).isNotNull().isEqualTo("RuntimeException: Test exception");

    verify(emailNotificationService).send(emailData);
  }

  /**
   * Should render deployment triggered by for email subject and body.
   */
  @Test
  public void shouldRenderDeploymentTriggeredBy() {
    emailState.setBody("Deployment triggered by: ${deploymentTriggeredBy}");
    emailState.setSubject("Deployment triggered by: ${deploymentTriggeredBy}");

    ExecutionResponse executionResponse = emailState.execute(context);
    assertThat(executionResponse)
        .extracting(ExecutionResponse::getExecutionStatus)
        .containsExactly(ExecutionStatus.SUCCESS);
    assertThat(executionResponse.getStateExecutionData())
        .isInstanceOf(EmailStateExecutionData.class)
        .isEqualTo(expected.but()
                       .withSubject("Deployment triggered by: admin")
                       .withBody("Deployment triggered by: admin")
                       .build());
    assertThat(executionResponse.getErrorMessage()).isNull();

    verify(emailNotificationService)
        .send(

            EmailData.builder()
                .to(Lists.newArrayList("to1", "to2"))
                .accountId(ACCOUNT_ID)
                .cc(Lists.newArrayList("cc1", "cc2"))
                .subject("Deployment triggered by: admin")
                .body("Deployment triggered by: admin")
                .build());
  }
}
