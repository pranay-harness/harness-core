package software.wings.service.impl.yaml.handler.service;

import static java.util.Collections.emptyList;
import static software.wings.beans.EntityType.SERVICE;
import static software.wings.beans.Service.Builder.aService;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.EntityType;
import software.wings.beans.NameValuePair;
import software.wings.beans.NameValuePair.Yaml;
import software.wings.beans.Service;
import software.wings.beans.ServiceVariable;
import software.wings.beans.ServiceVariable.ServiceVariableBuilder;
import software.wings.beans.ServiceVariable.Type;
import software.wings.beans.yaml.ChangeContext;
import software.wings.exception.HarnessException;
import software.wings.exception.WingsException;
import software.wings.service.impl.yaml.handler.BaseYamlHandler;
import software.wings.service.impl.yaml.service.YamlHelper;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.ServiceVariableService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.utils.ArtifactType;
import software.wings.utils.Util;
import software.wings.utils.Validator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author rktummala on 10/22/17
 */
@Singleton
public class ServiceYamlHandler extends BaseYamlHandler<Service.Yaml, Service> {
  private static final Logger logger = LoggerFactory.getLogger(ServiceYamlHandler.class);
  @Inject YamlHelper yamlHelper;
  @Inject ServiceResourceService serviceResourceService;
  @Inject ServiceVariableService serviceVariableService;
  @Inject SecretManager secretManager;

  @Override
  public Service.Yaml toYaml(Service service, String appId) {
    List<NameValuePair.Yaml> nameValuePairList = convertToNameValuePair(service.getServiceVariables());
    return Service.Yaml.builder()
        .type(SERVICE.name())
        .harnessApiVersion(getHarnessApiVersion())
        .description(service.getDescription())
        .artifactType(service.getArtifactType().name())
        .configVariables(nameValuePairList)
        .build();
  }

  private List<Yaml> convertToNameValuePair(List<ServiceVariable> serviceVariables) {
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
            logger.warn("Value type {} not supported, skipping the processing of value", variableType);
          }

          return NameValuePair.Yaml.builder()
              .valueType(variableType.name())
              .value(value)
              .name(serviceVariable.getName())
              .build();
        })
        .collect(Collectors.toList());
  }

  @Override
  public Service upsertFromYaml(ChangeContext<Service.Yaml> changeContext, List<ChangeContext> changeSetContext)
      throws HarnessException {
    ensureValidChange(changeContext, changeSetContext);

    String yamlFilePath = changeContext.getChange().getFilePath();
    String accountId = changeContext.getChange().getAccountId();
    String appId = yamlHelper.getAppId(accountId, yamlFilePath);
    Validator.notNullCheck("appId null for given yaml file:" + yamlFilePath, appId);

    String serviceName = yamlHelper.getServiceName(yamlFilePath);
    Service.Yaml yaml = changeContext.getYaml();
    Service current = aService().withAppId(appId).withName(serviceName).withDescription(yaml.getDescription()).build();

    Service previous = get(accountId, yamlFilePath);

    if (previous != null) {
      current.setUuid(previous.getUuid());
      current = serviceResourceService.update(current, true);
      Service.Yaml previousYaml = toYaml(previous, previous.getAppId());
      saveOrUpdateServiceVariables(
          previousYaml, yaml, previous.getServiceVariables(), current.getAppId(), current.getUuid());
    } else {
      ArtifactType artifactType = Util.getEnumFromString(ArtifactType.class, yaml.getArtifactType());
      current.setArtifactType(artifactType);
      current = serviceResourceService.save(current, true);
      saveOrUpdateServiceVariables(null, yaml, emptyList(), current.getAppId(), current.getUuid());
    }
    return current;
  }

  @Override
  public boolean validate(ChangeContext<Service.Yaml> changeContext, List<ChangeContext> changeSetContext) {
    return true;
  }

  @Override
  public Class getYamlClass() {
    return Service.Yaml.class;
  }

  @Override
  public Service get(String accountId, String yamlFilePath) {
    String appId = yamlHelper.getAppId(accountId, yamlFilePath);
    return yamlHelper.getService(appId, yamlFilePath);
  }

  private void saveOrUpdateServiceVariables(Service.Yaml previousYaml, Service.Yaml updatedYaml,
      List<ServiceVariable> currentServiceVariables, String appId, String serviceId) throws HarnessException {
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
      for (NameValuePair.Yaml cv : configVars) {
        configVarsToAdd.add(cv);
      }
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
          if (!cv.getValue().equals(beforeCV.getValue())) {
            configVarsToUpdate.add(cv);
          }
        }
      }
    }

    Map<String, ServiceVariable> serviceVariableMap = currentServiceVariables.stream().collect(
        Collectors.toMap(serviceVar -> serviceVar.getName(), serviceVar -> serviceVar));

    // do deletions
    configVarsToDelete.stream().forEach(configVar -> {
      if (serviceVariableMap.containsKey(configVar.getName())) {
        serviceVariableService.delete(appId, serviceVariableMap.get(configVar.getName()).getUuid());
      }
    });

    // save the new variables
    configVarsToAdd.stream().forEach(
        configVar -> serviceVariableService.save(createNewServiceVariable(appId, serviceId, configVar)));

    try {
      // update the existing variables
      configVarsToUpdate.stream().forEach(configVar -> {
        ServiceVariable serviceVar = serviceVariableMap.get(configVar.getName());
        if (serviceVar != null) {
          String value = configVar.getValue();
          if (serviceVar.getType() == Type.ENCRYPTED_TEXT) {
            serviceVar.setValue(value != null ? value.toCharArray() : null);
            serviceVar.setEncryptedValue(value != null ? value : null);
          } else if (serviceVar.getType() == Type.TEXT) {
            serviceVar.setValue(value != null ? value.toCharArray() : null);
          } else {
            logger.warn("Yaml doesn't support {} type service variables", serviceVar.getType());
            return;
          }

          serviceVariableService.update(serviceVar);
        }
      });
    } catch (WingsException ex) {
      throw new HarnessException(ex);
    }
  }

  private ServiceVariable createNewServiceVariable(String appId, String serviceId, NameValuePair.Yaml cv) {
    Validator.notNullCheck("Value type is not set for variable: " + cv.getName(), cv.getValueType());

    ServiceVariableBuilder serviceVariableBuilder = ServiceVariable.builder()
                                                        .name(cv.getName())
                                                        .entityType(EntityType.SERVICE)
                                                        .entityId(serviceId)
                                                        .templateId(ServiceVariable.DEFAULT_TEMPLATE_ID);

    if ("TEXT".equals(cv.getValueType())) {
      serviceVariableBuilder.type(Type.TEXT);
      serviceVariableBuilder.value(cv.getValue().toCharArray());
    } else if ("ENCRYPTED_TEXT".equals(cv.getValueType())) {
      serviceVariableBuilder.type(Type.ENCRYPTED_TEXT);
      serviceVariableBuilder.encryptedValue(cv.getValue());
    } else {
      logger.warn("Yaml doesn't support {} type service variables", cv.getValueType());
      serviceVariableBuilder.value(cv.getValue().toCharArray());
    }

    ServiceVariable serviceVariable = serviceVariableBuilder.build();
    serviceVariable.setAppId(appId);

    return serviceVariable;
  }

  @Override
  public void delete(ChangeContext<Service.Yaml> changeContext) throws HarnessException {
    Service service = get(changeContext.getChange().getAccountId(), changeContext.getChange().getFilePath());
    if (service != null) {
      serviceResourceService.delete(service.getAppId(), service.getUuid());
    }
  }
}
