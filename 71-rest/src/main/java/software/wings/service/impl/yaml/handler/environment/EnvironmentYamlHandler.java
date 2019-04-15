package software.wings.service.impl.yaml.handler.environment;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.exception.WingsException.USER;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static software.wings.beans.EntityType.SERVICE_TEMPLATE;
import static software.wings.beans.Environment.GLOBAL_ENV_ID;
import static software.wings.service.intfc.ServiceVariableService.EncryptedFieldMode.OBTAIN_VALUE;
import static software.wings.utils.Validator.notNullCheck;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.exception.HarnessException;
import io.harness.exception.WingsException;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.Key;
import software.wings.beans.Application;
import software.wings.beans.EntityType;
import software.wings.beans.Environment;
import software.wings.beans.Environment.Builder;
import software.wings.beans.Environment.EnvironmentType;
import software.wings.beans.Environment.VariableOverrideYaml;
import software.wings.beans.Environment.Yaml;
import software.wings.beans.Service;
import software.wings.beans.ServiceTemplate;
import software.wings.beans.ServiceVariable;
import software.wings.beans.ServiceVariable.OverrideType;
import software.wings.beans.ServiceVariable.ServiceVariableBuilder;
import software.wings.beans.ServiceVariable.Type;
import software.wings.beans.yaml.ChangeContext;
import software.wings.service.impl.yaml.handler.BaseYamlHandler;
import software.wings.service.impl.yaml.service.YamlHelper;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.ServiceTemplateService;
import software.wings.service.intfc.ServiceVariableService;
import software.wings.service.intfc.security.SecretManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
/**
 * @author rktummala on 11/07/17
 */
@Singleton
@Slf4j
public class EnvironmentYamlHandler extends BaseYamlHandler<Environment.Yaml, Environment> {
  @Inject YamlHelper yamlHelper;
  @Inject EnvironmentService environmentService;
  @Inject SecretManager secretManager;
  @Inject ServiceVariableService serviceVariableService;
  @Inject ServiceResourceService serviceResourceService;
  @Inject ServiceTemplateService serviceTemplateService;
  @Inject private AppService appService;

  @Override
  public Environment.Yaml toYaml(Environment environment, String appId) {
    List<ServiceVariable> serviceVariableList = getAllVariableOverridesForEnv(environment);
    List<VariableOverrideYaml> variableOverrideYamlList =
        convertToVariableOverrideYaml(serviceVariableList, environment.getName());
    return Environment.Yaml.builder()
        .description(environment.getDescription())
        .configMapYaml(environment.getConfigMapYaml())
        .configMapYamlByServiceTemplateId(environment.getConfigMapYamlByServiceTemplateId())
        .environmentType(environment.getEnvironmentType().name())
        .variableOverrides(variableOverrideYamlList)
        .harnessApiVersion(getHarnessApiVersion())
        .build();
  }

  private List<ServiceVariable> getAllVariableOverridesForEnv(Environment environment) {
    List<ServiceVariable> serviceVariableList = Lists.newArrayList();
    if (isEmpty(environment.getServiceTemplates())) {
      return serviceVariableList;
    }
    environment.getServiceTemplates().forEach(serviceTemplate -> {
      List<ServiceVariable> serviceVariablesByTemplate = serviceVariableService.getServiceVariablesByTemplate(
          environment.getAppId(), environment.getUuid(), serviceTemplate, OBTAIN_VALUE);
      serviceVariableList.addAll(serviceVariablesByTemplate);
    });

    List<ServiceVariable> serviceVariablesForAllServices = serviceVariableService.getServiceVariablesForEntity(
        environment.getAppId(), environment.getUuid(), OBTAIN_VALUE);
    serviceVariableList.addAll(serviceVariablesForAllServices);
    return serviceVariableList;
  }

  private List<VariableOverrideYaml> convertToVariableOverrideYaml(
      List<ServiceVariable> serviceVariables, String envName) {
    if (serviceVariables == null) {
      return Lists.newArrayList();
    }

    return serviceVariables.stream()
        .map(serviceVariable -> {
          Type variableType = serviceVariable.getType();
          String value;
          if (Type.ENCRYPTED_TEXT == variableType) {
            try {
              value = secretManager.getEncryptedYamlRef(serviceVariable);
            } catch (IllegalAccessException e) {
              throw new WingsException(e);
            }
          } else if (Type.TEXT == variableType) {
            value = String.valueOf(serviceVariable.getValue());
          } else {
            String msg = "Invalid value type: " + variableType + ". for variable: " + serviceVariable.getName()
                + " in env: " + envName;
            logger.warn(msg);
            throw new WingsException(msg);
          }

          String parentServiceName = getParentServiceName(serviceVariable);

          return VariableOverrideYaml.builder()
              .valueType(variableType.name())
              .value(value)
              .name(serviceVariable.getName())
              .serviceName(parentServiceName)
              .build();
        })
        .collect(toList());
  }

  private String getParentServiceName(ServiceVariable serviceVariable) {
    if (serviceVariable.getEntityType() == SERVICE_TEMPLATE) {
      String parentServiceVariableId = serviceVariable.getParentServiceVariableId();

      if (parentServiceVariableId != null) {
        ServiceVariable parentServiceVariable =
            serviceVariableService.get(serviceVariable.getAppId(), parentServiceVariableId);
        String serviceId = parentServiceVariable.getEntityId();
        Service service = serviceResourceService.get(serviceVariable.getAppId(), serviceId);
        notNullCheck("Service not found for id: " + serviceId, service, USER);
        return service.getName();
      } else {
        ServiceTemplate serviceTemplate =
            serviceTemplateService.get(serviceVariable.getAppId(), serviceVariable.getEntityId());
        notNullCheck("Service template not found for id: " + serviceVariable.getEntityId(), serviceTemplate, USER);
        String serviceId = serviceTemplate.getServiceId();
        Service service = serviceResourceService.get(serviceVariable.getAppId(), serviceId);
        notNullCheck("Service not found for id: " + serviceId, service, USER);
        return service.getName();
      }
    }

    return null;
  }

  @Override
  public Environment upsertFromYaml(ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext)
      throws HarnessException {
    String appId =
        yamlHelper.getAppId(changeContext.getChange().getAccountId(), changeContext.getChange().getFilePath());
    notNullCheck("appId null for given yaml file:" + changeContext.getChange().getFilePath(), appId, USER);
    Yaml yaml = changeContext.getYaml();
    String environmentName = yamlHelper.getEnvironmentName(changeContext.getChange().getFilePath());
    Environment current = Builder.anEnvironment()
                              .withAppId(appId)
                              .withName(environmentName)
                              .withDescription(yaml.getDescription())
                              .withConfigMapYaml(yaml.getConfigMapYaml())
                              .withConfigMapYamlByServiceTemplateId(yaml.getConfigMapYamlByServiceTemplateId())
                              .withEnvironmentType(EnvironmentType.valueOf(yaml.getEnvironmentType()))
                              .build();

    boolean syncFromGit = changeContext.getChange().isSyncFromGit();
    current.setSyncFromGit(syncFromGit);

    Environment previous = yamlHelper.getEnvironment(appId, changeContext.getChange().getFilePath());

    if (previous != null) {
      current.setUuid(previous.getUuid());
      current = environmentService.update(current, true);
      Yaml previousYaml = toYaml(previous, previous.getAppId());
      List<ServiceVariable> currentVariableList = getAllVariableOverridesForEnv(previous);
      saveOrUpdateVariableOverrides(previousYaml.getVariableOverrides(), yaml.getVariableOverrides(),
          currentVariableList, current.getAppId(), current.getUuid(), syncFromGit);
    } else {
      current = environmentService.save(current);
      saveOrUpdateVariableOverrides(
          null, yaml.getVariableOverrides(), emptyList(), current.getAppId(), current.getUuid(), syncFromGit);
    }
    return current;
  }

  @Override
  public Class getYamlClass() {
    return Environment.Yaml.class;
  }

  @Override
  public Environment get(String accountId, String yamlFilePath) {
    String appId = yamlHelper.getAppId(accountId, yamlFilePath);
    notNullCheck("appId null for given yaml file:" + yamlFilePath, appId, USER);
    return yamlHelper.getEnvironment(appId, yamlFilePath);
  }

  @Override
  public void delete(ChangeContext<Yaml> changeContext) throws HarnessException {
    String accountId = changeContext.getChange().getAccountId();
    String yamlFilePath = changeContext.getChange().getFilePath();
    Optional<Application> optionalApplication = yamlHelper.getApplicationIfPresent(accountId, yamlFilePath);
    if (!optionalApplication.isPresent()) {
      return;
    }

    Optional<Environment> optionalEnvironment =
        yamlHelper.getEnvIfPresent(optionalApplication.get().getUuid(), yamlFilePath);
    if (!optionalEnvironment.isPresent()) {
      return;
    }

    environmentService.delete(optionalApplication.get().getUuid(), optionalEnvironment.get().getUuid(),
        changeContext.getChange().isSyncFromGit());
  }

  private void saveOrUpdateVariableOverrides(List<VariableOverrideYaml> previousVariableOverrideList,
      List<VariableOverrideYaml> latestVariableOverrideList, List<ServiceVariable> currentVariables, String appId,
      String envId, boolean syncFromGit) throws HarnessException {
    // what are the config variable changes? Which are additions and which are deletions?
    List<VariableOverrideYaml> configVarsToAdd = new ArrayList<>();
    List<VariableOverrideYaml> configVarsToDelete = new ArrayList<>();
    List<VariableOverrideYaml> configVarsToUpdate = new ArrayList<>();

    // ----------- START VARIABLE OVERRIDES SECTION ---------------
    if (latestVariableOverrideList != null) {
      // initialize the config vars to add from the after
      configVarsToAdd.addAll(latestVariableOverrideList);
    }

    if (previousVariableOverrideList != null) {
      // initialize the config vars to delete from the before, and remove the befores from the config vars to add list
      for (VariableOverrideYaml cv : previousVariableOverrideList) {
        configVarsToDelete.add(cv);
        configVarsToAdd.remove(cv);
      }
    }

    if (latestVariableOverrideList != null) {
      // remove the afters from the config vars to delete list
      for (VariableOverrideYaml cv : latestVariableOverrideList) {
        configVarsToDelete.remove(cv);

        if (previousVariableOverrideList != null && previousVariableOverrideList.contains(cv)) {
          VariableOverrideYaml beforeCV = null;
          for (VariableOverrideYaml bcv : previousVariableOverrideList) {
            if (bcv.equals(cv)) {
              beforeCV = bcv;
              break;
            }
          }
          if (beforeCV != null && !cv.getValue().equals(beforeCV.getValue())) {
            configVarsToUpdate.add(cv);
          }
        }
      }
    }

    Map<ServiceVariableKey, ServiceVariable> variableMap = new HashMap<>();
    for (ServiceVariable serviceVariable : currentVariables) {
      String serviceName = getParentServiceName(serviceVariable);
      variableMap.put(ServiceVariableKey.builder().name(serviceVariable.getName()).serviceName(serviceName).build(),
          serviceVariable);
    }

    // do deletions
    for (VariableOverrideYaml variableOverrideYaml : configVarsToDelete) {
      ServiceVariableKey serviceVariableKey = ServiceVariableKey.builder()
                                                  .name(variableOverrideYaml.getName())
                                                  .serviceName(variableOverrideYaml.getServiceName())
                                                  .build();
      if (variableMap.containsKey(serviceVariableKey)) {
        serviceVariableService.delete(appId, variableMap.get(serviceVariableKey).getUuid(), syncFromGit);
      }
    }

    for (VariableOverrideYaml configVar : configVarsToAdd) {
      // save the new variables
      serviceVariableService.save(createNewVariableOverride(appId, envId, configVar), syncFromGit);
    }

    try {
      // update the existing variables
      for (VariableOverrideYaml configVar : configVarsToUpdate) {
        ServiceVariableKey serviceVariableKey =
            ServiceVariableKey.builder().name(configVar.getName()).serviceName(configVar.getServiceName()).build();
        ServiceVariable serviceVar = variableMap.get(serviceVariableKey);
        if (serviceVar != null) {
          String value = configVar.getValue();
          if (serviceVar.getType() == Type.ENCRYPTED_TEXT) {
            serviceVar.setValue(value != null ? value.toCharArray() : null);
            serviceVar.setEncryptedValue(value);
          } else if (serviceVar.getType() == Type.TEXT) {
            serviceVar.setValue(value != null ? value.toCharArray() : null);
          } else {
            logger.warn("Yaml doesn't support LB type service variables");
            continue;
          }

          serviceVariableService.update(serviceVar, syncFromGit);
        }
      }
    } catch (WingsException ex) {
      throw new HarnessException(ex);
    }
  }

  private ServiceVariable createNewVariableOverride(String appId, String envId, VariableOverrideYaml overrideYaml)
      throws HarnessException {
    notNullCheck("Value type is not set for variable: " + overrideYaml.getName(), overrideYaml.getValueType(), USER);

    String accountId = appService.get(appId, false).getAccountId();
    ServiceVariableBuilder variableBuilder =
        ServiceVariable.builder().name(overrideYaml.getName()).accountId(accountId);
    if (overrideYaml.getServiceName() == null) {
      variableBuilder.entityType(EntityType.ENVIRONMENT)
          .entityId(envId)
          .templateId(ServiceVariable.DEFAULT_TEMPLATE_ID)
          .envId(GLOBAL_ENV_ID);
    } else {
      String parentServiceName = overrideYaml.getServiceName();
      Service service = serviceResourceService.getServiceByName(appId, parentServiceName);
      notNullCheck("No service found for given name: " + parentServiceName, service, USER);
      List<Key<ServiceTemplate>> templateRefKeysByService =
          serviceTemplateService.getTemplateRefKeysByService(appId, service.getUuid(), envId);
      if (isEmpty(templateRefKeysByService)) {
        throw new HarnessException("Unable to locate a service template for the given service: " + parentServiceName);
      }

      String serviceTemplateId = (String) templateRefKeysByService.get(0).getId();
      if (isEmpty(serviceTemplateId)) {
        throw new HarnessException(
            "Unable to locate a service template with the given service: " + parentServiceName + " and env: " + envId);
      }

      List<ServiceVariable> serviceVariablesList =
          serviceVariableService.getServiceVariablesForEntity(appId, service.getUuid(), OBTAIN_VALUE);
      Optional<ServiceVariable> variableOptional =
          serviceVariablesList.stream()
              .filter(serviceVariable -> serviceVariable.getName().equals(overrideYaml.getName()))
              .findFirst();
      if (variableOptional.isPresent()) {
        ServiceVariable parentServiceVariable = variableOptional.get();
        variableBuilder.parentServiceVariableId(parentServiceVariable.getUuid());
      }

      variableBuilder.entityType(EntityType.SERVICE_TEMPLATE)
          .entityId(serviceTemplateId)
          .templateId(serviceTemplateId)
          .overrideType(OverrideType.ALL)
          .envId(envId);
    }

    if ("TEXT".equals(overrideYaml.getValueType())) {
      variableBuilder.type(Type.TEXT);
      variableBuilder.value(overrideYaml.getValue().toCharArray());
    } else if ("ENCRYPTED_TEXT".equals(overrideYaml.getValueType())) {
      variableBuilder.type(Type.ENCRYPTED_TEXT);
      variableBuilder.encryptedValue(overrideYaml.getValue());
      variableBuilder.value(overrideYaml.getValue().toCharArray());
    } else {
      logger.warn("Yaml doesn't support {} type service variables", overrideYaml.getValueType());
      variableBuilder.value(overrideYaml.getValue().toCharArray());
    }

    ServiceVariable serviceVariable = variableBuilder.build();
    serviceVariable.setAppId(appId);

    return serviceVariable;
  }

  @AllArgsConstructor
  @lombok.Builder
  @EqualsAndHashCode
  private static class ServiceVariableKey {
    private String name;
    private String serviceName;
  }
}
