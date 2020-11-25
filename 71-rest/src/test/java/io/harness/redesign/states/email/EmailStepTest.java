package io.harness.redesign.states.email;

import static io.harness.rule.OwnerRule.ALEXEI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import io.harness.category.element.UnitTests;
import io.harness.pms.ambiance.Ambiance;
import io.harness.pms.execution.Status;
import io.harness.rule.Owner;
import io.harness.state.io.StepInputPackage;
import io.harness.state.io.StepResponse;

import software.wings.WingsBaseTest;
import software.wings.service.intfc.EmailNotificationService;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;

public class EmailStepTest extends WingsBaseTest {
  @Mock private EmailNotificationService emailNotificationService;
  @InjectMocks @Inject private EmailStep emailStep;

  @Test
  @Owner(developers = ALEXEI)
  @Category({UnitTests.class})
  public void shouldReturnEmailType() {
    assertThat(emailStep.getType()).isEqualTo(EmailStep.STEP_TYPE);
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category({UnitTests.class})
  public void shouldSendEmail() {
    when(emailNotificationService.send(any())).thenReturn(true);
    testEmailState(false, Status.SUCCEEDED);
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category({UnitTests.class})
  public void shouldThrowErrorOnSendEmail() {
    when(emailNotificationService.send(any())).thenThrow(new RuntimeException("Something went wrong"));
    testEmailState(false, Status.FAILED);
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category({UnitTests.class})
  public void shouldSkipErrorOnSendEmail() {
    when(emailNotificationService.send(any())).thenThrow(new RuntimeException("Something went wrong"));
    testEmailState(true, Status.SUCCEEDED);
  }

  private void testEmailState(boolean ignoreDeliveryFailure, Status expectedStatus) {
    Ambiance ambiance =
        Ambiance.newBuilder().putAllSetupAbstractions(ImmutableMap.of("accountId", "accountIdValue")).build();
    EmailStepParameters emailStepParameters = EmailStepParameters.builder()
                                                  .body("body")
                                                  .toAddress("toAddress1, toAddress2")
                                                  .ccAddress("ccAddress1, ccAddress2")
                                                  .subject("subject")
                                                  .ignoreDeliveryFailure(ignoreDeliveryFailure)
                                                  .build();

    StepResponse stepResponse =
        emailStep.executeSync(ambiance, emailStepParameters, StepInputPackage.builder().build(), null);

    assertThat(stepResponse.getStatus()).isEqualTo(expectedStatus);
  }
}
