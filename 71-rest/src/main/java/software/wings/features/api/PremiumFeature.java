package software.wings.features.api;

import java.util.Collection;

public interface PremiumFeature extends RestrictedFeature {
  boolean isAvailableForAccount(String accountId);

  boolean isAvailable(String accountType);

  boolean isBeingUsed(String accountId);

  Collection<Usage> getDisallowedUsages(String accountId, String targetAccountType);
}
