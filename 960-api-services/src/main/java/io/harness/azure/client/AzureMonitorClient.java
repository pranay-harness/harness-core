/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

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

  /**
   * List event data with all properties by resource group name.
   *
   * @param azureConfig
   * @param subscriptionId
   * @param resourceGroupName
   * @param startTime
   * @param endTime
   * @return
   */
  List<EventData> listEventDataWithAllPropertiesByResourceGroupName(
      AzureConfig azureConfig, String subscriptionId, String resourceGroupName, DateTime startTime, DateTime endTime);
}
