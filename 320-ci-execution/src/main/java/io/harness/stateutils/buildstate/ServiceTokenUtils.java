package io.harness.stateutils.buildstate;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.security.ServiceTokenGenerator;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import java.time.Duration;

@OwnedBy(HarnessTeam.CI)
public class ServiceTokenUtils {
  private final ServiceTokenGenerator serviceTokenGenerator;
  private final String secret;

  @Inject
  public ServiceTokenUtils(ServiceTokenGenerator serviceTokenGenerator, @Named("serviceSecret") String secret) {
    this.serviceTokenGenerator = serviceTokenGenerator;
    this.secret = secret;
  }

  public String getServiceToken() {
    return serviceTokenGenerator.getServiceTokenWithDuration(secret, Duration.ofHours(12));
  }
}
