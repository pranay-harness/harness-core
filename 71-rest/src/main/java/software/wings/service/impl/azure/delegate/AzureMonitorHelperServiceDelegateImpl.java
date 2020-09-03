package software.wings.service.impl.azure.delegate;

import static io.harness.azure.model.AzureConstants.AZURE_MANAGEMENT_CLIENT_NULL_VALIDATION_MSG;
import static io.harness.azure.model.AzureConstants.RESOURCE_ID_NAME_NULL_VALIDATION_MSG;
import static org.apache.commons.lang3.StringUtils.isBlank;

import com.google.inject.Singleton;

import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.monitor.EventData;
import io.fabric8.utils.Objects;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import software.wings.beans.AzureConfig;
import software.wings.helpers.ext.azure.AzureHelperService;
import software.wings.service.intfc.azure.delegate.AzureMonitorHelperServiceDelegate;

import java.util.List;

@Singleton
@Slf4j
public class AzureMonitorHelperServiceDelegateImpl
    extends AzureHelperService implements AzureMonitorHelperServiceDelegate {
  @Override
  public List<EventData> listEventDataWithAllPropertiesByResourceId(
      AzureConfig azureConfig, DateTime startTime, DateTime endTime, final String resourceId) {
    if (isBlank(resourceId)) {
      throw new IllegalArgumentException(RESOURCE_ID_NAME_NULL_VALIDATION_MSG);
    }

    Azure azure = getAzureClient(azureConfig);
    Objects.notNull(azure, AZURE_MANAGEMENT_CLIENT_NULL_VALIDATION_MSG);

    logger.debug("Start listing event data with all properties for resourceId {}, startTime {}, endTime: {}",
        resourceId, startTime.toDateTime(), endTime.toDateTime());
    return azure.activityLogs()
        .defineQuery()
        .startingFrom(startTime)
        .endsBefore(endTime)
        .withAllPropertiesInResponse()
        .filterByResource(resourceId)
        .execute();
  }
}
