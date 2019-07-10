package software.wings.features;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import software.wings.beans.EntityType;
import software.wings.beans.User;
import software.wings.features.api.AbstractPremiumFeature;
import software.wings.features.api.ComplianceByRemovingUsage;
import software.wings.features.api.FeatureRestrictions;
import software.wings.features.api.Usage;
import software.wings.security.authentication.TwoFactorAuthenticationManager;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.UserService;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Singleton
public class TwoFactorAuthenticationFeature extends AbstractPremiumFeature implements ComplianceByRemovingUsage {
  public static final String FEATURE_NAME = "TWO_FACTOR_AUTHENTICATION";

  private final TwoFactorAuthenticationManager twoFactorAuthenticationManager;
  private final UserService userService;

  @Inject
  public TwoFactorAuthenticationFeature(AccountService accountService, FeatureRestrictions featureRestrictions,
      TwoFactorAuthenticationManager twoFactorAuthenticationManager, UserService userService) {
    super(accountService, featureRestrictions);
    this.twoFactorAuthenticationManager = twoFactorAuthenticationManager;
    this.userService = userService;
  }

  @Override
  public boolean removeUsageForCompliance(String accountId, String targetAccountType) {
    if (isUsageCompliantWithRestrictions(accountId, targetAccountType)) {
      return true;
    }

    twoFactorAuthenticationManager.disableTwoFactorAuthentication(accountId);
    return isUsageCompliantWithRestrictions(accountId, targetAccountType);
  }

  @Override
  public boolean isBeingUsed(String accountId) {
    return !getUsages(accountId).isEmpty();
  }

  @Override
  public Collection<Usage> getDisallowedUsages(String accountId, String targetAccountType) {
    if (isAvailable(targetAccountType)) {
      return Collections.emptyList();
    }

    return getUsages(accountId);
  }

  private Collection<Usage> getUsages(String accountId) {
    return getUsersWith2FAEnabled(accountId)
        .stream()
        .map(TwoFactorAuthenticationFeature::toUsage)
        .collect(Collectors.toList());
  }

  private static Usage toUsage(User user) {
    return Usage.builder()
        .entityId(user.getUuid())
        .entityName(user.getEmail())
        .entityType(EntityType.USER.name())
        .build();
  }

  @Override
  public String getFeatureName() {
    return FEATURE_NAME;
  }

  private List<User> getUsersWith2FAEnabled(String accountId) {
    return userService.getUsersWithThisAsPrimaryAccount(accountId)
        .stream()
        .filter(u -> twoFactorAuthenticationManager.isTwoFactorEnabled(accountId, u))
        .collect(Collectors.toList());
  }
}
