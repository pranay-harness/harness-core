package software.wings.helpers.ext.pcf;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import lombok.Builder;
import lombok.Data;
import org.cloudfoundry.operations.CloudFoundryOperations;
import org.cloudfoundry.reactor.ConnectionContext;
import org.cloudfoundry.reactor.DefaultConnectionContext;

@Data
@Builder
@TargetModule(HarnessModule._970_API_SERVICES_BEANS)
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
