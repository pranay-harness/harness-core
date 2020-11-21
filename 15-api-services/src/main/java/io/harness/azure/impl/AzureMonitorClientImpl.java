package io.harness.azure.impl;

import static io.harness.azure.model.AzureConstants.RESOURCE_ID_NAME_NULL_VALIDATION_MSG;

import static org.apache.commons.lang3.StringUtils.isBlank;

import io.harness.azure.AzureClient;
import io.harness.azure.client.AzureMonitorClient;
import io.harness.azure.model.AzureConfig;

import com.google.inject.Singleton;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.monitor.EventData;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;

@Singleton
@Slf4j
public class AzureMonitorClientImpl extends AzureClient implements AzureMonitorClient {
  @Override
  public List<EventData> listEventDataWithAllPropertiesByResourceId(
      AzureConfig azureConfig, String subscriptionId, DateTime startTime, DateTime endTime, final String resourceId) {
    if (isBlank(resourceId)) {
      throw new IllegalArgumentException(RESOURCE_ID_NAME_NULL_VALIDATION_MSG);
    }

    Azure azure = getAzureClient(azureConfig, subscriptionId);

    log.debug("Start listing event data with all properties for resourceId {}, startTime {}, endTime: {}", resourceId,
        startTime.toDateTime(), endTime.toDateTime());
    return azure.activityLogs()
        .defineQuery()
        .startingFrom(startTime)
        .endsBefore(endTime)
        .withAllPropertiesInResponse()
        .filterByResource(resourceId)
        .execute();
  }
}
