package software.wings.sm.states;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static org.assertj.core.api.Assertions.assertThat;
import static org.joor.Reflect.on;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.Environment.Builder.anEnvironment;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ENV_ID;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Injector;

import freemarker.template.TemplateException;
import org.apache.commons.mail.EmailException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.api.EmailStateExecutionData;
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

import java.io.IOException;

/**
 * Created by peeyushaggarwal on 6/7/16.
 */
public class PauseStateTest extends WingsBaseTest {
  private static final String BASE_URL = "https://env.harness.io/";
  private static final String stateName = "PauseState1";
  private static final EmailStateExecutionData.Builder expected =
      EmailStateExecutionData.Builder.anEmailStateExecutionData()
          .withToAddress("to1,to2")
          .withCcAddress("cc1,cc2")
          .withSubject("subject")
          .withBody("body");

  @Inject private Injector injector;

  @Mock private EmailNotificationService emailNotificationService;
  @Mock private AccountService accountService;

  @InjectMocks private PauseState pauseState = new PauseState(stateName);

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
    on(workflowStandardParams).set("accountService", accountService);

    context.pushContextElement(workflowStandardParams);

    pauseState.setToAddress("to1,to2");
    pauseState.setCcAddress("cc1,cc2");

    PortalConfig portalConfig = new PortalConfig();
    portalConfig.setUrl(BASE_URL);
    when(configuration.getPortal()).thenReturn(portalConfig);
  }

  /**
   * Should send email and return correlation id on execute.
   *
   * @throws EmailException    the email exception
   * @throws TemplateException the template exception
   * @throws IOException       the io exception
   */
  @Test
  public void shouldSendEmailAndReturnCorrelationIdOnExecute() throws EmailException, TemplateException, IOException {
    pauseState.setBody("body");
    pauseState.setSubject("subject");

    ExecutionResponse executionResponse = pauseState.execute(context);
    assertThat(executionResponse)
        .extracting(ExecutionResponse::getExecutionStatus)
        .containsExactly(ExecutionStatus.PAUSED);
    assertThat(executionResponse.getStateExecutionData())
        .isInstanceOf(EmailStateExecutionData.class)
        .isEqualTo(expected.but().build());
    assertThat(executionResponse.getErrorMessage()).isNull();

    verify(emailNotificationService)
        .send(EmailData.builder()
                  .to(Lists.newArrayList("to1", "to2"))
                  .accountId(ACCOUNT_ID)
                  .cc(Lists.newArrayList("cc1", "cc2"))
                  .subject("subject")
                  .body("body")
                  .build());
  }
}
