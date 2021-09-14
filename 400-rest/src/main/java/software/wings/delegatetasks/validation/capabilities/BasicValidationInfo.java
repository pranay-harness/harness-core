/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.delegatetasks.validation.capabilities;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@TargetModule(HarnessModule._930_DELEGATE_TASKS)
public class BasicValidationInfo {
  @NotNull private String accountId;
  @NotNull private String appId;
  @NotNull private String activityId;
  @NotNull private boolean executeOnDelegate;
  @NotNull private String publicDns;
}
