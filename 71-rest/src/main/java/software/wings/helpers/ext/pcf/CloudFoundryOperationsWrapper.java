package software.wings.helpers.ext.pcf;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.cloudfoundry.operations.CloudFoundryOperations;
import org.cloudfoundry.reactor.ConnectionContext;
import org.cloudfoundry.reactor.DefaultConnectionContext;
@Data
@Builder
@Slf4j
public class CloudFoundryOperationsWrapper implements AutoCloseable {
  private CloudFoundryOperations cloudFoundryOperations;
  private ConnectionContext connectionContext;
  @Override
  public void close() {
    if (connectionContext != null) {
      ((DefaultConnectionContext) connectionContext).dispose();
    }
  }
}