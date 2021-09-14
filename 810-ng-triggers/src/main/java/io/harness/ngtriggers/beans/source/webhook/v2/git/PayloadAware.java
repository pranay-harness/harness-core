/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ngtriggers.beans.source.webhook.v2.git;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ngtriggers.beans.source.webhook.v2.TriggerEventDataCondition;

import java.util.List;

@OwnedBy(PIPELINE)
public interface PayloadAware {
  List<TriggerEventDataCondition> fetchHeaderConditions();
  List<TriggerEventDataCondition> fetchPayloadConditions();
  String fetchJexlCondition();
}
