package software.wings.licensing.violations.checkers;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import lombok.extern.slf4j.Slf4j;
import software.wings.beans.AccountType;
import software.wings.beans.FeatureEnabledViolation;
import software.wings.beans.FeatureUsageViolation.Usage;
import software.wings.beans.FeatureViolation;
import software.wings.beans.sso.SSOSettings;
import software.wings.beans.sso.SSOType;
import software.wings.licensing.violations.FeatureViolationChecker;
import software.wings.licensing.violations.RestrictedFeature;
import software.wings.service.intfc.SSOSettingService;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Singleton
@Slf4j
public class SSOViolationChecker implements FeatureViolationChecker {
  private SSOSettingService ssoSettingService;

  @Inject
  public SSOViolationChecker(SSOSettingService ssoSettingService) {
    this.ssoSettingService = ssoSettingService;
  }

  @Override
  public List<FeatureViolation> getViolationsForCommunityAccount(String accountId) {
    logger.info(
        "Checking flow control violations for accountId={} and targetAccountType={}", accountId, AccountType.COMMUNITY);
    List<SSOSettings> ssoSettingsList = this.ssoSettingService.getAllSsoSettings(accountId);

    List<Usage> flowControlUsageList =
        ssoSettingsList.stream()
            .filter(s -> SSOType.SAML.equals(s.getType()) || SSOType.LDAP.equals(s.getType()))
            .map(s
                -> Usage.builder()
                       .entityId(s.getUuid())
                       .entityName(s.getDisplayName())
                       .entityType(s.getType().name())
                       .build())
            .collect(Collectors.toList());

    if (isNotEmpty(flowControlUsageList)) {
      logger.info("Found {} SSO violations for accountId={} and targetAccountType={}", flowControlUsageList.size(),
          accountId, AccountType.COMMUNITY);
      return Collections.singletonList(new FeatureEnabledViolation(RestrictedFeature.SSO, flowControlUsageList.size()));
    }

    return Collections.emptyList();
  }
}
