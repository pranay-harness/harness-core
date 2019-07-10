package software.wings.features;

import com.google.inject.Inject;

import software.wings.features.api.AbstractRestrictedFeature;
import software.wings.features.api.FeatureRestrictions;
import software.wings.features.api.FeatureUsageComplianceReport;
import software.wings.service.intfc.AccountService;

import java.util.Optional;

public class AuditTrailFeature extends AbstractRestrictedFeature {
  public static final String FEATURE_NAME = "AUDIT_TRAIL";

  @Inject
  public AuditTrailFeature(AccountService accountService, FeatureRestrictions featureRestrictions) {
    super(accountService, featureRestrictions);
  }

  @Override
  public String getFeatureName() {
    return FEATURE_NAME;
  }

  public Optional<Integer> getRetentionPeriodInDays(String accountId) {
    return Optional.ofNullable((Integer) getRestrictions(accountId).get("retentionPeriodInDays"));
  }

  @Override
  public boolean isUsageCompliantWithRestrictions(String accountId, String targetAccountType) {
    return true;
  }

  @Override
  public FeatureUsageComplianceReport getUsageComplianceReport(String accountId, String targetAccountType) {
    return FeatureUsageComplianceReport.builder().featureName(FEATURE_NAME).build();
  }
}
