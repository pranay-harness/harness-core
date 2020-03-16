package software.wings.delegatetasks.validation.capabilitycheck;

import static io.harness.rule.OwnerRule.PRASHANT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.executioncapability.CapabilityResponse;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import software.wings.WingsBaseTest;
import software.wings.delegatetasks.validation.capabilities.SmtpCapability;
import software.wings.helpers.ext.external.comm.handlers.EmailHandler;
import software.wings.helpers.ext.mail.SmtpConfig;

import java.util.ArrayList;

public class SmtpCapabilityCheckTest extends WingsBaseTest {
  private SmtpCapability smtpCapability =
      SmtpCapability.builder().smtpConfig(SmtpConfig.builder().build()).encryptionDetails(new ArrayList<>()).build();

  @Mock private EmailHandler emailHandler;
  @Inject @InjectMocks SmtpCapabilityCheck smtpCapabilityCheck;

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldPerformCapabilityCheck() {
    Mockito.when(emailHandler.validateDelegateConnection(any(), any())).thenReturn(true);
    CapabilityResponse capabilityResponse = smtpCapabilityCheck.performCapabilityCheck(smtpCapability);
    assertThat(capabilityResponse).isNotNull();
    assertThat(capabilityResponse.isValidated()).isTrue();
  }
}