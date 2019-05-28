package software.wings.service.impl.yaml;

import static io.harness.exception.WingsException.USER;
import static io.harness.expression.ExpressionEvaluator.matchesVariablePattern;
import static software.wings.beans.EntityType.CF_AWS_CONFIG_ID;
import static software.wings.beans.EntityType.ENVIRONMENT;
import static software.wings.beans.EntityType.HELM_GIT_CONFIG_ID;
import static software.wings.beans.EntityType.INFRASTRUCTURE_MAPPING;
import static software.wings.beans.EntityType.SERVICE;
import static software.wings.utils.Validator.notNullCheck;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import software.wings.beans.EntityType;
import software.wings.beans.Environment;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.Service;
import software.wings.beans.SettingAttribute;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.SettingsService;
import software.wings.settings.SettingValue.SettingVariableTypes;

@Singleton
public class WorkflowYAMLHelper {
  @Inject EnvironmentService environmentService;
  @Inject ArtifactStreamService artifactStreamService;
  @Inject InfrastructureMappingService infraMappingService;
  @Inject ServiceResourceService serviceResourceService;
  @Inject SettingsService settingsService;

  public String getWorkflowVariableValueBean(
      String accountId, String envId, String appId, String entityType, String variableValue) {
    if (ENVIRONMENT.name().equals(entityType)) {
      // This is already taken care in resolveEnvironmentId method
      return null;
    } else if (SERVICE.name().equals(entityType)) {
      if (matchesVariablePattern(variableValue)) {
        return variableValue;
      }
      Service service = serviceResourceService.getServiceByName(appId, variableValue, false);
      if (service != null) {
        return service.getUuid();
      } else {
        notNullCheck("Service [" + variableValue + "] does not exist", service, USER);
      }
    } else if (INFRASTRUCTURE_MAPPING.name().equals(entityType)) {
      if (matchesVariablePattern(variableValue)) {
        return variableValue;
      }
      InfrastructureMapping infrastructureMapping =
          infraMappingService.getInfraMappingByName(appId, envId, variableValue);
      if (infrastructureMapping != null) {
        return infrastructureMapping.getUuid();
      } else {
        notNullCheck("Service Infrastructure [" + variableValue + "] does not exist for the environment",
            infrastructureMapping, USER);
      }
    } else if (CF_AWS_CONFIG_ID.name().equals(entityType)) {
      if (matchesVariablePattern(variableValue)) {
        return variableValue;
      }
      SettingAttribute settingAttribute =
          settingsService.fetchSettingAttributeByName(accountId, variableValue, SettingVariableTypes.AWS);
      if (settingAttribute != null) {
        return settingAttribute.getUuid();
      } else {
        notNullCheck(
            "Aws Cloud Provider [" + variableValue + "] associated to the Cloud Formation State does not exist",
            settingAttribute, USER);
      }
    } else if (HELM_GIT_CONFIG_ID.name().equals(entityType)) {
      if (matchesVariablePattern(variableValue)) {
        return variableValue;
      }
      SettingAttribute settingAttribute =
          settingsService.fetchSettingAttributeByName(accountId, variableValue, SettingVariableTypes.GIT);
      if (settingAttribute != null) {
        return settingAttribute.getUuid();
      } else {
        notNullCheck("Git Connector [" + variableValue + "] associated to the Helm State does not exist",
            settingAttribute, USER);
      }
    } else {
      return variableValue;
    }

    return null;
  }

  public String getWorkflowVariableValueYaml(String appId, String entryValue, EntityType entityType) {
    if (matchesVariablePattern(entryValue)) {
      return entryValue;
    }
    if (ENVIRONMENT.equals(entityType)) {
      Environment environment = environmentService.get(appId, entryValue, false);
      if (environment != null) {
        return environment.getName();
      }
    } else if (SERVICE.equals(entityType)) {
      Service service = serviceResourceService.get(appId, entryValue, false);
      if (service != null) {
        return service.getName();
      }
    } else if (INFRASTRUCTURE_MAPPING.equals(entityType)) {
      InfrastructureMapping infrastructureMapping = infraMappingService.get(appId, entryValue);
      if (infrastructureMapping != null) {
        return infrastructureMapping.getName();
      }
    } else if (CF_AWS_CONFIG_ID.equals(entityType)) {
      SettingAttribute settingAttribute = settingsService.get(entryValue);
      if (settingAttribute != null) {
        return settingAttribute.getName();
      }
    } else if (HELM_GIT_CONFIG_ID.equals(entityType)) {
      SettingAttribute settingAttribute = settingsService.get(entryValue);
      if (settingAttribute != null) {
        return settingAttribute.getName();
      }
    } else {
      return entryValue;
    }

    return null;
  }
}
