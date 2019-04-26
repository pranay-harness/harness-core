package software.wings.licensing.violations;

import software.wings.beans.FeatureViolation;

import java.util.List;

public interface FeatureViolationsService {
  List<FeatureViolation> getViolations(String accountId, String targetAccountType);

  List<RestrictedFeature> getRestrictedFeatures(String accountId);
}
