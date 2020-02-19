package software.wings.sm.states;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.AADITI;
import static io.harness.rule.OwnerRule.GEORGE;
import static io.harness.rule.OwnerRule.RAGHU;
import static io.harness.rule.OwnerRule.SRINIVAS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.joor.Reflect.on;
import static org.mockito.Matchers.any;
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
import io.harness.beans.ExecutionStatus;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.api.EmailStateExecutionData;
import software.wings.api.HostElement;
import software.wings.app.MainConfiguration;
import software.wings.app.PortalConfig;
import software.wings.helpers.ext.mail.EmailData;
import software.wings.helpers.ext.url.SubdomainUrlHelperIntfc;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.EmailNotificationService;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionResponse;
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
  @Mock private SubdomainUrlHelperIntfc subdomainUrlHelper;

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
    on(workflowStandardParams).set("app", anApplication().accountId(ACCOUNT_ID).uuid(APP_ID).build());
    on(workflowStandardParams).set("env", anEnvironment().uuid(ENV_ID).build());
    on(workflowStandardParams).set("configuration", configuration);
    on(workflowStandardParams).set("currentUser", EmbeddedUser.builder().name("admin").build());
    on(workflowStandardParams).set("accountService", accountService);
    on(workflowStandardParams).set("subdomainUrlHelper", subdomainUrlHelper);

    context.pushContextElement(workflowStandardParams);

    HostElement host = HostElement.builder().build();
    host.setHostName("app123.application.com");
    context.pushContextElement(host);

    emailState.setToAddress("to1,to2");
    emailState.setCcAddress("cc1,cc2");

    PortalConfig portalConfig = new PortalConfig();
    portalConfig.setUrl(BASE_URL);
    when(configuration.getPortal()).thenReturn(portalConfig);
    when(subdomainUrlHelper.getPortalBaseUrl(any())).thenReturn("baseUrl");
  }

  /**
   * Should send email.
   */
  @Test
  @Owner(developers = SRINIVAS)
  @Category(UnitTests.class)
  public void shouldSendEmail() {
    emailState.setBody("body");
    emailState.setSubject("subject");

    ExecutionResponse executionResponse = emailState.execute(context);
    assertThat(executionResponse).extracting(ExecutionResponse::getExecutionStatus).isEqualTo(ExecutionStatus.SUCCESS);
    assertThat(executionResponse.getStateExecutionData())
        .isInstanceOf(EmailStateExecutionData.class)
        .isEqualTo(expected.but().build());
    assertThat(executionResponse.getErrorMessage()).isNull();
    assertThat(emailState.getBody()).isNotEmpty();
    assertThat(emailState.getSubject()).isNotEmpty();
    assertThat(emailState.getToAddress()).isNotEmpty();
    assertThat(emailState.getCcAddress()).isNotEmpty();
    assertThat(emailState.isIgnoreDeliveryFailure()).isTrue();

    verify(emailNotificationService).send(emailData);
  }

  /**
   * Should evaluate context elements for email subject and body.
   */
  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void shouldEvaluateContextElementsForEmailSubjectAndBody() {
    emailState.setBody("Deployed to host ${host.hostName}");
    emailState.setSubject("Deployed ${host.hostName}");

    ExecutionResponse executionResponse = emailState.execute(context);
    assertThat(executionResponse).extracting(ExecutionResponse::getExecutionStatus).isEqualTo(ExecutionStatus.SUCCESS);
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
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void shouldCaptureErrorMessageWhenFailedToSendEmail() {
    doThrow(new RuntimeException("Test exception")).when(emailNotificationService).send(emailData);

    emailState.setBody("body");
    emailState.setSubject("subject");

    ExecutionResponse executionResponse = emailState.execute(context);
    assertThat(executionResponse).extracting(ExecutionResponse::getExecutionStatus).isEqualTo(ExecutionStatus.SUCCESS);
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
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void shouldReturnExecutionResultAsErrorWhenNotIgnored() {
    doThrow(new RuntimeException("Test exception")).when(emailNotificationService).send(emailData);

    emailState.setBody("body");
    emailState.setSubject("subject");
    emailState.setIgnoreDeliveryFailure(false);

    ExecutionResponse executionResponse = emailState.execute(context);
    assertThat(executionResponse).extracting(ExecutionResponse::getExecutionStatus).isEqualTo(ExecutionStatus.ERROR);
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
  @Owner(developers = AADITI)
  @Category(UnitTests.class)
  public void shouldRenderDeploymentTriggeredBy() {
    emailState.setBody("Deployment triggered by: ${deploymentTriggeredBy}");
    emailState.setSubject("Deployment triggered by: ${deploymentTriggeredBy}");

    ExecutionResponse executionResponse = emailState.execute(context);
    assertThat(executionResponse).extracting(ExecutionResponse::getExecutionStatus).isEqualTo(ExecutionStatus.SUCCESS);
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
