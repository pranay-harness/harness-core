/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ngtriggers.utils;

import io.harness.ngtriggers.beans.dto.TriggerDetails;
import io.harness.ngtriggers.eventmapper.filters.dto.FilterRequestData;

import java.util.List;

public interface GitProviderBaseDataObtainer {
  void acquireProviderData(FilterRequestData filterRequestData, List<TriggerDetails> triggers);
}
