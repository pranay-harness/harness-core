package io.harness.azure.client;

import io.harness.azure.model.AzureConfig;

import com.microsoft.azure.management.monitor.EventData;
import java.util.List;
import org.joda.time.DateTime;

public interface AzureMonitorClient {
  /**
   * List event data with all properties by resource id.
   *
   * @param azureConfig
   * @param subscriptionId
   * @param startTime
   * @param endTime
   * @param resourceId
   * @return
   */
  List<EventData> listEventDataWithAllPropertiesByResourceId(
      AzureConfig azureConfig, String subscriptionId, DateTime startTime, DateTime endTime, String resourceId);
}
