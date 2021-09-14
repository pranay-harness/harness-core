/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.beans;

import java.util.Map;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class LicenseUpdateInfo {
  @NotNull private LicenseInfo licenseInfo;
  private Map<String, Map<String, Object>> requiredInfoToComply;
}
