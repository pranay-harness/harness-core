package io.harness.datahandler.models;

import static io.harness.annotations.dev.HarnessModule._955_ACCOUNT_MGMT;
import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.ccm.license.CeLicenseInfo;
import io.harness.limits.ConfiguredLimit;

import software.wings.beans.LicenseInfo;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@OwnedBy(PL)
@Data
@Builder
@TargetModule(_955_ACCOUNT_MGMT)
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
  private Boolean nextgenEnabled;
}
