package io.harness.ngtriggers.utils;

import io.harness.ngtriggers.beans.dto.TriggerDetails;
import io.harness.ngtriggers.eventmapper.filters.dto.FilterRequestData;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;

@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Singleton
public class GitProviderDataObtainmentManager {
  private final Map<String, GitProviderBaseDataObtainer> obtainerMap;

  public void acquireProviderData(FilterRequestData filterRequestData, List<TriggerDetails> triggers) {
    String sourceRepoType = filterRequestData.getWebhookPayloadData().getOriginalEvent().getSourceRepoType();
    if (obtainerMap.containsKey(sourceRepoType)) {
      obtainerMap.get(sourceRepoType).acquireProviderData(filterRequestData, triggers);
    }
  }
}