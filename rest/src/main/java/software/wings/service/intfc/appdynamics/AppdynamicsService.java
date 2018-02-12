package software.wings.service.intfc.appdynamics;

import software.wings.beans.SettingAttribute;
import software.wings.service.impl.appdynamics.AppdynamicsTier;
import software.wings.service.impl.newrelic.NewRelicApplication;

import java.io.IOException;
import java.util.List;
import javax.validation.constraints.NotNull;

/**
 * Created by rsingh on 4/17/17.
 */
public interface AppdynamicsService {
  List<NewRelicApplication> getApplications(@NotNull String settingId) throws IOException;

  List<AppdynamicsTier> getTiers(String settingId, long appdynamicsAppId) throws IOException;

  void validateConfig(@NotNull SettingAttribute settingAttribute);
}
