package software.wings.helpers.ext.pcf;

import lombok.Builder;
import lombok.Data;
import org.cloudfoundry.operations.CloudFoundryOperations;
import org.cloudfoundry.reactor.ConnectionContext;
import org.cloudfoundry.reactor.DefaultConnectionContext;

@Data
@Builder
class CloudFoundryOperationsWrapper implements AutoCloseable {
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
