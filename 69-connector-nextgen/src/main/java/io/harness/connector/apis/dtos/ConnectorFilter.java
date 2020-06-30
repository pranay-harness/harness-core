package io.harness.connector.apis.dtos;

import io.harness.connector.common.ConnectorType;
import io.harness.connector.entities.connectivityStatus.ConnectivityStatus;
import io.opencensus.tags.Tags;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class ConnectorFilter {
  String accountId;
  String projectId;
  String orgId;
  ConnectorType type;
  // todo: @deepak Add tags here
  List<Tags> tag;
  Long lastActivity;
  ConnectivityStatus status;
  String name;
}
