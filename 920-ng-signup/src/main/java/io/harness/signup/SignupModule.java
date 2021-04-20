package io.harness.signup;

import static io.harness.annotations.dev.HarnessTeam.GTM;

import io.harness.annotations.dev.OwnedBy;
import io.harness.remote.client.ServiceHttpClientConfig;
import io.harness.signup.services.SignupService;
import io.harness.signup.services.impl.SignupServiceImpl;
import io.harness.user.UserClientModule;

import com.google.inject.AbstractModule;

@OwnedBy(GTM)
public class SignupModule extends AbstractModule {
  private final ServiceHttpClientConfig serviceHttpClientConfig;
  private final String managerServiceSecret;
  private final String clientId;

  public SignupModule(ServiceHttpClientConfig serviceHttpClientConfig, String managerServiceSecret, String clientId) {
    this.serviceHttpClientConfig = serviceHttpClientConfig;
    this.managerServiceSecret = managerServiceSecret;
    this.clientId = clientId;
  }

  @Override
  protected void configure() {
    install(UserClientModule.getInstance(serviceHttpClientConfig, managerServiceSecret, clientId));
    bind(SignupService.class).to(SignupServiceImpl.class);
  }
}