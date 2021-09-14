/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.pcf.cfsdk;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Data;
import org.cloudfoundry.operations.CloudFoundryOperations;
import org.cloudfoundry.reactor.ConnectionContext;
import org.cloudfoundry.reactor.DefaultConnectionContext;

@Data
@Builder
@OwnedBy(CDP)
public class CloudFoundryOperationsWrapper implements AutoCloseable {
  private CloudFoundryOperations cloudFoundryOperations;
  private ConnectionContext connectionContext;
  private boolean ignorePcfConnectionContextCache;

  @Override
  public void close() {
    if (ignorePcfConnectionContextCache && connectionContext != null) {
      ((DefaultConnectionContext) connectionContext).dispose();
    }
  }
}
