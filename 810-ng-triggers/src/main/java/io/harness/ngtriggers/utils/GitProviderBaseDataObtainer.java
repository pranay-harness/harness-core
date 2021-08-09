package io.harness.ngtriggers.utils;

import io.harness.ngtriggers.beans.dto.TriggerDetails;
import io.harness.ngtriggers.eventmapper.filters.dto.FilterRequestData;

import java.util.List;

public interface GitProviderBaseDataObtainer {
  void acquireProviderData(FilterRequestData filterRequestData, List<TriggerDetails> triggers);
}