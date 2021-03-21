package io.harness.connector.helper;

import static io.harness.NGCommonEntityConstants.CONNECTOR_IDENTIFIER_KEY;
import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.logging.AutoLogContext;

@OwnedBy(DX)
public class ConnectorLogContext extends AutoLogContext {
  public ConnectorLogContext(String connectorIdentifier, OverrideBehavior behavior) {
    super(CONNECTOR_IDENTIFIER_KEY, connectorIdentifier, behavior);
  }
}
