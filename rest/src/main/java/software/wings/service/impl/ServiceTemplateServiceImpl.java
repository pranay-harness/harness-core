package software.wings.service.impl;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.util.Arrays.asList;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.concat;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.beans.ConfigFile.DEFAULT_TEMPLATE_ID;
import static software.wings.beans.SearchFilter.Operator.EQ;
import static software.wings.beans.SearchFilter.Operator.IN;
import static software.wings.beans.ServiceTemplate.Builder.aServiceTemplate;
import static software.wings.dl.PageRequest.PageRequestBuilder.aPageRequest;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.Key;
import org.mongodb.morphia.annotations.Transient;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.ConfigFile;
import software.wings.beans.Environment;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.Service;
import software.wings.beans.ServiceTemplate;
import software.wings.beans.ServiceVariable;
import software.wings.beans.ServiceVariable.Type;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.ConfigService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.HostService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.ServiceInstanceService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.ServiceTemplateService;
import software.wings.service.intfc.ServiceVariableService;
import software.wings.service.intfc.security.ManagerDecryptionService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.utils.ArtifactType;
import software.wings.utils.Validator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;
import javax.validation.executable.ValidateOnExecution;
/**
 * Created by anubhaw on 4/4/16.
 */
@ValidateOnExecution
@Singleton
public class ServiceTemplateServiceImpl implements ServiceTemplateService {
  private static final Logger logger = LoggerFactory.getLogger(ServiceTemplateServiceImpl.class);
  @Inject private WingsPersistence wingsPersistence;
  @Inject private ConfigService configService;
  @Inject private ServiceVariableService serviceVariableService;
  @Inject private ServiceInstanceService serviceInstanceService;
  @Inject private ExecutorService executorService;
  @Inject private ServiceResourceService serviceResourceService;
  @Inject private EnvironmentService environmentService;
  @Inject private InfrastructureMappingService infrastructureMappingService;
  @Inject private HostService hostService;
  @Transient @Inject private transient SecretManager secretManager;

  @Transient @Inject private transient ManagerDecryptionService managerDecryptionService;

  /* (non-Javadoc)
   * @see software.wings.service.intfc.ServiceTemplateService#list(software.wings.dl.PageRequest)
   */
  @Override
  public PageResponse<ServiceTemplate> list(
      PageRequest<ServiceTemplate> pageRequest, boolean withDetails, boolean maskEncryptedFields) {
    PageResponse<ServiceTemplate> pageResponse = wingsPersistence.query(ServiceTemplate.class, pageRequest);
    List<ServiceTemplate> serviceTemplates = pageResponse.getResponse();
    setArtifactTypeAndInfraMappings(serviceTemplates);

    if (withDetails) {
      serviceTemplates.forEach(serviceTemplate -> {
        try {
          populateServiceAndOverrideConfigFiles(serviceTemplate);
        } catch (Exception e) {
          logger.error(
              "Failed to populate the service and override config files for service template {} ", serviceTemplate, e);
        }
        try {
          populateServiceAndOverrideServiceVariables(serviceTemplate, maskEncryptedFields);
        } catch (Exception e) {
          logger.error("Failed to populate the service and service variable overrides for service template {} ",
              serviceTemplate, e);
        }
        try {
          populateServiceAndOverrideConfigMapYamls(serviceTemplate);
        } catch (Exception e) {
          logger.error("Failed to populate the service and override config map yamls for service template {} ",
              serviceTemplate, e);
        }
        try {
          populateServiceAndOverrideHelmValueYamls(serviceTemplate);
        } catch (Exception e) {
          logger.error("Failed to populate the service and override helm value yamls for service template {} ",
              serviceTemplate, e);
        }
      });
    }

    return pageResponse;
  }

  private void setArtifactTypeAndInfraMappings(List<ServiceTemplate> serviceTemplates) {
    if (isEmpty(serviceTemplates)) {
      return;
    }
    String appId = serviceTemplates.get(0).getAppId();
    String envId = serviceTemplates.get(0).getEnvId();

    ImmutableMap<String, ServiceTemplate> serviceTemplateMap =
        Maps.uniqueIndex(serviceTemplates, ServiceTemplate::getUuid);

    List<Service> services = serviceResourceService.findServicesByApp(appId);
    ImmutableMap<String, Service> serviceMap = Maps.uniqueIndex(services, Service::getUuid);
    serviceTemplateMap.forEach((serviceTemplateId, serviceTemplate) -> {
      Service tempService = serviceMap.get(serviceTemplate.getServiceId());
      serviceTemplate.setServiceArtifactType(tempService != null ? tempService.getArtifactType() : ArtifactType.OTHER);
    });

    PageRequest<InfrastructureMapping> infraPageRequest =
        aPageRequest()
            .addFilter("appId", EQ, appId)
            .addFilter("envId", EQ, envId)
            .addFilter("serviceTemplateId", IN, serviceTemplateMap.keySet().toArray())
            .build();
    List<InfrastructureMapping> infrastructureMappings =
        infrastructureMappingService.list(infraPageRequest).getResponse();
    Map<String, List<InfrastructureMapping>> infraMappingListByTemplateId =
        infrastructureMappings.stream().collect(Collectors.groupingBy(InfrastructureMapping::getServiceTemplateId));
    infraMappingListByTemplateId.forEach(
        (templateId, infrastructureMappingList)
            -> serviceTemplateMap.get(templateId).setInfrastructureMappings(infrastructureMappingList));
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.ServiceTemplateService#save(software.wings.beans.ServiceTemplate)
   */
  @Override
  public ServiceTemplate save(ServiceTemplate serviceTemplate) {
    return Validator.duplicateCheck(
        () -> wingsPersistence.saveAndGet(ServiceTemplate.class, serviceTemplate), "name", serviceTemplate.getName());
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.ServiceTemplateService#update(software.wings.beans.ServiceTemplate)
   */
  @Override
  public ServiceTemplate update(ServiceTemplate serviceTemplate) {
    wingsPersistence.updateFields(ServiceTemplate.class, serviceTemplate.getUuid(),
        ImmutableMap.of("name", serviceTemplate.getName(), "description", serviceTemplate.getDescription()));
    return get(serviceTemplate.getAppId(), serviceTemplate.getEnvId(), serviceTemplate.getUuid(), true, true);
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.ServiceTemplateService#get(java.lang.String, java.lang.String, java.lang.String)
   */
  @Override
  public ServiceTemplate get(
      String appId, String envId, String serviceTemplateId, boolean withDetails, boolean maskEncryptedFields) {
    ServiceTemplate serviceTemplate = get(appId, serviceTemplateId);
    if (serviceTemplate != null) {
      setArtifactTypeAndInfraMappings(asList(serviceTemplate));
      if (withDetails) {
        populateServiceAndOverrideConfigFiles(serviceTemplate);
        populateServiceAndOverrideServiceVariables(serviceTemplate, maskEncryptedFields);
        populateServiceAndOverrideConfigMapYamls(serviceTemplate);
        populateServiceAndOverrideHelmValueYamls(serviceTemplate);
      }
    }
    return serviceTemplate;
  }

  @Override
  public ServiceTemplate get(String appId, String serviceTemplateId) {
    return wingsPersistence.get(ServiceTemplate.class, appId, serviceTemplateId);
  }

  @Override
  public ServiceTemplate get(@NotEmpty String appId, @NotEmpty String serviceId, @NotEmpty String environmentId) {
    return wingsPersistence.createQuery(ServiceTemplate.class)
        .filter(ServiceTemplate.APP_ID_KEY, appId)
        .filter(ServiceTemplate.SERVICE_ID_KEY, serviceId)
        .filter(ServiceTemplate.ENVIRONMENT_ID_KEY, environmentId)
        .get();
  }

  @Override
  public List<Key<ServiceTemplate>> getTemplateRefKeysByService(String appId, String serviceId, String envId) {
    Query<ServiceTemplate> templateQuery = wingsPersistence.createQuery(ServiceTemplate.class)
                                               .filter(ServiceTemplate.APP_ID_KEY, appId)
                                               .filter(ServiceTemplate.SERVICE_ID_KEY, serviceId);

    if (isNotBlank(envId)) {
      templateQuery.filter("envId", envId);
    }
    return templateQuery.asKeyList();
  }

  @Override
  public void updateDefaultServiceTemplateName(
      String appId, String serviceId, String oldServiceName, String newServiceName) {
    Query<ServiceTemplate> query = wingsPersistence.createQuery(ServiceTemplate.class)
                                       .filter(ServiceTemplate.APP_ID_KEY, appId)
                                       .filter(ServiceTemplate.SERVICE_ID_KEY, serviceId)
                                       .filter("defaultServiceTemplate", true)
                                       .filter("name", oldServiceName);
    UpdateOperations<ServiceTemplate> updateOperations =
        wingsPersistence.createUpdateOperations(ServiceTemplate.class).set("name", newServiceName);
    wingsPersistence.update(query, updateOperations);
  }

  @Override
  public boolean exist(String appId, String templateId) {
    return wingsPersistence.createQuery(ServiceTemplate.class)
               .filter(ServiceTemplate.APP_ID_KEY, appId)
               .filter(ID_KEY, templateId)
               .getKey()
        != null;
  }

  private void populateServiceAndOverrideConfigFiles(ServiceTemplate template) {
    List<ConfigFile> serviceConfigFiles =
        configService.getConfigFilesForEntity(template.getAppId(), DEFAULT_TEMPLATE_ID, template.getServiceId());
    template.setServiceConfigFiles(serviceConfigFiles);

    List<ConfigFile> overrideConfigFiles =
        configService.getConfigFileByTemplate(template.getAppId(), template.getEnvId(), template.getUuid());

    ImmutableMap<String, ConfigFile> serviceConfigFilesMap = Maps.uniqueIndex(serviceConfigFiles, ConfigFile::getUuid);

    overrideConfigFiles.forEach(configFile -> {
      if (configFile.getParentConfigFileId() != null
          && serviceConfigFilesMap.containsKey(configFile.getParentConfigFileId())) {
        configFile.setOverriddenConfigFile(serviceConfigFilesMap.get(configFile.getParentConfigFileId()));
      }
    });
    template.setConfigFilesOverrides(overrideConfigFiles);
  }

  private void populateServiceAndOverrideServiceVariables(ServiceTemplate template, boolean maskEncryptedFields) {
    List<ServiceVariable> serviceVariables = serviceVariableService.getServiceVariablesForEntity(
        template.getAppId(), template.getServiceId(), maskEncryptedFields);
    template.setServiceVariables(serviceVariables);

    List<ServiceVariable> overrideServiceVariables = serviceVariableService.getServiceVariablesByTemplate(
        template.getAppId(), template.getEnvId(), template, maskEncryptedFields);

    ImmutableMap<String, ServiceVariable> serviceVariablesMap =
        Maps.uniqueIndex(serviceVariables, ServiceVariable::getUuid);

    overrideServiceVariables.forEach(serviceVariable -> {
      if (serviceVariable.getParentServiceVariableId() != null
          && serviceVariablesMap.containsKey(serviceVariable.getParentServiceVariableId())) {
        serviceVariable.setOverriddenServiceVariable(
            serviceVariablesMap.get(serviceVariable.getParentServiceVariableId()));
      }
    });
    template.setServiceVariablesOverrides(overrideServiceVariables);
  }

  private void populateServiceAndOverrideConfigMapYamls(ServiceTemplate template) {
    Environment env = environmentService.get(template.getAppId(), template.getEnvId(), false);
    Map<String, String> envConfigMaps = env.getConfigMapYamlByServiceTemplateId();
    if (isNotEmpty(envConfigMaps) && isNotBlank(envConfigMaps.get(template.getUuid()))) {
      template.setConfigMapYamlOverride(envConfigMaps.get(template.getUuid()));
    }
  }

  private void populateServiceAndOverrideHelmValueYamls(ServiceTemplate template) {
    Environment env = environmentService.get(template.getAppId(), template.getEnvId(), false);
    Map<String, String> envHelmValues = env.getHelmValueYamlByServiceTemplateId();
    if (isNotEmpty(envHelmValues) && isNotBlank(envHelmValues.get(template.getUuid()))) {
      template.setHelmValueYamlOverride(envHelmValues.get(template.getUuid()));
    }
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.ServiceTemplateService#delete(java.lang.String, java.lang.String,
   * java.lang.String)
   */
  @Override
  public void delete(String appId, String serviceTemplateId) {
    // TODO: move to the prune pattern
    boolean deleted = wingsPersistence.delete(wingsPersistence.createQuery(ServiceTemplate.class)
                                                  .filter(ServiceTemplate.APP_ID_KEY, appId)
                                                  .filter(ID_KEY, serviceTemplateId));
    if (deleted) {
      executorService.submit(() -> infrastructureMappingService.deleteByServiceTemplate(appId, serviceTemplateId));
      executorService.submit(() -> configService.deleteByTemplateId(appId, serviceTemplateId));
      executorService.submit(() -> serviceVariableService.deleteByTemplateId(appId, serviceTemplateId));
    }
  }

  @Override
  public void pruneByEnvironment(String appId, String envId) {
    List<Key<ServiceTemplate>> keys = wingsPersistence.createQuery(ServiceTemplate.class)
                                          .filter(ServiceTemplate.APP_ID_KEY, appId)
                                          .filter("envId", envId)
                                          .asKeyList();
    for (Key<ServiceTemplate> key : keys) {
      delete(appId, (String) key.getId());
    }
  }

  @Override
  public void pruneByService(String appId, String serviceId) {
    wingsPersistence.createQuery(ServiceTemplate.class)
        .filter(ServiceTemplate.APP_ID_KEY, appId)
        .filter(ServiceTemplate.SERVICE_ID_KEY, serviceId)
        .asList()
        .forEach(serviceTemplate -> delete(serviceTemplate.getAppId(), serviceTemplate.getUuid()));
  }

  @Override
  public void createDefaultTemplatesByEnv(Environment env) {
    List<Service> services = serviceResourceService.findServicesByApp(env.getAppId());
    services.forEach(service
        -> save(aServiceTemplate()
                    .withAppId(service.getAppId())
                    .withEnvId(env.getUuid())
                    .withServiceId(service.getUuid())
                    .withName(service.getName())
                    .withDefaultServiceTemplate(true)
                    .build()));
  }

  @Override
  public void createDefaultTemplatesByService(Service service) {
    List<Environment> environments = environmentService.getEnvByApp(service.getAppId());
    environments.forEach(environment
        -> save(aServiceTemplate()
                    .withAppId(service.getAppId())
                    .withEnvId(environment.getUuid())
                    .withServiceId(service.getUuid())
                    .withName(service.getName())
                    .withDefaultServiceTemplate(true)
                    .build()));
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.ServiceTemplateService#computedConfigFiles(java.lang.String, java.lang.String,
   * java.lang.String)
   */
  @Override
  public List<ConfigFile> computedConfigFiles(String appId, String envId, String templateId) {
    ServiceTemplate serviceTemplate = get(appId, envId, templateId, false, false);
    if (serviceTemplate == null) {
      return new ArrayList<>();
    }

    /* override order(left to right): Service -> Env: All Services -> Env: Service Template */

    List<ConfigFile> serviceConfigFiles =
        configService.getConfigFilesForEntity(appId, DEFAULT_TEMPLATE_ID, serviceTemplate.getServiceId(), envId);
    List<ConfigFile> allServiceConfigFiles =
        configService.getConfigFilesForEntity(appId, DEFAULT_TEMPLATE_ID, envId, envId);
    List<ConfigFile> templateConfigFiles =
        configService.getConfigFilesForEntity(appId, templateId, serviceTemplate.getUuid(), envId);

    return overrideConfigFiles(overrideConfigFiles(serviceConfigFiles, allServiceConfigFiles), templateConfigFiles);
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.ServiceTemplateService#computedConfigFiles(java.lang.String, java.lang.String,
   * java.lang.String)
   */
  @Override
  public List<ServiceVariable> computeServiceVariables(
      String appId, String envId, String templateId, String workflowExecutionId, boolean maskEncryptedFields) {
    ServiceTemplate serviceTemplate = get(appId, envId, templateId, false, false);
    if (serviceTemplate == null) {
      return new ArrayList<>();
    }

    List<ServiceVariable> serviceVariables =
        serviceVariableService.getServiceVariablesForEntity(appId, serviceTemplate.getServiceId(), maskEncryptedFields);
    List<ServiceVariable> allServiceVariables =
        serviceVariableService.getServiceVariablesForEntity(appId, envId, maskEncryptedFields);
    List<ServiceVariable> templateServiceVariables =
        serviceVariableService.getServiceVariablesForEntity(appId, serviceTemplate.getUuid(), maskEncryptedFields);

    return overrideServiceSettings(
        overrideServiceSettings(serviceVariables, allServiceVariables, appId, workflowExecutionId, maskEncryptedFields),
        templateServiceVariables, appId, workflowExecutionId, maskEncryptedFields);
  }

  @Override
  public String computeConfigMapYaml(String appId, String envId, String templateId) {
    ServiceTemplate serviceTemplate = get(appId, envId, templateId, false, false);
    if (serviceTemplate == null) {
      return null;
    }

    Service service = serviceResourceService.get(appId, serviceTemplate.getServiceId());
    Environment env = environmentService.get(appId, envId, false);

    String configMapYaml = service.getConfigMapYaml();

    if (isNotBlank(env.getConfigMapYaml())) {
      configMapYaml = env.getConfigMapYaml();
    }

    Map<String, String> envConfigMaps = env.getConfigMapYamlByServiceTemplateId();
    if (isNotEmpty(envConfigMaps) && isNotBlank(envConfigMaps.get(templateId))) {
      configMapYaml = envConfigMaps.get(templateId);
    }

    return configMapYaml;
  }

  @Override
  public List<String> helmValueOverridesYamlFiles(String appId, String templateId) {
    List<String> result = new ArrayList<>();

    ServiceTemplate serviceTemplate = get(appId, templateId);
    if (serviceTemplate != null) {
      Service service = serviceResourceService.get(appId, serviceTemplate.getServiceId());
      Environment env = environmentService.get(appId, serviceTemplate.getEnvId(), false);

      if (isNotBlank(service.getHelmValueYaml())) {
        result.add(service.getHelmValueYaml());
      }

      if (isNotBlank(env.getHelmValueYaml())) {
        result.add(env.getHelmValueYaml());
      }

      Map<String, String> envHelmValues = env.getHelmValueYamlByServiceTemplateId();
      if (isNotEmpty(envHelmValues) && isNotBlank(envHelmValues.get(templateId))) {
        result.add(envHelmValues.get(templateId));
      }
    }
    return result;
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.ServiceTemplateService#overrideConfigFiles(java.util.List, java.util.List)
   */
  @Override
  public List<ConfigFile> overrideConfigFiles(List<ConfigFile> existingFiles, List<ConfigFile> newFiles) {
    List<ConfigFile> mergedConfigFiles = existingFiles;

    if (!existingFiles.isEmpty() || !newFiles.isEmpty()) {
      logger.debug("Config files before overrides [{}]", existingFiles.toString());
      logger.debug("New override config files [{}]", newFiles != null ? newFiles.toString() : null);
      if (isNotEmpty(newFiles)) {
        mergedConfigFiles = concat(newFiles.stream(), existingFiles.stream())
                                .filter(new TreeSet<>(comparing(ConfigFile::getRelativeFilePath))::add)
                                .collect(toList());
      }
    }
    logger.debug("Config files after overrides [{}]", mergedConfigFiles.toString());
    return mergedConfigFiles;
  }

  private List<ServiceVariable> overrideServiceSettings(List<ServiceVariable> existingServiceVariables,
      List<ServiceVariable> newServiceVariables, String appId, String workflowExecutionId,
      boolean maskEncryptedFields) {
    List<ServiceVariable> mergedServiceSettings = existingServiceVariables;
    if (!existingServiceVariables.isEmpty() || !newServiceVariables.isEmpty()) {
      logger.debug("Service variables before overrides [{}]", existingServiceVariables.toString());
      logger.debug(
          "New override service variables [{}]", newServiceVariables != null ? newServiceVariables.toString() : null);
      if (isNotEmpty(newServiceVariables)) {
        mergedServiceSettings = concat(newServiceVariables.stream(), existingServiceVariables.stream())
                                    .filter(new TreeSet<>(comparing(ServiceVariable::getName))::add)
                                    .collect(toList());
      }
    }
    logger.debug("Service variables after overrides [{}]", mergedServiceSettings.toString());
    if (!maskEncryptedFields) {
      mergedServiceSettings.forEach(serviceVariable -> {
        if (serviceVariable.getType() == Type.ENCRYPTED_TEXT) {
          managerDecryptionService.decrypt(
              serviceVariable, secretManager.getEncryptionDetails(serviceVariable, appId, workflowExecutionId));
        }
      });
    }
    return mergedServiceSettings;
  }
}
