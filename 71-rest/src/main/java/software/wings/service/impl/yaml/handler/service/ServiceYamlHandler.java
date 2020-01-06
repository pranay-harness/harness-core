package software.wings.service.impl.yaml.handler.service;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.USER;
import static io.harness.validation.Validator.notNullCheck;
import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static software.wings.beans.Environment.GLOBAL_ENV_ID;
import static software.wings.beans.ServiceVariable.Type.ARTIFACT;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.exception.EncryptDecryptException;
import lombok.extern.slf4j.Slf4j;
import software.wings.api.DeploymentType;
import software.wings.beans.AllowedValueYaml;
import software.wings.beans.AppContainer;
import software.wings.beans.Application;
import software.wings.beans.EntityType;
import software.wings.beans.FeatureName;
import software.wings.beans.NameValuePair;
import software.wings.beans.Service;
import software.wings.beans.Service.Yaml;
import software.wings.beans.Service.Yaml.YamlBuilder;
import software.wings.beans.ServiceVariable;
import software.wings.beans.ServiceVariable.ServiceVariableBuilder;
import software.wings.beans.ServiceVariable.Type;
import software.wings.beans.yaml.ChangeContext;
import software.wings.service.impl.yaml.handler.ArtifactVariableYamlHelper;
import software.wings.service.impl.yaml.handler.BaseYamlHandler;
import software.wings.service.impl.yaml.handler.ServiceVariableYamlHelper;
import software.wings.service.impl.yaml.service.YamlHelper;
import software.wings.service.intfc.AppContainerService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.ServiceVariableService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.utils.ArtifactType;
import software.wings.utils.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author rktummala on 10/22/17
 */
@Singleton
@Slf4j
public class ServiceYamlHandler extends BaseYamlHandler<Yaml, Service> {
  @Inject YamlHelper yamlHelper;
  @Inject ServiceResourceService serviceResourceService;
  @Inject ServiceVariableService serviceVariableService;
  @Inject SecretManager secretManager;
  @Inject AppContainerService appContainerService;
  @Inject AppService appService;
  @Inject FeatureFlagService featureFlagService;
  @Inject ArtifactVariableYamlHelper artifactVariableYamlHelper;
  @Inject ServiceVariableYamlHelper serviceVariableYamlHelper;

  @Override
  public Yaml toYaml(Service service, String appId) {
    List<NameValuePair.Yaml> nameValuePairList =
        convertToNameValuePair(service.getServiceVariables(), service.getAccountId());
    AppContainer appContainer = service.getAppContainer();
    String applicationStack = appContainer != null ? appContainer.getName() : null;
    String deploymentType = service.getDeploymentType() != null ? service.getDeploymentType().name() : null;

    YamlBuilder yamlBuilder = Yaml.builder()
                                  .harnessApiVersion(getHarnessApiVersion())
                                  .description(service.getDescription())
                                  .artifactType(service.getArtifactType().name())
                                  .deploymentType(deploymentType)
                                  .configMapYaml(service.getConfigMapYaml())
                                  .configVariables(nameValuePairList)
                                  .applicationStack(applicationStack);

    Yaml yaml = yamlBuilder.build();
    updateYamlWithAdditionalInfo(service, appId, yaml);

    return yaml;
  }

  private List<NameValuePair.Yaml> convertToNameValuePair(List<ServiceVariable> serviceVariables, String accountId) {
    if (serviceVariables == null) {
      return Lists.newArrayList();
    }

    return serviceVariables.stream()
        .map(serviceVariable -> {
          List<AllowedValueYaml> allowedValueYamlList = new ArrayList<>();
          Type variableType = serviceVariable.getType();
          String value = null;
          if (Type.ENCRYPTED_TEXT == variableType) {
            try {
              value = secretManager.getEncryptedYamlRef(serviceVariable);
            } catch (IllegalAccessException e) {
              throw new EncryptDecryptException(
                  format("Could not find yaml ref for %s", serviceVariable.getValue()), e);
            }
          } else if (Type.TEXT == variableType) {
            if (serviceVariable.getValue() != null) {
              value = String.valueOf(serviceVariable.getValue());
            }
          } else if (ARTIFACT == variableType) {
            serviceVariableYamlHelper.convertArtifactVariableToYaml(accountId, serviceVariable, allowedValueYamlList);
          } else {
            logger.warn("Variable type {} not supported, skipping the processing of value", variableType);
          }

          return NameValuePair.Yaml.builder()
              .valueType(variableType.name())
              .value(value)
              .name(serviceVariable.getName())
              .allowedValueYamlList(allowedValueYamlList)
              .build();
        })
        .collect(toList());
  }

  @Override
  public Service upsertFromYaml(ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext) {
    String yamlFilePath = changeContext.getChange().getFilePath();
    String accountId = changeContext.getChange().getAccountId();
    String appId = yamlHelper.getAppId(accountId, yamlFilePath);
    notNullCheck("appId null for given yaml file:" + yamlFilePath, appId, USER);

    String serviceName = yamlHelper.getServiceName(yamlFilePath);

    Yaml yaml = changeContext.getYaml();

    Service currentService = new Service();
    currentService.setAppId(appId);
    currentService.setAccountId(accountId);
    currentService.setName(serviceName);
    currentService.setDescription(yaml.getDescription());
    currentService.setConfigMapYaml(yaml.getConfigMapYaml());

    String applicationStack = yaml.getApplicationStack();
    if (isNotBlank(applicationStack)) {
      AppContainer appContainer = appContainerService.getByName(accountId, applicationStack);
      notNullCheck("No application stack found with the given name: " + applicationStack, appContainer, USER);
      currentService.setAppContainer(appContainer);
    }
    Service previousService = get(accountId, yamlFilePath);

    boolean syncFromGit = changeContext.getChange().isSyncFromGit();
    currentService.setSyncFromGit(syncFromGit);

    if (previousService != null) {
      currentService.setUuid(previousService.getUuid());
      // Service Variables are updated first. Consider a scenario where we are adding 1000 new variables.
      // If the service is updated first, then the change set created because of this may not contain all service
      // variables. Lets assume that only 200 variables were added. Now after the change set is pushed to git, it will
      // come back to harness. There is a chance that this change set may delete the newly added service
      // variables if the remaining 800 variables are not added yet. This does not apply when new service is created
      // as this is not allowed in harness UI. If the service is created in git, then we wont be pushing it to git
      // again.
      saveOrUpdateServiceVariables(yaml, previousService.getServiceVariables(), previousService.getAppId(),
          previousService.getUuid(), syncFromGit);

      currentService = serviceResourceService.update(currentService, true);

    } else {
      ArtifactType artifactType = Utils.getEnumFromString(ArtifactType.class, yaml.getArtifactType());
      currentService.setArtifactType(artifactType);
      if (isNotBlank(yaml.getDeploymentType())) {
        DeploymentType deploymentType = Utils.getEnumFromString(DeploymentType.class, yaml.getDeploymentType());
        currentService.setDeploymentType(deploymentType);
      }
      currentService =
          serviceResourceService.save(currentService, true, serviceResourceService.hasInternalCommands(currentService));
      saveOrUpdateServiceVariables(yaml, emptyList(), currentService.getAppId(), currentService.getUuid(), syncFromGit);
    }

    changeContext.setEntity(currentService);
    return currentService;
  }

  @Override
  public Class getYamlClass() {
    return Yaml.class;
  }

  @Override
  public Service get(String accountId, String yamlFilePath) {
    String appId = yamlHelper.getAppId(accountId, yamlFilePath);
    return yamlHelper.getService(appId, yamlFilePath);
  }

  private List<NameValuePair.Yaml> getConfigVariablesToAdd(
      Yaml yaml, Map<String, ServiceVariable> serviceVariablesMap) {
    List<NameValuePair.Yaml> configVarsToAdd = new ArrayList<>();

    if (isEmpty(yaml.getConfigVariables())) {
      return configVarsToAdd;
    }

    for (NameValuePair.Yaml configVar : yaml.getConfigVariables()) {
      if (!serviceVariablesMap.containsKey(configVar.getName())) {
        configVarsToAdd.add(configVar);
      }
    }

    return configVarsToAdd;
  }

  private List<NameValuePair.Yaml> getConfigVariablesToUpdate(
      Yaml yaml, Map<String, ServiceVariable> serviceVariablesMap) {
    List<NameValuePair.Yaml> configVarsToUpdate = new ArrayList<>();

    if (isEmpty(yaml.getConfigVariables())) {
      return configVarsToUpdate;
    }

    for (NameValuePair.Yaml configVar : yaml.getConfigVariables()) {
      if (serviceVariablesMap.containsKey(configVar.getName())) {
        ServiceVariable serviceVariable = serviceVariablesMap.get(configVar.getName());

        switch (serviceVariable.getType()) {
          case TEXT:
            if (configVar.getValue() == null
                || !Arrays.equals(configVar.getValue().toCharArray(), serviceVariable.getValue())) {
              configVarsToUpdate.add(configVar);
            }
            break;

          case ENCRYPTED_TEXT:
          case ARTIFACT:
            configVarsToUpdate.add(configVar);
            break;

          default:
            logger.warn(
                format("Unhandled type %s while finding config variables to update", serviceVariable.getType()));
        }
      }
    }

    return configVarsToUpdate;
  }

  private List<ServiceVariable> getConfigVariablesToDelete(
      Yaml yaml, Map<String, ServiceVariable> serviceVariablesMap) {
    List<ServiceVariable> configVarsToDelete = new ArrayList<>();

    if (isEmpty(serviceVariablesMap)) {
      return configVarsToDelete;
    }

    Map<String, NameValuePair.Yaml> configVariablesFromYamlMap = new HashMap<>();
    if (isNotEmpty(yaml.getConfigVariables())) {
      for (NameValuePair.Yaml configVar : yaml.getConfigVariables()) {
        configVariablesFromYamlMap.put(configVar.getName(), configVar);
      }
    }

    for (Entry<String, ServiceVariable> entry : serviceVariablesMap.entrySet()) {
      if (!configVariablesFromYamlMap.containsKey(entry.getKey())) {
        configVarsToDelete.add(entry.getValue());
      }
    }

    return configVarsToDelete;
  }

  private void saveOrUpdateServiceVariables(Yaml updatedYaml, List<ServiceVariable> previousServiceVariables,
      String appId, String serviceId, boolean syncFromGit) {
    Map<String, ServiceVariable> serviceVariableMap =
        previousServiceVariables.stream().collect(Collectors.toMap(ServiceVariable::getName, identity()));

    List<NameValuePair.Yaml> configVarsToAdd = getConfigVariablesToAdd(updatedYaml, serviceVariableMap);
    List<ServiceVariable> configVarsToDelete = getConfigVariablesToDelete(updatedYaml, serviceVariableMap);
    List<NameValuePair.Yaml> configVarsToUpdate = getConfigVariablesToUpdate(updatedYaml, serviceVariableMap);

    // We are passing true for syncFromGit because we don't want to generate multiple yaml change sets if user is adding
    // many service variables through yaml. After performing all add/update/delete, we will call pushToGit which will
    // push everything to git
    ServiceVariable lastUpdatedServiceVariable = null;
    String accountId = appService.get(appId).getAccountId();

    try {
      // Delete service variables
      for (ServiceVariable serviceVariable : configVarsToDelete) {
        serviceVariableService.delete(appId, serviceVariable.getUuid(), true);
        lastUpdatedServiceVariable = serviceVariable;
      }

      // Add service variables
      for (NameValuePair.Yaml yaml : configVarsToAdd) {
        ServiceVariable serviceVariable =
            serviceVariableService.save(createNewServiceVariable(accountId, appId, serviceId, yaml), true);
        lastUpdatedServiceVariable = serviceVariable;
      }

      // Update service variables
      lastUpdatedServiceVariable =
          updateServiceVariables(accountId, configVarsToUpdate, serviceVariableMap, lastUpdatedServiceVariable);

    } finally {
      if (!syncFromGit && lastUpdatedServiceVariable != null) {
        serviceVariableService.pushServiceVariablesToGit(lastUpdatedServiceVariable);
      }
    }
  }

  private ServiceVariable updateServiceVariables(String accountId, List<NameValuePair.Yaml> configVarsToUpdate,
      Map<String, ServiceVariable> serviceVariableMap, ServiceVariable lastUpdatedServiceVariable) {
    for (NameValuePair.Yaml configVar : configVarsToUpdate) {
      ServiceVariable serviceVariable = serviceVariableMap.get(configVar.getName());
      String value = configVar.getValue();

      switch (serviceVariable.getType()) {
        case TEXT:
          serviceVariable.setValue(value != null ? value.toCharArray() : null);
          break;

        case ENCRYPTED_TEXT:
          String encryptedRecordId = yamlHelper.extractEncryptedRecordId(value);
          serviceVariable.setEncryptedValue(encryptedRecordId);
          serviceVariable.setValue(isBlank(encryptedRecordId) ? null : encryptedRecordId.toCharArray());
          break;

        case ARTIFACT:
          if (featureFlagService.isEnabled(FeatureName.ARTIFACT_STREAM_REFACTOR, accountId)) {
            List<String> allowedList = artifactVariableYamlHelper.computeAllowedList(
                accountId, configVar.getAllowedList(), configVar.getName());
            serviceVariable.setAllowedList(allowedList);
          } else {
            logger.warn("Yaml doesn't support {} type service variables", configVar.getValueType());
            continue;
          }
          break;

        default:
          logger.warn("Yaml doesn't  support {} type service variables", serviceVariable.getType());
          continue;
      }

      lastUpdatedServiceVariable = serviceVariableService.update(serviceVariable, true);
    }

    return lastUpdatedServiceVariable;
  }

  private ServiceVariable createNewServiceVariable(
      String accountId, String appId, String serviceId, NameValuePair.Yaml cv) {
    notNullCheck("Value type is not set for variable: " + cv.getName(), cv.getValueType(), USER);

    ServiceVariableBuilder serviceVariableBuilder = ServiceVariable.builder()
                                                        .name(cv.getName())
                                                        .entityType(EntityType.SERVICE)
                                                        .entityId(serviceId)
                                                        .accountId(accountId)
                                                        .templateId(ServiceVariable.DEFAULT_TEMPLATE_ID);

    if ("TEXT".equals(cv.getValueType())) {
      serviceVariableBuilder.type(Type.TEXT);
      serviceVariableBuilder.value(cv.getValue() != null ? cv.getValue().toCharArray() : null);
    } else if ("ENCRYPTED_TEXT".equals(cv.getValueType())) {
      serviceVariableBuilder.type(Type.ENCRYPTED_TEXT);
      // wingsPersistence will encrypt the record depending on type and value, so we need not
      // setEncryptedValue. If the value is already a secret reference ( eg. safeHarness:xxxxx ),
      // it will be persisted as such which we do not want, therefore we need to extract out the
      // encrypted record id.
      serviceVariableBuilder.value(
          cv.getValue() != null ? yamlHelper.extractEncryptedRecordId(cv.getValue()).toCharArray() : null);
    } else if ("ARTIFACT".equals(cv.getValueType())) {
      if (featureFlagService.isEnabled(FeatureName.ARTIFACT_STREAM_REFACTOR, accountId)) {
        serviceVariableBuilder.type(Type.ARTIFACT);
        List<String> allowedList =
            artifactVariableYamlHelper.computeAllowedList(accountId, cv.getAllowedList(), cv.getName());
        serviceVariableBuilder.allowedList(allowedList);
      } else {
        logger.warn("Yaml doesn't support {} type service variables", cv.getValueType());
      }
    } else {
      logger.warn("Yaml doesn't support {} type service variables", cv.getValueType());
      serviceVariableBuilder.value(cv.getValue() != null ? cv.getValue().toCharArray() : null);
    }

    ServiceVariable serviceVariable = serviceVariableBuilder.build();
    serviceVariable.setAppId(appId);
    serviceVariable.setEnvId(GLOBAL_ENV_ID);

    return serviceVariable;
  }

  @Override
  public void delete(ChangeContext<Yaml> changeContext) {
    String accountId = changeContext.getChange().getAccountId();
    String yamlFilePath = changeContext.getChange().getFilePath();
    Optional<Application> optionalApplication = yamlHelper.getApplicationIfPresent(accountId, yamlFilePath);
    if (!optionalApplication.isPresent()) {
      return;
    }

    Application application = optionalApplication.get();
    Optional<Service> serviceOptional = yamlHelper.getServiceIfPresent(application.getUuid(), yamlFilePath);
    if (!serviceOptional.isPresent()) {
      return;
    }

    Service service = serviceOptional.get();
    serviceResourceService.deleteByYamlGit(
        service.getAppId(), service.getUuid(), changeContext.getChange().isSyncFromGit());
  }
}
