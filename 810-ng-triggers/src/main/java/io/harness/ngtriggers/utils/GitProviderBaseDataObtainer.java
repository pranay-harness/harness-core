package io.harness.ngtriggers.utils;

import io.harness.ngtriggers.eventmapper.filters.dto.FilterRequestData;

public interface GitProviderBaseDataObtainer {
  void acquireProviderData(FilterRequestData filterRequestData);
}