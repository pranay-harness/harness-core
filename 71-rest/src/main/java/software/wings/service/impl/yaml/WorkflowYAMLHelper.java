package software.wings.service.impl.yaml;

import static io.harness.exception.WingsException.USER;
import static io.harness.expression.ExpressionEvaluator.matchesVariablePattern;
import static io.harness.validation.Validator.notNullCheck;
import static software.wings.beans.EntityType.ENVIRONMENT;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.persistence.NameAccess;
import io.harness.persistence.UuidAccess;
import org.jetbrains.annotations.Nullable;
import software.wings.beans.EntityType;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.InfrastructureDefinitionService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.SettingsService;
import software.wings.settings.SettingValue.SettingVariableTypes;

@Singleton
public class WorkflowYAMLHelper {
  @Inject EnvironmentService environmentService;
  @Inject ArtifactStreamService artifactStreamService;
  @Inject InfrastructureMappingService infraMappingService;
  @Inject InfrastructureDefinitionService infrastructureDefinitionService;
  @Inject ServiceResourceService serviceResourceService;
  @Inject SettingsService settingsService;

  public String getWorkflowVariableValueBean(
      String accountId, String envId, String appId, String entityType, String variableValue) {
    if (matchesVariablePattern(variableValue) || entityType == null) {
      return variableValue;
    }
    EntityType entityTypeEnum = EntityType.valueOf(entityType);
    if (ENVIRONMENT.equals(entityTypeEnum)) {
      return null;
    }

    UuidAccess uuidAccess = getUuidAccess(accountId, envId, appId, variableValue, entityTypeEnum);
    if (uuidAccess != null) {
      return uuidAccess.getUuid();
    } else {
      return variableValue;
    }
  }

  public String getWorkflowVariableValueYaml(String appId, String entryValue, EntityType entityType) {
    if (matchesVariablePattern(entryValue) || entityType == null) {
      return entryValue;
    }
    NameAccess x = getNameAccess(appId, entryValue, entityType);
    if (x != null) {
      return x.getName();
    } else {
      return entryValue;
    }
  }

  @Nullable
  private NameAccess getNameAccess(String appId, String entryValue, EntityType entityType) {
    switch (entityType) {
      case ENVIRONMENT:
        return environmentService.get(appId, entryValue, false);
      case SERVICE:
        return serviceResourceService.get(appId, entryValue, false);
      case INFRASTRUCTURE_MAPPING:
        return infraMappingService.get(appId, entryValue);
      case INFRASTRUCTURE_DEFINITION:
        return infrastructureDefinitionService.get(appId, entryValue);
      case CF_AWS_CONFIG_ID:
      case HELM_GIT_CONFIG_ID:
        return settingsService.get(entryValue);
      default:
        return null;
    }
  }

  @Nullable
  private UuidAccess getUuidAccess(
      String accountId, String envId, String appId, String variableValue, EntityType entityType) {
    UuidAccess uuidAccess;
    switch (entityType) {
      case SERVICE:
        uuidAccess = serviceResourceService.getServiceByName(appId, variableValue, false);
        notNullCheck("Service [" + variableValue + "] does not exist", uuidAccess, USER);
        break;
      case INFRASTRUCTURE_MAPPING:
        uuidAccess = infraMappingService.getInfraMappingByName(appId, envId, variableValue);
        notNullCheck(
            "Service Infrastructure [" + variableValue + "] does not exist for the environment", uuidAccess, USER);
        break;
      case INFRASTRUCTURE_DEFINITION:
        uuidAccess = infrastructureDefinitionService.getInfraDefByName(appId, envId, variableValue);
        notNullCheck(
            "Service Infrastructure [" + variableValue + "] does not exist for the environment", uuidAccess, USER);
        break;
      case CF_AWS_CONFIG_ID:
        uuidAccess = settingsService.fetchSettingAttributeByName(accountId, variableValue, SettingVariableTypes.AWS);
        notNullCheck(
            "Aws Cloud Provider [" + variableValue + "] associated to the Cloud Formation State does not exist",
            uuidAccess, USER);
        break;
      case HELM_GIT_CONFIG_ID:
        uuidAccess = settingsService.fetchSettingAttributeByName(accountId, variableValue, SettingVariableTypes.GIT);
        notNullCheck(
            "Git Connector [" + variableValue + "] associated to the Helm State does not exist", uuidAccess, USER);
        break;
      default:
        return null;
    }

    return uuidAccess;
  }
}
