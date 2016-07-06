package software.wings.service.intfc;

import org.hibernate.validator.constraints.NotEmpty;
import software.wings.beans.Setup;

/**
 * Created by anubhaw on 6/30/16.
 */
public interface SetupService {
  /**
   * Gets application setup status.
   *
   * @param appId the app id
   * @return the application setup status
   */
  Setup getApplicationSetupStatus(@NotEmpty String appId);

  /**
   * Gets service setup status.
   *
   * @param appId     the app id
   * @param serviceId the service id
   * @return the service setup status
   */
  Setup getServiceSetupStatus(@NotEmpty String appId, @NotEmpty String serviceId);

  /**
   * Gets environment setup status.
   *
   * @param appId the app id
   * @param envId the env id
   * @return the environment setup status
   */
  Setup getEnvironmentSetupStatus(@NotEmpty String appId, @NotEmpty String envId);
}
