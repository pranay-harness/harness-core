package software.wings.features;

import io.harness.ng.core.account.AuthenticationMechanism;

import software.wings.beans.AccountType;
import software.wings.beans.sso.LdapSettings;
import software.wings.features.api.AbstractPremiumFeature;
import software.wings.features.api.ComplianceByRemovingUsage;
import software.wings.features.api.FeatureRestrictions;
import software.wings.features.api.Usage;
import software.wings.security.authentication.OauthProviderType;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.SSOService;
import software.wings.service.intfc.SSOSettingService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;

@Singleton
public class LdapFeature extends AbstractPremiumFeature implements ComplianceByRemovingUsage {
  public static final String FEATURE_NAME = "LDAP";

  private final SSOService ssoService;
  private final SSOSettingService ssoSettingService;

  @Inject
  public LdapFeature(AccountService accountService, FeatureRestrictions featureRestrictions, SSOService ssoService,
      SSOSettingService ssoSettingService) {
    super(accountService, featureRestrictions);
    this.ssoService = ssoService;
    this.ssoSettingService = ssoSettingService;
  }

  @Override
  public boolean removeUsageForCompliance(String accountId, String targetAccountType) {
    if (isUsageCompliantWithRestrictions(accountId, targetAccountType)) {
      return true;
    }
    ssoService.deleteLdapSettings(accountId);

    if (!accountService.get(accountId).isOauthEnabled() && targetAccountType.equals(AccountType.COMMUNITY)) {
      ssoService.uploadOauthConfiguration(accountId, "", EnumSet.allOf(OauthProviderType.class));
      ssoService.setAuthenticationMechanism(accountId, AuthenticationMechanism.OAUTH);
    }

    return isUsageCompliantWithRestrictions(accountId, targetAccountType);
  }

  @Override
  public boolean isBeingUsed(String accountId) {
    return getLdapSettings(accountId) != null;
  }

  private LdapSettings getLdapSettings(String accountId) {
    return ssoSettingService.getLdapSettingsByAccountId(accountId);
  }

  @Override
  public Collection<Usage> getDisallowedUsages(String accountId, String targetAccountType) {
    if (isAvailable(targetAccountType)) {
      return Collections.emptyList();
    }
    if (!isBeingUsed(accountId)) {
      return Collections.emptyList();
    }

    LdapSettings ldapSettings = getLdapSettings(accountId);
    return Collections.singletonList(Usage.builder()
                                         .entityId(ldapSettings.getAccountId())
                                         .entityType(ldapSettings.getDisplayName())
                                         .entityName(ldapSettings.getType().name())
                                         .build());
  }

  @Override
  public String getFeatureName() {
    return FEATURE_NAME;
  }
}
