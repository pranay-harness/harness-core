/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.beans.connector;

import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;

/**
 * Marker interface for connector heartbeat data which will be sent to delegate via rest call and Validation Handler's
 * validate will be called in perpetual task. If the implementors of this interface must also implement {@link
 * io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander}.
 */
public interface ConnectorValidationParams extends ExecutionCapabilityDemander {
  ConnectorType getConnectorType();
  String getConnectorName();
}
