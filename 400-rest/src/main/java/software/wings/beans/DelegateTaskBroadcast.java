/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.beans;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import java.util.Set;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@TargetModule(HarnessModule._420_DELEGATE_SERVICE)
public class DelegateTaskBroadcast {
  private String version;
  private String accountId;
  private String taskId;
  private boolean async;
  private String preAssignedDelegateId;
  private Set<String> alreadyTriedDelegates;
}
