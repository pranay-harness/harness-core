package io.harness.accesscontrol;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.remote.client.ServiceHttpClientConfig;
import io.harness.secret.ConfigSecret;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@Value
@Builder
@OwnedBy(PL)
@FieldDefaults(makeFinal = false)
public class AccessControlAdminClientConfiguration {
  ServiceHttpClientConfig accessControlServiceConfig;
  @ConfigSecret String accessControlServiceSecret;
  Boolean mockAccessControlService;
}
