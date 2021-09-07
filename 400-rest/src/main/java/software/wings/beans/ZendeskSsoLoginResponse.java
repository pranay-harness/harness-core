package software.wings.beans;

import static io.harness.annotations.dev.HarnessModule._950_NG_AUTHENTICATION_SERVICE;

import io.harness.annotations.dev.TargetModule;

import lombok.Builder;
import lombok.Data;

/**
 * Created by anubhaw on 11/30/17.
 */
@Data
@Builder
@TargetModule(_950_NG_AUTHENTICATION_SERVICE)
public class ZendeskSsoLoginResponse {
  private String redirectUrl;
  private String userId;
}
