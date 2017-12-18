package software.wings.service.impl.yaml.handler.environment;

import static java.util.Collections.emptyList;
import static software.wings.beans.EntityType.ENVIRONMENT;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.EntityType;
import software.wings.beans.Environment;
import software.wings.beans.Environment.Builder;
import software.wings.beans.Environment.EnvironmentType;
import software.wings.beans.Environment.Yaml;
import software.wings.beans.NameValuePair;
import software.wings.beans.ServiceVariable;
import software.wings.beans.ServiceVariable.Type;
import software.wings.beans.yaml.ChangeContext;
import software.wings.exception.HarnessException;
import software.wings.exception.WingsException;
import software.wings.service.impl.yaml.handler.BaseYamlHandler;
import software.wings.service.impl.yaml.service.YamlHelper;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.ServiceTemplateService;
import software.wings.service.intfc.ServiceVariableService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.utils.Util;
import software.wings.utils.Validator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author rktummala on 11/07/17
 */
public class EnvironmentYamlHandler extends BaseYamlHandler<Environment.Yaml, Environment> {
  private static final Logger logger = LoggerFactory.getLogger(EnvironmentYamlHandler.class);
  @Inject YamlHelper yamlHelper;
  @Inject EnvironmentService environmentService;
  @Inject SecretManager secretManager;
  @Inject ServiceVariableService serviceVariableService;
  @Inject ServiceTemplateService serviceTemplateService;

  @Override
  public Environment.Yaml toYaml(Environment environment, String appId) {
    List<ServiceVariable> serviceVariableList = getAllVariableOverridesForEnv(environment);
    List<NameValuePair.Yaml> nameValuePairList = convertToNameValuePair(serviceVariableList);
    return Environment.Yaml.builder()
        .type(ENVIRONMENT.name())
        .description(environment.getDescription())
        .environmentType(environment.getEnvironmentType().name())
        .configVariables(nameValuePairList)
        .build();
  }

  private List<ServiceVariable> getAllVariableOverridesForEnv(Environment environment) {
    List<ServiceVariable> serviceVariableList = Lists.newArrayList();
    if (Util.isEmpty(environment.getServiceTemplates())) {
      return serviceVariableList;
    }
    environment.getServiceTemplates().stream().forEach(serviceTemplate -> {
      List<ServiceVariable> serviceVariablesByTemplate = serviceVariableService.getServiceVariablesByTemplate(
          environment.getAppId(), environment.getUuid(), serviceTemplate, false);
      serviceVariableList.addAll(serviceVariablesByTemplate);
    });

    List<ServiceVariable> serviceVariablesForAllServices =
        serviceVariableService.getServiceVariablesForEntity(environment.getAppId(), environment.getUuid(), false);
    serviceVariableList.addAll(serviceVariablesForAllServices);
    return serviceVariableList;
  }

  private List<NameValuePair.Yaml> convertToNameValuePair(List<ServiceVariable> serviceVariables) {
    if (serviceVariables == null) {
      return Lists.newArrayList();
    }

    return serviceVariables.stream()
        .map(serviceVariable -> {
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
          } else {
            logger.warn("Value type LB not supported, skipping the processing of value");
          }

          return NameValuePair.Yaml.Builder.aYaml()
              .withValueType(variableType.name())
              .withValue(value)
              .withName(serviceVariable.getName())
              .build();
        })
        .collect(Collectors.toList());
  }

  @Override
  public Environment upsertFromYaml(ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext)
      throws HarnessException {
    ensureValidChange(changeContext, changeSetContext);
    String appId =
        yamlHelper.getAppId(changeContext.getChange().getAccountId(), changeContext.getChange().getFilePath());
    Validator.notNullCheck("appId null for given yaml file:" + changeContext.getChange().getFilePath(), appId);
    Yaml yaml = changeContext.getYaml();
    String environmentName = yamlHelper.getEnvironmentName(changeContext.getChange().getFilePath());
    Environment current = Builder.anEnvironment()
                              .withAppId(appId)
                              .withName(environmentName)
                              .withDescription(yaml.getDescription())
                              .withEnvironmentType(EnvironmentType.valueOf(yaml.getEnvironmentType()))
                              .build();

    Environment previous = yamlHelper.getEnvironment(appId, changeContext.getChange().getFilePath());

    if (previous != null) {
      current.setUuid(previous.getUuid());
      Yaml previousYaml = toYaml(previous, previous.getAppId());
      List<ServiceVariable> currentVariableList = getAllVariableOverridesForEnv(previous);
      saveOrUpdateServiceVariables(previousYaml.getConfigVariables(), yaml.getConfigVariables(), currentVariableList,
          current.getAppId(), current.getUuid());
      return environmentService.update(current);
    } else {
      saveOrUpdateServiceVariables(null, yaml.getConfigVariables(), emptyList(), current.getAppId(), current.getUuid());
      return environmentService.save(current);
    }
  }

  @Override
  public boolean validate(ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext) {
    return true;
  }

  @Override
  public Class getYamlClass() {
    return Environment.Yaml.class;
  }

  @Override
  public Environment get(String accountId, String yamlFilePath) {
    return yamlHelper.getEnvironment(accountId, yamlFilePath);
  }

  @Override
  public void delete(ChangeContext<Yaml> changeContext) throws HarnessException {
    Environment environment = get(changeContext.getChange().getAccountId(), changeContext.getChange().getFilePath());
    if (environment != null) {
      environmentService.delete(environment.getAppId(), environment.getUuid());
    }
  }

  private void saveOrUpdateServiceVariables(List<NameValuePair.Yaml> previousConfigVarNameValuePairs,
      List<NameValuePair.Yaml> latestConfigVarNameValuePairs, List<ServiceVariable> currentVariables, String appId,
      String envId) throws HarnessException {
    // what are the config variable changes? Which are additions and which are deletions?
    List<NameValuePair.Yaml> configVarsToAdd = new ArrayList<>();
    List<NameValuePair.Yaml> configVarsToDelete = new ArrayList<>();
    List<NameValuePair.Yaml> configVarsToUpdate = new ArrayList<>();

    // ----------- START CONFIG VARIABLE SECTION ---------------
    if (latestConfigVarNameValuePairs != null) {
      // initialize the config vars to add from the after
      for (NameValuePair.Yaml cv : latestConfigVarNameValuePairs) {
        configVarsToAdd.add(cv);
      }
    }

    if (previousConfigVarNameValuePairs != null) {
      // initialize the config vars to delete from the before, and remove the befores from the config vars to add list
      for (NameValuePair.Yaml cv : previousConfigVarNameValuePairs) {
        configVarsToDelete.add(cv);
        configVarsToAdd.remove(cv);
      }
    }

    if (latestConfigVarNameValuePairs != null) {
      // remove the afters from the config vars to delete list
      for (NameValuePair.Yaml cv : latestConfigVarNameValuePairs) {
        configVarsToDelete.remove(cv);

        if (previousConfigVarNameValuePairs.contains(cv)) {
          NameValuePair.Yaml beforeCV = null;
          for (NameValuePair.Yaml bcv : previousConfigVarNameValuePairs) {
            if (bcv.equals(cv)) {
              beforeCV = bcv;
              break;
            }
          }
          if (!cv.getValue().equals(beforeCV.getValue())) {
            configVarsToUpdate.add(cv);
          }
        }
      }
    }

    Map<String, ServiceVariable> variableMap =
        currentVariables.stream().collect(Collectors.toMap(var -> var.getName(), serviceVar -> serviceVar));

    // do deletions
    configVarsToDelete.stream().forEach(configVar -> {
      if (variableMap.containsKey(configVar.getName())) {
        serviceVariableService.delete(appId, variableMap.get(configVar.getName()).getUuid());
      }
    });

    // save the new variables
    configVarsToAdd.stream().forEach(
        configVar -> serviceVariableService.save(createNewVariableOverride(appId, envId, configVar)));

    try {
      // update the existing variables
      configVarsToUpdate.stream().forEach(configVar -> {
        ServiceVariable serviceVar = variableMap.get(configVar.getName());
        if (serviceVar != null) {
          String value = configVar.getValue();
          if (serviceVar.getType() == Type.ENCRYPTED_TEXT) {
            serviceVar.setValue(value != null ? value.toCharArray() : null);
            serviceVar.setEncryptedValue(value != null ? value : null);
          } else if (serviceVar.getType() == Type.TEXT) {
            serviceVar.setValue(value != null ? value.toCharArray() : null);
          } else {
            logger.warn("Yaml doesn't support LB type service variables");
            return;
          }

          serviceVariableService.update(serviceVar);
        }
      });
    } catch (WingsException ex) {
      throw new HarnessException(ex);
    }
  }

  private ServiceVariable createNewVariableOverride(String appId, String envId, NameValuePair.Yaml cv) {
    ServiceVariable newServiceVariable = ServiceVariable.builder()
                                             .name(cv.getName())
                                             .value(cv.getValue().toCharArray())
                                             .entityType(EntityType.ENVIRONMENT)
                                             .entityId(envId)
                                             .templateId(ServiceVariable.DEFAULT_TEMPLATE_ID)
                                             .type(Type.TEXT)
                                             .build();
    newServiceVariable.setAppId(appId);

    return newServiceVariable;
  }
}
