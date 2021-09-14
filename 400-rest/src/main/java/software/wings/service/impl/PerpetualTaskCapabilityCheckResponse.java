/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.service.impl;

import static io.harness.annotations.dev.HarnessTeam.DEL;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.delegate.beans.DelegateMetaInfo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@TargetModule(HarnessModule._955_DELEGATE_BEANS)
@OwnedBy(DEL)
public class PerpetualTaskCapabilityCheckResponse implements CapabilityCheckResponse {
  private DelegateMetaInfo delegateMetaInfo;
  private boolean ableToExecutePerpetualTask;
}
