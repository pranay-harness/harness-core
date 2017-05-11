package software.wings.service.impl.appdynamics;

import static software.wings.beans.DelegateTask.Context.Builder.aContext;

import software.wings.beans.AppDynamicsConfig;
import software.wings.beans.Base;
import software.wings.beans.DelegateTask.Context;
import software.wings.beans.ErrorCode;
import software.wings.beans.SettingAttribute;
import software.wings.delegatetasks.DelegateProxyFactory;
import software.wings.exception.WingsException;
import software.wings.service.intfc.appdynamics.AppdynamicsDeletegateService;
import software.wings.service.intfc.appdynamics.AppdynamicsService;

import java.io.IOException;
import java.util.List;
import javax.inject.Inject;

/**
 * Created by rsingh on 4/17/17.
 */
public class AppdynamicsServiceImpl implements AppdynamicsService {
  @Inject private DelegateProxyFactory delegateProxyFactory;

  @Override
  public List<AppdynamicsApplicationResponse> getApplications(final SettingAttribute settingAttribute)
      throws IOException {
    Context context = aContext().withAccountId(settingAttribute.getAccountId()).withAppId(Base.GLOBAL_APP_ID).build();
    return delegateProxyFactory.get(AppdynamicsDeletegateService.class, context)
        .getAllApplications((AppDynamicsConfig) settingAttribute.getValue());
  }

  @Override
  public void validateConfig(final SettingAttribute settingAttribute) {
    try {
      Context context = aContext().withAccountId(settingAttribute.getAccountId()).withAppId(Base.GLOBAL_APP_ID).build();
      delegateProxyFactory.get(AppdynamicsDeletegateService.class, context)
          .validateConfig((AppDynamicsConfig) settingAttribute.getValue());
    } catch (Exception e) {
      throw new WingsException(ErrorCode.APPDYNAMICS_CONFIGURATION_ERROR, "reason", e.getMessage());
    }
  }
}
