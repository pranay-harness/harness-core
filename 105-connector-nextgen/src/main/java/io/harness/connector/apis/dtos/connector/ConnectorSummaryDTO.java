package io.harness.connector.apis.dtos.connector;

import io.harness.connector.common.ConnectorCategory;
import io.harness.connector.common.ConnectorType;
import io.harness.connector.entities.Connector;
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

  Connector.Scope scope;
  ConnectorType type;
  List<ConnectorCategory> categories;
  ConnectorConfigSummaryDTO connectorDetials;

  List<String> tags;

  Long createdAt;
  Long lastModifiedAt;
  Long version;

  // todo @deepak: Add the createdBy lastUpdatedBy
}