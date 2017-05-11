package software.wings.service.intfc.appdynamics;

import software.wings.beans.SettingAttribute;
import software.wings.service.impl.appdynamics.AppdynamicsApplicationResponse;

import java.io.IOException;
import java.util.List;

/**
 * Created by rsingh on 4/17/17.
 */
public interface AppdynamicsService {
  List<AppdynamicsApplicationResponse> getApplications(SettingAttribute settingAttribute) throws IOException;

  void validateConfig(SettingAttribute settingAttribute);
}
