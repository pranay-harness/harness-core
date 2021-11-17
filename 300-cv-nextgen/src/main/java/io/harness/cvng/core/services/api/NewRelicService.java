package io.harness.cvng.core.services.api;

import io.harness.cvng.beans.MetricPackDTO;
import io.harness.cvng.beans.newrelic.NewRelicApplication;
import io.harness.cvng.core.beans.MetricPackValidationResponse;

import java.util.List;

public interface NewRelicService {
  List<String> getNewRelicEndpoints();
  List<NewRelicApplication> getNewRelicApplications(String accountId, String connectorIdentifier, String orgIdentifier,
      String projectIdentifier, String filter, String tracingId);
  MetricPackValidationResponse validateData(String accountId, String connectorIdentifier, String orgIdentifier,
      String projectIdentifier, String appName, String appId, List<MetricPackDTO> metricPacks, String tracingId);
}
