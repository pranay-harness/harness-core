package io.harness.azure.client;

import com.microsoft.azure.management.monitor.EventData;
import io.harness.azure.model.AzureConfig;
import org.joda.time.DateTime;

import java.util.List;

public interface AzureMonitorClient {
  /**
   * List event data with all properties by resource id.
   *
   * @param azureConfig
   * @param startTime
   * @param endTime
   * @param resourceId
   * @return
   */
  List<EventData> listEventDataWithAllPropertiesByResourceId(
      AzureConfig azureConfig, DateTime startTime, DateTime endTime, String resourceId);
}
