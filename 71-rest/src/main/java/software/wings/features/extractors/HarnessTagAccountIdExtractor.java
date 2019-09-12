package software.wings.features.extractors;

import software.wings.beans.HarnessTag;
import software.wings.features.api.AccountIdExtractor;

public class HarnessTagAccountIdExtractor implements AccountIdExtractor<HarnessTag> {
  @Override
  public String getAccountId(HarnessTag tag) {
    return tag.getAccountId();
  }
}
