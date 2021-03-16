package io.harness.cvng.core.services.api;

import io.harness.cvng.beans.newrelic.NewRelicApplication;

import java.util.List;

public interface NewRelicService {
  List<String> getNewRelicEndpoints();
  List<NewRelicApplication> getNewRelicApplications(String accountId, String connectorIdentifier, String orgIdentifier,
      String projectIdentifier, String filter, String tracingId);
}
