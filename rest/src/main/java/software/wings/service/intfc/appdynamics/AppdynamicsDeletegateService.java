package software.wings.service.intfc.appdynamics;

import software.wings.beans.AppDynamicsConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.TaskType;
import software.wings.delegatetasks.DelegateTaskType;
import software.wings.service.impl.appdynamics.AppdynamicsApplicationResponse;

import java.io.IOException;
import java.util.List;

/**
 * Created by rsingh on 4/17/17.
 */
public interface AppdynamicsDeletegateService {
  @DelegateTaskType(TaskType.APPDYNAMICS_APP_TASK)
  List<AppdynamicsApplicationResponse> getAllApplications(final AppDynamicsConfig appDynamicsConfig) throws IOException;
}
