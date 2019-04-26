package software.wings.licensing.violations;

import software.wings.beans.FeatureViolation;

import java.util.List;

public interface FeatureViolationChecker { List<FeatureViolation> check(String accountId, String targetAccountType); }
