package software.wings.service.intfc;

import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.Service;
import software.wings.beans.Setup;

import javax.validation.constraints.NotNull;

/**
 * Created by anubhaw on 6/30/16.
 */
public interface SetupService {
  /**
   * Gets application setup status.
   *
   * @param application the application
   * @return the application setup status
   */
  Setup getApplicationSetupStatus(@NotNull Application application);

  /**
   * Gets service setup status.
   *
   * @param service the service
   * @return the service setup status
   */
  Setup getServiceSetupStatus(@NotNull Service service);

  /**
   * Gets environment setup status.
   *
   * @param environment the environment
   * @return the environment setup status
   */
  Setup getEnvironmentSetupStatus(@NotNull Environment environment);
}
