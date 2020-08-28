package software.wings.service.intfc.azure.delegate;

import com.microsoft.azure.management.monitor.EventData;
import org.joda.time.DateTime;
import software.wings.beans.AzureConfig;

import java.util.List;

public interface AzureMonitorHelperServiceDelegate {
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
