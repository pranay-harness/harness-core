package software.wings.service.impl.yaml.handler.service;

import static io.harness.exception.WingsException.USER;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static software.wings.beans.Environment.GLOBAL_ENV_ID;
import static software.wings.utils.Validator.notNullCheck;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.exception.WingsException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
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
import java.util.List;
import java.util.Map;
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
              throw new WingsException(e);
            }
          } else if (Type.TEXT == variableType) {
            value = String.valueOf(serviceVariable.getValue());
          } else if (Type.ARTIFACT == variableType) {
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
    if (StringUtils.isNotBlank(applicationStack)) {
      AppContainer appContainer = appContainerService.getByName(accountId, applicationStack);
      notNullCheck("No application stack found with the given name: " + applicationStack, appContainer, USER);
      currentService.setAppContainer(appContainer);
    }
    Service previousService = get(accountId, yamlFilePath);

    boolean syncFromGit = changeContext.getChange().isSyncFromGit();
    currentService.setSyncFromGit(syncFromGit);

    if (previousService != null) {
      currentService.setUuid(previousService.getUuid());
      currentService = serviceResourceService.update(currentService, true);
      Yaml previousYaml = toYaml(previousService, previousService.getAppId());
      saveOrUpdateServiceVariables(previousYaml, yaml, previousService.getServiceVariables(), currentService.getAppId(),
          currentService.getUuid(), syncFromGit);
    } else {
      ArtifactType artifactType = Utils.getEnumFromString(ArtifactType.class, yaml.getArtifactType());
      currentService.setArtifactType(artifactType);
      if (StringUtils.isNotBlank(yaml.getDeploymentType())) {
        DeploymentType deploymentType = Utils.getEnumFromString(DeploymentType.class, yaml.getDeploymentType());
        currentService.setDeploymentType(deploymentType);
      }
      currentService =
          serviceResourceService.save(currentService, true, serviceResourceService.hasInternalCommands(currentService));
      saveOrUpdateServiceVariables(
          null, yaml, emptyList(), currentService.getAppId(), currentService.getUuid(), syncFromGit);
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

  private void saveOrUpdateServiceVariables(Yaml previousYaml, Yaml updatedYaml,
      List<ServiceVariable> previousServiceVariables, String appId, String serviceId, boolean syncFromGit) {
    // what are the config variable changes? Which are additions and which are deletions?
    List<NameValuePair.Yaml> configVarsToAdd = new ArrayList<>();
    List<NameValuePair.Yaml> configVarsToDelete = new ArrayList<>();
    List<NameValuePair.Yaml> configVarsToUpdate = new ArrayList<>();

    // ----------- START CONFIG VARIABLE SECTION ---------------
    List<NameValuePair.Yaml> configVars = updatedYaml.getConfigVariables();
    List<NameValuePair.Yaml> beforeConfigVars = null;

    if (previousYaml != null) {
      beforeConfigVars = previousYaml.getConfigVariables();
    }

    if (configVars != null) {
      // initialize the config vars to add from the after
      configVarsToAdd.addAll(configVars);
    }

    if (beforeConfigVars != null) {
      // initialize the config vars to delete from the before, and remove the befores from the config vars to add list
      for (NameValuePair.Yaml cv : beforeConfigVars) {
        configVarsToDelete.add(cv);
        configVarsToAdd.remove(cv);
      }
    }

    if (configVars != null) {
      // remove the afters from the config vars to delete list
      for (NameValuePair.Yaml cv : configVars) {
        configVarsToDelete.remove(cv);

        if (beforeConfigVars != null && beforeConfigVars.contains(cv)) {
          NameValuePair.Yaml beforeCV = null;
          for (NameValuePair.Yaml bcv : beforeConfigVars) {
            if (bcv.equals(cv)) {
              beforeCV = bcv;
              break;
            }
          }
          if (beforeCV != null) {
            if (beforeCV.getValueType().equals("ARTIFACT")) {
              configVarsToUpdate.add(cv);
            } else if (!cv.getValue().equals(beforeCV.getValue())) {
              configVarsToUpdate.add(cv);
            }
          }
        }
      }
    }

    Map<String, ServiceVariable> serviceVariableMap =
        previousServiceVariables.stream().collect(Collectors.toMap(ServiceVariable::getName, serviceVar -> serviceVar));

    // do deletions
    for (NameValuePair.Yaml yaml : configVarsToDelete) {
      if (serviceVariableMap.containsKey(yaml.getName())) {
        serviceVariableService.delete(appId, serviceVariableMap.get(yaml.getName()).getUuid(), syncFromGit);
      }
    }

    String accountId = appService.get(appId).getAccountId();
    // save the new variables
    for (NameValuePair.Yaml yaml : configVarsToAdd) {
      serviceVariableService.save(createNewServiceVariable(accountId, appId, serviceId, yaml), syncFromGit);
    }

    // update the existing variables
    for (NameValuePair.Yaml configVar : configVarsToUpdate) {
      ServiceVariable serviceVar = serviceVariableMap.get(configVar.getName());
      if (serviceVar != null) {
        String value = configVar.getValue();
        if (serviceVar.getType() == Type.ENCRYPTED_TEXT) {
          serviceVar.setValue(value != null ? value.toCharArray() : null);
          serviceVar.setEncryptedValue(value);
        } else if (serviceVar.getType() == Type.TEXT) {
          serviceVar.setValue(value != null ? value.toCharArray() : null);
        } else if (serviceVar.getType() == Type.ARTIFACT) {
          if (featureFlagService.isEnabled(FeatureName.ARTIFACT_STREAM_REFACTOR, accountId)) {
            List<String> allowedList =
                artifactVariableYamlHelper.computeAllowedList(accountId, configVar.getAllowedList());
            serviceVar.setAllowedList(allowedList);
          } else {
            logger.warn("Yaml doesn't support {} type service variables", configVar.getValueType());
          }
        } else {
          logger.warn("Yaml doesn't support {} type service variables", serviceVar.getType());
          continue;
        }

        serviceVariableService.update(serviceVar, syncFromGit);
      }
    }
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
      serviceVariableBuilder.encryptedValue(cv.getValue());
    } else if ("ARTIFACT".equals(cv.getValueType())) {
      if (featureFlagService.isEnabled(FeatureName.ARTIFACT_STREAM_REFACTOR, accountId)) {
        serviceVariableBuilder.type(Type.ARTIFACT);
        List<String> allowedList = artifactVariableYamlHelper.computeAllowedList(accountId, cv.getAllowedList());
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
