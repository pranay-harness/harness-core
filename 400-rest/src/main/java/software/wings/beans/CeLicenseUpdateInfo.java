/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.beans;

import io.harness.ccm.license.CeLicenseInfo;

import java.util.Map;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CeLicenseUpdateInfo {
  @NotNull private CeLicenseInfo ceLicenseInfo;
  private Map<String, Map<String, Object>> requiredInfoToComply;
}
