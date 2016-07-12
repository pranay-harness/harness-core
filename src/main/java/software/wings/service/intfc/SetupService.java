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
  Setup getApplicationSetupStatus(@NotNull Application application);

  Setup getServiceSetupStatus(@NotNull Service service);

  Setup getEnvironmentSetupStatus(@NotNull Environment environment);
}
