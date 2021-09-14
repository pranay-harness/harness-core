/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.beans.security.access;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * Wrapper class created for holding all the white list configs for an account. It was needed for CacheManager.
 * @author rktummala on 04/11/2018
 */
@OwnedBy(PL)
@Data
@Builder
public class WhitelistConfig {
  private String accountId;
  private List<Whitelist> whitelistList;
}
