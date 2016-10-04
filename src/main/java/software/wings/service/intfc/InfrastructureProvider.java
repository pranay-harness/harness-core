package software.wings.service.intfc;

import software.wings.beans.infrastructure.Host;
import software.wings.beans.infrastructure.Infrastructure.InfrastructureType;
import software.wings.beans.infrastructure.InfrastructureProviderConfig;

import java.util.List;

/**
 * Created by anubhaw on 10/4/16.
 */
public interface InfrastructureProvider {
  /**
   * Infra type supported boolean.
   *
   * @param infrastructureType the infrastructure type
   * @return the boolean
   */
  boolean infraTypeSupported(InfrastructureType infrastructureType);

  /**
   * Gets all host.
   *
   * @param infrastructureProviderConfig the infrastructure provider config
   * @return the all host
   */
  public List<Host> getAllHost(InfrastructureProviderConfig infrastructureProviderConfig);
}
