package software.wings.licensing.violations;

import io.harness.data.structure.CollectionUtils;
import software.wings.beans.AccountType;
import software.wings.beans.FeatureViolation;

import java.util.List;
import javax.validation.constraints.NotNull;

public interface FeatureViolationChecker {
  default List<FeatureViolation> check(@NotNull String accountId, @NotNull String targetAccountType) {
    List<FeatureViolation> featureViolationList = null;

    switch (targetAccountType) {
      case AccountType.COMMUNITY:
        featureViolationList = getViolationsForCommunityAccount(accountId);
        break;
      default:
    }

    return CollectionUtils.emptyIfNull(featureViolationList);
  }

  List<FeatureViolation> getViolationsForCommunityAccount(@NotNull String accountId);
}
