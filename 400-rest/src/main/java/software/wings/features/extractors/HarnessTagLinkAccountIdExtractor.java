/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.features.extractors;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import software.wings.beans.HarnessTagLink;
import software.wings.features.api.AccountIdExtractor;

@OwnedBy(PL)
public class HarnessTagLinkAccountIdExtractor implements AccountIdExtractor<HarnessTagLink> {
  @Override
  public String getAccountId(HarnessTagLink tagLink) {
    return tagLink.getAccountId();
  }
}
