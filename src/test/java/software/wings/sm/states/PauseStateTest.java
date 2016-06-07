package software.wings.sm.states;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static software.wings.api.PauseStateExecutionData.Builder.aPauseStateExecutionData;

import com.google.common.collect.Lists;
import com.google.inject.Injector;

import freemarker.template.TemplateException;
import org.apache.commons.mail.EmailException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.api.PauseStateExecutionData;
import software.wings.common.UUIDGenerator;
import software.wings.service.intfc.NotificationService;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.StateExecutionInstance;

import java.io.IOException;
import javax.inject.Inject;

/**
 * Created by peeyushaggarwal on 6/7/16.
 */
public class PauseStateTest extends WingsBaseTest {
  private static final String stateName = "PauseState1";
  private static final PauseStateExecutionData.Builder expected =
      aPauseStateExecutionData().withToAddress("to1,to2").withCcAddress("cc1,cc2").withSubject("subject").withBody(
          "body");

  @Inject private Injector injector;

  @Mock private NotificationService<EmailState> emailNotificationService;

  @InjectMocks private PauseState pauseState = new PauseState(stateName);

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

    pauseState.setToAddress("to1,to2");
    pauseState.setCcAddress("cc1,cc2");
  }

  @Test
  public void shouldSendEmailAndReturnCorrelationIdOnExecute() throws EmailException, TemplateException, IOException {
    pauseState.setBody("body");
    pauseState.setSubject("subject");

    ExecutionResponse executionResponse = pauseState.execute(context);
    assertThat(executionResponse)
        .extracting(ExecutionResponse::getExecutionStatus)
        .containsExactly(ExecutionStatus.SUCCESS);
    assertThat(executionResponse.getStateExecutionData())
        .isInstanceOf(PauseStateExecutionData.class)
        .isEqualTo(expected.but().build());
    assertThat(executionResponse.getErrorMessage()).isNull();
    assertThat(executionResponse.isAsynch()).isTrue();
    assertThat(executionResponse.getCorrelationIds()).hasSize(1);

    verify(emailNotificationService)
        .send(Lists.newArrayList("to1", "to2"), Lists.newArrayList("cc1", "cc2"), "subject", "body");
  }
}
