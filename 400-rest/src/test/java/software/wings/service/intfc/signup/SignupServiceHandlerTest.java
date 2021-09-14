/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.service.intfc.signup;

import static io.harness.annotations.dev.HarnessModule._950_NG_SIGNUP;
import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.rule.OwnerRule.AMAN;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.category.element.UnitTests;
import io.harness.exception.SignupException;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;

import com.google.inject.Inject;
import java.net.URISyntaxException;
import javax.ws.rs.core.Response;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;

@OwnedBy(PL)
@TargetModule(_950_NG_SIGNUP)
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
