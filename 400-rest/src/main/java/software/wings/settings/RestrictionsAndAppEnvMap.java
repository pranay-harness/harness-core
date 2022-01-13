/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package software.wings.settings;

import software.wings.security.UsageRestrictions;

import java.util.Map;
import java.util.Set;
import lombok.Builder;
import lombok.Data;

/**
 * This is a wrapper class that needs
 * @author rktummala on 07/26/18
 */
@Data
@Builder
public class RestrictionsAndAppEnvMap {
  private UsageRestrictions usageRestrictions;
  private Map<String, Set<String>> appEnvMap;
}
