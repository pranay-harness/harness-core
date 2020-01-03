package software.wings.service.intfc.signup;

import static io.harness.rule.OwnerRule.AMAN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import software.wings.WingsBaseTest;

import java.net.URISyntaxException;
import javax.ws.rs.core.Response;

public class SignupServiceHandlerTest extends WingsBaseTest {
  @Mock AzureMarketplaceIntegrationService azureMarketplaceIntegrationService;

  @Inject @InjectMocks SignupService signupService;

  @Test
  @Owner(developers = AMAN)
  @Category(UnitTests.class)
  public void testValidateTokenShouldSucceed() throws URISyntaxException {
    when(azureMarketplaceIntegrationService.validate(Mockito.anyString())).thenReturn(true);
    Response response = signupService.checkValidity("token");
    assertThat(response.getLocation().toString().contains("azure-signup"));
  }

  @Test
  @Owner(developers = AMAN)
  @Category(UnitTests.class)
  public void testValidateTokenShouldFail() throws URISyntaxException {
    when(azureMarketplaceIntegrationService.validate(Mockito.anyString())).thenReturn(true);
    Response response = signupService.checkValidity("token");
    assertThat(response.getLocation().toString().contains("azure-signup"));

    when(azureMarketplaceIntegrationService.validate(Mockito.anyString())).thenThrow(new SignupException("Failed"));
    response = signupService.checkValidity("token");
    assertThat(response.getLocation().toString().contains("azure-signup"));
  }
}
