package software.wings.verification;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.exception.WingsException.USER;
import static io.harness.validation.Validator.notNullCheck;

import com.google.inject.Inject;

import io.harness.exception.HarnessException;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.SettingAttribute;
import software.wings.beans.yaml.ChangeContext;
import software.wings.service.impl.appdynamics.AppdynamicsTier;
import software.wings.service.impl.newrelic.NewRelicApplication;
import software.wings.service.intfc.appdynamics.AppdynamicsService;
import software.wings.sm.StateType;
import software.wings.verification.appdynamics.AppDynamicsCVServiceConfiguration;
import software.wings.verification.appdynamics.AppDynamicsCVServiceConfiguration.AppDynamicsCVConfigurationYaml;

import java.util.List;
import java.util.Set;

@Slf4j
public class AppDynamicsCVConfigurationYamlHandler
    extends CVConfigurationYamlHandler<AppDynamicsCVConfigurationYaml, AppDynamicsCVServiceConfiguration> {
  @Inject AppdynamicsService appdynamicsService;

  @Override
  public AppDynamicsCVConfigurationYaml toYaml(AppDynamicsCVServiceConfiguration bean, String appId) {
    AppDynamicsCVConfigurationYaml yaml = AppDynamicsCVConfigurationYaml.builder().build();
    super.toYaml(yaml, bean);

    yaml.setType(StateType.APP_DYNAMICS.name());

    try {
      NewRelicApplication appdynamicsApp =
          appdynamicsService.getAppDynamicsApplication(bean.getConnectorId(), bean.getAppDynamicsApplicationId());
      if (appdynamicsApp != null) {
        yaml.setAppDynamicsApplicationName(appdynamicsApp.getName());
        AppdynamicsTier appdynamicsTier =
            appdynamicsService.getTier(bean.getConnectorId(), appdynamicsApp.getId(), bean.getTierId());
        if (appdynamicsTier != null) {
          yaml.setTierName(appdynamicsTier.getName());
        }
      }
      if (isEmpty(yaml.getAppDynamicsApplicationName()) || isEmpty(yaml.getTierName())) {
        final String errMsg = String.format(
            "AppDynamics ApplicationName or TierName is empty during conversion to yaml. ApplicationId %s, tierID %s",
            bean.getAppDynamicsApplicationId(), bean.getTierId());
        logger.error(errMsg);
        throw new InvalidRequestException(errMsg);
      }
    } catch (Exception ex) {
      throw new WingsException(ex.getMessage());
    }

    return yaml;
  }

  @Override
  public AppDynamicsCVServiceConfiguration upsertFromYaml(ChangeContext<AppDynamicsCVConfigurationYaml> changeContext,
      List<ChangeContext> changeSetContext) throws HarnessException {
    String yamlFilePath = changeContext.getChange().getFilePath();
    String accountId = changeContext.getChange().getAccountId();
    String appId = yamlHelper.getAppId(accountId, yamlFilePath);

    notNullCheck("Couldn't retrieve app from yaml:" + yamlFilePath, appId, USER);

    String envId = yamlHelper.getEnvironmentId(appId, yamlFilePath);
    String name = yamlHelper.getNameFromYamlFilePath(changeContext.getChange().getFilePath());

    CVConfiguration previous = cvConfigurationService.getConfiguration(name, appId, envId);

    AppDynamicsCVServiceConfiguration bean = AppDynamicsCVServiceConfiguration.builder().build();
    toBean(bean, changeContext, appId);

    if (previous != null) {
      bean.setUuid(previous.getUuid());
      cvConfigurationService.updateConfiguration(bean, appId);
    } else {
      bean.setUuid(generateUuid());
      cvConfigurationService.saveToDatabase(bean, true);
    }

    return bean;
  }

  @Override
  public Class getYamlClass() {
    return AppDynamicsCVConfigurationYaml.class;
  }

  @Override
  public AppDynamicsCVServiceConfiguration get(String accountId, String yamlFilePath) {
    return (AppDynamicsCVServiceConfiguration) yamlHelper.getCVConfiguration(accountId, yamlFilePath);
  }

  private void toBean(AppDynamicsCVServiceConfiguration bean,
      ChangeContext<AppDynamicsCVConfigurationYaml> changeContext, String appId) throws HarnessException {
    AppDynamicsCVConfigurationYaml yaml = changeContext.getYaml();
    String yamlFilePath = changeContext.getChange().getFilePath();
    String accountId = changeContext.getChange().getAccountId();
    SettingAttribute appDConnector = settingsService.getSettingAttributeByName(accountId, yaml.getConnectorName());

    super.toBean(changeContext, bean, appId, yamlFilePath);
    try {
      List<NewRelicApplication> apps = appdynamicsService.getApplications(appDConnector.getUuid());
      NewRelicApplication appDynamicsApp = null;
      for (NewRelicApplication app : apps) {
        if (app.getName().equals(yaml.getAppDynamicsApplicationName())) {
          bean.setAppDynamicsApplicationId(String.valueOf(app.getId()));
          appDynamicsApp = app;
          break;
        }
      }
      if (appDynamicsApp != null) {
        Set<AppdynamicsTier> tiers = appdynamicsService.getTiers(appDConnector.getUuid(), appDynamicsApp.getId());
        for (AppdynamicsTier tier : tiers) {
          if (tier.getName().equals(yaml.getTierName())) {
            bean.setTierId(String.valueOf(tier.getId()));
            break;
          }
        }
      }
      if (isEmpty(bean.getAppDynamicsApplicationId()) || isEmpty(bean.getTierId())) {
        final String errMsg = String.format(
            "AppDynamics ApplicationName or TierName is incorrect during edit from yaml. ApplicationName %s, tierName %s",
            yaml.getAppDynamicsApplicationName(), yaml.getTierName());
        logger.error(errMsg);
        throw new WingsException(errMsg);
      }
    } catch (Exception ex) {
      throw new WingsException(ex.getMessage());
    }
    bean.setStateType(StateType.APP_DYNAMICS);
  }
}
