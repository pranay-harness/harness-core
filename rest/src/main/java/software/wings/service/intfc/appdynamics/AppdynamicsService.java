package software.wings.service.intfc.appdynamics;

import software.wings.beans.SettingAttribute;
import software.wings.service.impl.appdynamics.AppdynamicsApplicationResponse;

import java.io.IOException;
import java.util.List;
import javax.validation.constraints.NotNull;

/**
 * Created by rsingh on 4/17/17.
 */
public interface AppdynamicsService {
  List<AppdynamicsApplicationResponse> getApplications(@NotNull String settingId) throws IOException;

  void validateConfig(final SettingAttribute settingAttribute);
}
