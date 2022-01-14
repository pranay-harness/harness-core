package io.harness.cdng.servicenow.resources.service;

import io.harness.beans.IdentifierRef;
import io.harness.servicenow.ServiceNowFieldNG;

import java.util.List;

public interface ServiceNowResourceService {
  List<ServiceNowFieldNG> getIssueCreateMetadata(
      IdentifierRef serviceNowConnectorRef, String orgId, String projectId, String ticketType);
}
