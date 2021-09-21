package io.harness.ng.core.user;

import static io.harness.annotations.dev.HarnessTeam.GTM;

import io.harness.annotations.dev.OwnedBy;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@OwnedBy(GTM)
public class UtmInfo {
  private String utmSource;
  private String utmContent;
  private String utmMedium;
  private String utmTerm;
  private String utmCampaign;
}
