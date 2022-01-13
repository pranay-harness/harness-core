/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.azure.impl;

import static io.harness.rule.OwnerRule.IVAN;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.whenNew;

import io.harness.CategoryTest;
import io.harness.azure.AzureClient;
import io.harness.azure.model.AzureConfig;
import io.harness.category.element.UnitTests;
import io.harness.network.Http;
import io.harness.rule.Owner;

import com.google.common.util.concurrent.TimeLimiter;
import com.microsoft.azure.Page;
import com.microsoft.azure.PagedList;
import com.microsoft.azure.credentials.ApplicationTokenCredentials;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.monitor.ActivityLogs;
import com.microsoft.azure.management.monitor.ActivityLogs.ActivityLogsQueryDefinitionStages.WithActivityLogsQueryExecute;
import com.microsoft.azure.management.monitor.ActivityLogs.ActivityLogsQueryDefinitionStages.WithActivityLogsSelectFilter;
import com.microsoft.azure.management.monitor.ActivityLogs.ActivityLogsQueryDefinitionStages.WithEventDataEndFilter;
import com.microsoft.azure.management.monitor.ActivityLogs.ActivityLogsQueryDefinitionStages.WithEventDataFieldFilter;
import com.microsoft.azure.management.monitor.ActivityLogs.ActivityLogsQueryDefinitionStages.WithEventDataStartTimeFilter;
import com.microsoft.azure.management.monitor.EventData;
import com.microsoft.rest.LogLevel;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Azure.class, AzureClient.class, Http.class, TimeLimiter.class})
@PowerMockIgnore({"javax.security.*", "javax.net.*"})
public class AzureMonitorClientImplTest extends CategoryTest {
  @Mock private Azure.Configurable configurable;
  @Mock private Azure.Authenticated authenticated;
  @Mock private Azure azure;

  @InjectMocks AzureMonitorClientImpl azureMonitorClient;

  @Before
  public void before() throws Exception {
    ApplicationTokenCredentials tokenCredentials = mock(ApplicationTokenCredentials.class);
    whenNew(ApplicationTokenCredentials.class).withAnyArguments().thenReturn(tokenCredentials);
    when(tokenCredentials.getToken(anyString())).thenReturn("tokenValue");
    mockStatic(Azure.class);
    when(Azure.configure()).thenReturn(configurable);
    when(configurable.withLogLevel(any(LogLevel.class))).thenReturn(configurable);
    when(configurable.authenticate(any(ApplicationTokenCredentials.class))).thenReturn(authenticated);
    when(authenticated.withDefaultSubscription()).thenReturn(azure);
    when(authenticated.withSubscription(anyString())).thenReturn(azure);
  }

  @Test
  @Owner(developers = IVAN)
  @Category(UnitTests.class)
  public void testListEventDataWithAllPropertiesByResourceId() {
    DateTime endDate = DateTime.now();
    DateTime startDate = endDate.minusMinutes(1);
    String subscriptionId = "subscriptionId";
    String resourceId = "resourceId";
    AzureConfig azureConfig =
        AzureConfig.builder().clientId("clientId").tenantId("tenantId").key("key".toCharArray()).build();
    ActivityLogs activityLogs = mock(ActivityLogs.class);
    WithEventDataStartTimeFilter query = mock(WithEventDataStartTimeFilter.class);
    WithEventDataEndFilter queryWithStartDate = mock(WithEventDataEndFilter.class);
    WithEventDataFieldFilter queryWithStartAndEndDate = mock(WithEventDataFieldFilter.class);
    WithActivityLogsSelectFilter queryWithStartAndEndDateAndProperties = mock(WithActivityLogsSelectFilter.class);
    WithActivityLogsQueryExecute queryExecute = mock(WithActivityLogsQueryExecute.class);
    EventData eventData = mock(EventData.class);
    PagedList<EventData> eventDataList = getPageList();
    eventDataList.add(eventData);

    when(azure.activityLogs()).thenReturn(activityLogs);
    when(activityLogs.defineQuery()).thenReturn(query);
    when(query.startingFrom(any())).thenReturn(queryWithStartDate);
    when(queryWithStartDate.endsBefore(any())).thenReturn(queryWithStartAndEndDate);
    when(queryWithStartAndEndDate.withAllPropertiesInResponse()).thenReturn(queryWithStartAndEndDateAndProperties);
    when(queryWithStartAndEndDateAndProperties.filterByResource(resourceId)).thenReturn(queryExecute);
    when(queryExecute.execute()).thenReturn(eventDataList);

    List<EventData> response = azureMonitorClient.listEventDataWithAllPropertiesByResourceId(
        azureConfig, subscriptionId, startDate, endDate, resourceId);

    assertThat(response).isNotNull();
    assertThat(response.size()).isEqualTo(1);
    assertThat(response.get(0)).isInstanceOf(EventData.class);
  }

  @Test
  @Owner(developers = IVAN)
  @Category(UnitTests.class)
  public void testListEventDataWithAllPropertiesByResourceIdWithException() {
    DateTime endDate = DateTime.now();
    DateTime startDate = endDate.minusMinutes(1);
    AzureConfig azureConfig =
        AzureConfig.builder().clientId("clientId").tenantId("tenantId").key("key".toCharArray()).build();

    assertThatThrownBy(()
                           -> azureMonitorClient.listEventDataWithAllPropertiesByResourceId(
                               azureConfig, null, startDate, endDate, null))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @NotNull
  public <T> PagedList<T> getPageList() {
    return new PagedList<T>() {
      @Override
      public Page<T> nextPage(String s) {
        return new Page<T>() {
          @Override
          public String nextPageLink() {
            return null;
          }
          @Override
          public List<T> items() {
            return null;
          }
        };
      }
    };
  }
}
