package io.harness.redesign.states.email;

import static io.harness.rule.OwnerRule.ALEXEI;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import com.google.inject.Inject;

import io.harness.ambiance.Ambiance;
import io.harness.category.element.UnitTests;
import io.harness.execution.status.NodeExecutionStatus;
import io.harness.rule.Owner;
import io.harness.state.io.StepParameters;
import io.harness.state.io.StepResponse;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.service.intfc.EmailNotificationService;

import java.util.Collections;

public class EmailStepTest extends WingsBaseTest {
  @Mock private EmailNotificationService emailNotificationService;
  @InjectMocks @Inject private EmailStep emailStep;

  @Test
  @Owner(developers = ALEXEI)
  @Category({UnitTests.class})
  public void shouldReturnEmailType() {
    assertThat(emailStep.getType()).isEqualTo(EmailStep.STATE_TYPE);
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category({UnitTests.class})
  public void shouldSendEmail() {
    when(emailNotificationService.send(any())).thenReturn(true);
    testEmailState(false, NodeExecutionStatus.SUCCEEDED);
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category({UnitTests.class})
  public void shouldThrowErrorOnSendEmail() {
    when(emailNotificationService.send(any())).thenThrow(new RuntimeException("Something went wrong"));
    testEmailState(false, NodeExecutionStatus.FAILED);
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category({UnitTests.class})
  public void shouldSkipErrorOnSendEmail() {
    when(emailNotificationService.send(any())).thenThrow(new RuntimeException("Something went wrong"));
    testEmailState(true, NodeExecutionStatus.SUCCEEDED);
  }

  private void testEmailState(boolean ignoreDeliveryFailure, NodeExecutionStatus expectedStatus) {
    Ambiance ambiance = Ambiance.builder().setupAbstraction("accountId", "accountIdValue").build();
    StepParameters emailStepParameters = EmailStepParameters.builder()
                                             .body("body")
                                             .toAddress("toAddress1, toAddress2")
                                             .ccAddress("ccAddress1, ccAddress2")
                                             .subject("subject")
                                             .ignoreDeliveryFailure(ignoreDeliveryFailure)
                                             .build();

    StepResponse stepResponse = emailStep.executeSync(ambiance, emailStepParameters, Collections.emptyList(), null);

    assertThat(stepResponse.getStatus()).isEqualTo(expectedStatus);
  }
}
