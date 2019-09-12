package software.wings.features.extractors;

import software.wings.beans.HarnessTagLink;
import software.wings.features.api.AccountIdExtractor;

public class HarnessTagLinkAccountIdExtractor implements AccountIdExtractor<HarnessTagLink> {
  @Override
  public String getAccountId(HarnessTagLink tagLink) {
    return tagLink.getAccountId();
  }
}
