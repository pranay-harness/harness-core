package io.harness.datahandler.models;

import io.harness.ccm.license.CeLicenseInfo;
import io.harness.limits.ConfiguredLimit;
import lombok.Builder;
import lombok.Data;
import software.wings.beans.LicenseInfo;

import java.util.List;

@Data
@Builder
public class AccountSummary {
  private String accountId;
  private String accountName;
  private String companyName;
  private List<String> whiteListedDomains;
  private LicenseInfo licenseInfo;
  private CeLicenseInfo ceLicenseInfo;
  private Boolean twoFactorAdminEnforced;
  private Boolean oauthEnabled;
  private Boolean cloudCostEnabled;
  private Boolean is24x7GuardEnabled;
  private Boolean povEnabled;
  private Boolean ceAutoCollectK8sEventsEnabled;
  private List<ConfiguredLimit> limits;
  private Integer numSecretManagers;
  private Integer numDelegates;
}
