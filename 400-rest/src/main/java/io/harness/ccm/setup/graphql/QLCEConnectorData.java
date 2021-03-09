package io.harness.ccm.setup.graphql;

import software.wings.graphql.schema.type.QLObject;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@Scope(PermissionAttribute.ResourceType.CE_CONNECTOR)
public class QLCEConnectorData implements QLObject {
  private List<QLCEConnector> ceConnectors;
}
