package software.wings.service.intfc;

import org.hibernate.validator.constraints.NotEmpty;
import software.wings.beans.Setup;

/**
 * Created by anubhaw on 6/30/16.
 */
public interface SetupService {
  Setup getApplicationSetupStatus(@NotEmpty String appId);
  Setup getServiceSetupStatus(@NotEmpty String appId, @NotEmpty String serviceId);
  Setup getEnvironmentSetupStatus(@NotEmpty String appId, @NotEmpty String envId);
}
