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
