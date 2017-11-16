package software.wings.service.impl.yaml.handler.service;

import static software.wings.beans.EntityType.SERVICE;
import static software.wings.utils.Util.isEmpty;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.EntityType;
import software.wings.beans.NameValuePair;
import software.wings.beans.NameValuePair.Yaml;
import software.wings.beans.Service;
import software.wings.beans.Service.Builder;
import software.wings.beans.ServiceVariable;
import software.wings.beans.ServiceVariable.Type;
import software.wings.beans.yaml.ChangeContext;
import software.wings.exception.HarnessException;
import software.wings.exception.WingsException;
import software.wings.service.impl.yaml.handler.BaseYamlHandler;
import software.wings.service.impl.yaml.sync.YamlSyncHelper;
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
public class ServiceYamlHandler extends BaseYamlHandler<Service.Yaml, Service> {
  private static final Logger logger = LoggerFactory.getLogger(ServiceYamlHandler.class);
  @Inject YamlSyncHelper yamlSyncHelper;
  @Inject ServiceResourceService serviceResourceService;
  @Inject ServiceVariableService serviceVariableService;
  @Inject SecretManager secretManager;

  @Override
  public Service.Yaml toYaml(Service service, String appId) {
    List<NameValuePair.Yaml> nameValuePairList = convertToNameValuePair(service.getServiceVariables());
    return Service.Yaml.Builder.anYaml()
        .withType(SERVICE.name())
        .withName(service.getName())
        .withDescription(service.getDescription())
        .withArtifactType(service.getArtifactType().name())
        .withConfigVariables(nameValuePairList)
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
  public Service updateFromYaml(ChangeContext<Service.Yaml> changeContext, List<ChangeContext> changeSetContext)
      throws HarnessException {
    if (!validate(changeContext, changeSetContext)) {
      return null;
    }

    String appId =
        yamlSyncHelper.getAppId(changeContext.getChange().getAccountId(), changeContext.getChange().getFilePath());
    Service previous = yamlSyncHelper.getService(appId, changeContext.getChange().getFilePath());
    Builder builder = previous.toBuilder();
    setWithYamlValues(builder, changeContext.getYaml());

    Service.Yaml previousYaml = toYaml(previous, previous.getAppId());
    saveOrUpdateServiceVariables(
        previousYaml, changeContext.getYaml(), previous.getServiceVariables(), previous.getAppId(), previous.getUuid());

    return serviceResourceService.update(builder.build());
  }

  private void setWithYamlValues(Builder builder, Service.Yaml serviceYaml) throws HarnessException {
    ArtifactType artifactType = Util.getEnumFromString(ArtifactType.class, serviceYaml.getArtifactType());
    builder.withName(serviceYaml.getName())
        .withDescription(serviceYaml.getDescription())
        .withArtifactType(artifactType)
        .build();
  }

  @Override
  public boolean validate(ChangeContext<Service.Yaml> changeContext, List<ChangeContext> changeSetContext) {
    Service.Yaml applicationYaml = changeContext.getYaml();
    return !(isEmpty(applicationYaml.getName()));
  }

  @Override
  public Service createFromYaml(ChangeContext<Service.Yaml> changeContext, List<ChangeContext> changeSetContext)
      throws HarnessException {
    if (!validate(changeContext, changeSetContext)) {
      return null;
    }

    String appId =
        yamlSyncHelper.getAppId(changeContext.getChange().getAccountId(), changeContext.getChange().getFilePath());
    Validator.notNullCheck("appId null for given yaml file:" + changeContext.getChange().getFilePath(), appId);
    Builder builder = Builder.aService().withAppId(appId);
    setWithYamlValues(builder, changeContext.getYaml());
    Service savedService = serviceResourceService.save(builder.build());
    saveOrUpdateServiceVariables(null, changeContext.getYaml(), null, savedService.getAppId(), savedService.getUuid());
    return savedService;
  }

  @Override
  public Class getYamlClass() {
    return Service.Yaml.class;
  }

  @Override
  public Service get(String accountId, String yamlFilePath) {
    String appId = yamlSyncHelper.getAppId(accountId, yamlFilePath);
    return yamlSyncHelper.getService(appId, yamlFilePath);
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

        if (beforeConfigVars.contains(cv)) {
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

  private ServiceVariable createNewServiceVariable(String appId, String serviceId, NameValuePair.Yaml cv) {
    ServiceVariable newServiceVariable = ServiceVariable.builder()
                                             .name(cv.getName())
                                             .value(cv.getValue().toCharArray())
                                             .entityType(EntityType.SERVICE)
                                             .entityId(serviceId)
                                             .templateId(ServiceVariable.DEFAULT_TEMPLATE_ID)
                                             .type(Type.TEXT)
                                             .build();
    newServiceVariable.setAppId(appId);

    return newServiceVariable;
  }
}
