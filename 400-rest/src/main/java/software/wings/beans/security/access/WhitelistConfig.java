/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
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
