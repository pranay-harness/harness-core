package io.harness.connector.apis.dto;

import io.harness.delegate.beans.connector.ConnectorCategory;
import io.harness.delegate.beans.connector.ConnectorType;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class ConnectorSummaryDTO {
  String identifier;
  String name;
  String description;

  String accountId;
  String orgId;
  String projectId;

  String accountName;
  String orgName;
  String projectName;

  ConnectorType type;
  List<ConnectorCategory> categories;
  ConnectorConfigSummaryDTO connectorDetials;

  List<String> tags;

  Long createdAt;
  Long lastModifiedAt;
  Long version;

  // todo @deepak: Add the createdBy lastUpdatedBy
}