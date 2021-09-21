package io.harness.signup.data;

import static io.harness.annotations.dev.HarnessModule._950_NG_AUTHENTICATION_SERVICE;
import static io.harness.annotations.dev.HarnessTeam.GTM;

import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@OwnedBy(GTM)
@TargetModule(_950_NG_AUTHENTICATION_SERVICE)
public class UtmInfo {
  private String utmSource;
  private String utmContent;
  private String utmMedium;
  private String utmTerm;
  private String utmCampaign;
}
