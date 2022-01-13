/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.task.executioncapability;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
import io.harness.delegate.task.TaskParameters;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@TargetModule(HarnessModule._920_DELEGATE_SERVICE_BEANS)
public class BatchCapabilityCheckTaskParameters implements TaskParameters {
  private List<CapabilityCheckDetails> capabilityCheckDetailsList;
}
