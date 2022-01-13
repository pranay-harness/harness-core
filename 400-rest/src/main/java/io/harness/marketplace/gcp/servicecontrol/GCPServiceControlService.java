/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.marketplace.gcp.servicecontrol;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.marketplace.gcp.GcpMarketPlaceConstants;

import software.wings.beans.marketplace.gcp.GCPUsageReport;

import com.google.api.services.servicecontrol.v1.ServiceControl;
import com.google.api.services.servicecontrol.v1.model.CheckRequest;
import com.google.api.services.servicecontrol.v1.model.CheckResponse;
import com.google.api.services.servicecontrol.v1.model.MetricValue;
import com.google.api.services.servicecontrol.v1.model.MetricValueSet;
import com.google.api.services.servicecontrol.v1.model.Operation;
import com.google.api.services.servicecontrol.v1.model.ReportRequest;
import com.google.api.services.servicecontrol.v1.model.ReportResponse;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

@OwnedBy(PL)
@Singleton
@Slf4j
public class GCPServiceControlService {
  @Inject private ServiceControlAPIClientBuilder serviceControlAPIClientBuilder;

  public boolean reportUsageDataToGCP(GCPUsageReport gcpUsageReport) {
    Optional<ServiceControl> serviceControlApiMaybe = serviceControlAPIClientBuilder.getInstance();
    ServiceControl serviceControl;
    if (!serviceControlApiMaybe.isPresent()) {
      log.error("GCP_MKT_PLACE couldn't get service control api client !!");
      return false;
    } else {
      serviceControl = serviceControlApiMaybe.get();
    }
    Operation operation = getOperation(gcpUsageReport);
    if (executeCheckRequest(serviceControl, operation) && executeReportRequest(serviceControl, operation)) {
      return true;
    }
    return false;
  }

  public boolean checkServiceControlRequest(GCPUsageReport gcpUsageReport) {
    Optional<ServiceControl> serviceControlApiMaybe = serviceControlAPIClientBuilder.getInstance();
    ServiceControl serviceControl;
    if (!serviceControlApiMaybe.isPresent()) {
      log.error("GCP_MKT_PLACE couldn't get service control api client !!");
      return false;
    } else {
      serviceControl = serviceControlApiMaybe.get();
    }
    Operation operation = getOperation(gcpUsageReport);
    return this.executeCheckRequest(serviceControl, operation);
  }

  private boolean executeCheckRequest(ServiceControl serviceControl, Operation operation) {
    CheckRequest checkRequest = new CheckRequest();
    checkRequest.setOperation(operation);
    try {
      CheckResponse checkResponse =
          serviceControl.services().check(GcpMarketPlaceConstants.SERVICE_NAME, checkRequest).execute();
      log.info("GCP_MKT_PLACE check response {} for operation {} ", checkResponse.toString(), operation.toString());
      if (CollectionUtils.isEmpty(checkResponse.getCheckErrors())) {
        return true;
      }
    } catch (IOException e) {
      log.error("GCP_MKT_PLACE exception in check request {} ", e);
    }
    return false;
  }

  private boolean executeReportRequest(ServiceControl serviceControl, Operation operation) {
    ReportRequest reportRequest = new ReportRequest();
    reportRequest.setOperations(new ArrayList<>(Arrays.asList(operation)));
    try {
      ReportResponse reportResponse =
          serviceControl.services().report(GcpMarketPlaceConstants.SERVICE_NAME, reportRequest).execute();
      log.info(
          "GCP_MKT_PLACE report request {} report response {} ", reportRequest.toString(), reportResponse.toString());
      if (CollectionUtils.isEmpty(reportResponse.getReportErrors())) {
        return true;
      }
    } catch (IOException e) {
      log.error("GCP_MKT_PLACE exception in report request {} ", e);
    }
    return false;
  }

  private Operation getOperation(GCPUsageReport gcpUsageReport) {
    MetricValue metricValue = new MetricValue();
    metricValue.setInt64Value(Long.valueOf(gcpUsageReport.getInstanceUsage()));

    MetricValueSet metricValueSet = new MetricValueSet();
    metricValueSet.setMetricName(GcpMarketPlaceConstants.GCP_METRIC_NAME);
    metricValueSet.setMetricValues(new ArrayList<>(Arrays.asList(metricValue)));

    Operation operation = new Operation();
    operation.setOperationId(gcpUsageReport.getOperationId());
    operation.setOperationName(GcpMarketPlaceConstants.GCP_OPERATION_NAME);
    operation.setConsumerId(gcpUsageReport.getConsumerId());
    operation.setStartTime(gcpUsageReport.getStartTimestamp().toString());
    operation.setEndTime(gcpUsageReport.getEndTimestamp().toString());
    operation.setMetricValueSets(new ArrayList<>(Arrays.asList(metricValueSet)));
    return operation;
  }
}
