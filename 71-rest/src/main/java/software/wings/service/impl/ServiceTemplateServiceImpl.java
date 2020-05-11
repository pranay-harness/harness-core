package software.wings.service.impl;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.beans.SearchFilter.Operator.EQ;
import static io.harness.beans.SearchFilter.Operator.IN;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.util.Arrays.asList;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.concat;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.beans.ConfigFile.DEFAULT_TEMPLATE_ID;
import static software.wings.beans.ServiceTemplate.Builder.aServiceTemplate;
import static software.wings.beans.appmanifest.AppManifestKind.HELM_CHART_OVERRIDE;
import static software.wings.beans.appmanifest.ManifestFile.VALUES_YAML_KEY;
import static software.wings.service.intfc.ServiceVariableService.EncryptedFieldMode.MASKED;
import static software.wings.service.intfc.ServiceVariableService.EncryptedFieldMode.OBTAIN_VALUE;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.security.encryption.EncryptableSettingWithEncryptionDetails;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.security.encryption.EncryptionConfig;
import io.harness.validation.PersistenceValidator;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.Key;
import org.mongodb.morphia.annotations.Transient;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import software.wings.beans.ConfigFile;
import software.wings.beans.Environment;
import software.wings.beans.FeatureName;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.Service;
import software.wings.beans.ServiceTemplate;
import software.wings.beans.ServiceTemplate.ServiceTemplateKeys;
import software.wings.beans.ServiceVariable;
import software.wings.beans.ServiceVariable.Type;
import software.wings.beans.appmanifest.AppManifestKind;
import software.wings.beans.appmanifest.ApplicationManifest;
import software.wings.beans.appmanifest.ManifestFile;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ApplicationManifestService;
import software.wings.service.intfc.ArtifactStreamServiceBindingService;
import software.wings.service.intfc.ConfigService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.HostService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.ServiceInstanceService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.ServiceTemplateService;
import software.wings.service.intfc.ServiceVariableService;
import software.wings.service.intfc.ServiceVariableService.EncryptedFieldMode;
import software.wings.service.intfc.security.ManagerDecryptionService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.utils.ArtifactType;

import java.util.ArrayList;
import java.util.HashMap;
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
@Slf4j
public class ServiceTemplateServiceImpl implements ServiceTemplateService {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private ConfigService configService;
  @Inject private ServiceVariableService serviceVariableService;
  @Inject private ServiceInstanceService serviceInstanceService;
  @Inject private ExecutorService executorService;
  @Inject private ServiceResourceService serviceResourceService;
  @Inject private EnvironmentService environmentService;
  @Inject private InfrastructureMappingService infrastructureMappingService;
  @Inject private HostService hostService;
  @Inject private AppService appService;
  @Inject private FeatureFlagService featureFlagService;
  @Inject private ApplicationManifestService applicationManifestService;
  @Inject private ArtifactStreamServiceBindingService artifactStreamServiceBindingService;
  @Transient @Inject private transient SecretManager secretManager;

  @Transient @Inject private transient ManagerDecryptionService managerDecryptionService;

  /* (non-Javadoc)
   * @see software.wings.service.intfc.ServiceTemplateService#list(software.wings.dl.PageRequest)
   */
  @Override
  public PageResponse<ServiceTemplate> list(
      PageRequest<ServiceTemplate> pageRequest, boolean withDetails, EncryptedFieldMode encryptedFieldMode) {
    PageResponse<ServiceTemplate> pageResponse = wingsPersistence.query(ServiceTemplate.class, pageRequest);
    List<ServiceTemplate> serviceTemplates = pageResponse.getResponse();
    setArtifactTypeAndInfraMappings(serviceTemplates);

    if (withDetails) {
      long startTime = System.currentTimeMillis();
      setConfigs(encryptedFieldMode, serviceTemplates);
      logger.info("Total time taken to load all the configs {}", System.currentTimeMillis() - startTime);
    }

    return pageResponse;
  }

  private void setConfigs(EncryptedFieldMode encryptedFieldMode, List<ServiceTemplate> serviceTemplates) {
    serviceTemplates.forEach(serviceTemplate -> {
      long startTime;
      try {
        startTime = System.currentTimeMillis();
        populateServiceAndOverrideConfigFiles(serviceTemplate);
        logger.info("Total time taken to load ServiceOverrideConfigFiles for one ServiceTemplate Id {} is {},",
            serviceTemplate.getUuid(), System.currentTimeMillis() - startTime);
      } catch (Exception e) {
        logger.error(
            "Failed to populate the service and override config files for service template {} ", serviceTemplate, e);
      }
      try {
        startTime = System.currentTimeMillis();
        populateServiceAndOverrideServiceVariables(serviceTemplate, encryptedFieldMode);
        logger.info("Total time taken to load ServiceAndOverrideServiceVariables for one ServiceTemplate Id {} is {},",
            serviceTemplate.getUuid(), System.currentTimeMillis() - startTime);

      } catch (Exception e) {
        logger.error("Failed to populate the service and service variable overrides for service template {} ",
            serviceTemplate, e);
      }
      try {
        startTime = System.currentTimeMillis();
        populateServiceAndOverrideConfigMapYamls(serviceTemplate);
        logger.info("Total time taken to load ServiceAndOverrideConfigMapYaml for one ServiceTemplate Id {} is {},",
            serviceTemplate.getUuid(), System.currentTimeMillis() - startTime);
      } catch (Exception e) {
        logger.error("Failed to populate the service and override config map yamls for service template {} ",
            serviceTemplate, e);
      }
      try {
        startTime = System.currentTimeMillis();
        populateServiceAndOverrideHelmValueYamls(serviceTemplate);
        logger.info("Total time taken to load ServiceAndOverrideHelmValueYamls for one ServiceTemplate Id {} is {},",
            serviceTemplate.getUuid(), System.currentTimeMillis() - startTime);
      } catch (Exception e) {
        logger.error("Failed to populate the service and override helm value yamls for service template {} ",
            serviceTemplate, e);
      }
      try {
        startTime = System.currentTimeMillis();
        populateServiceAndOverrideAppManifest(serviceTemplate);
        logger.info("Total time taken to load ServiceAndOverrideValuesAppManifest for one ServiceTemplate Id {} is {},",
            serviceTemplate.getUuid(), System.currentTimeMillis() - startTime);
      } catch (Exception e) {
        logger.error("Failed to populate the service and override application manifest for service template {} ",
            serviceTemplate, e);
      }
      try {
        startTime = System.currentTimeMillis();
        populateServiceAndOverrideValuesManifestFile(serviceTemplate);
        logger.info("Total time taken to load ServiceAndOverrideValuesManifestFil for one ServiceTemplate Id {} is {},",
            serviceTemplate.getUuid(), System.currentTimeMillis() - startTime);
      } catch (Exception e) {
        logger.error(
            "Failed to populate the service and override manifest file for service template {} ", serviceTemplate, e);
      }
    });
  }

  @Override
  public PageResponse<ServiceTemplate> listWithoutServiceAndInfraMappingSummary(
      PageRequest<ServiceTemplate> pageRequest, boolean withDetails, EncryptedFieldMode encryptedFieldMode) {
    PageResponse<ServiceTemplate> pageResponse = wingsPersistence.query(ServiceTemplate.class, pageRequest);
    List<ServiceTemplate> serviceTemplates = pageResponse.getResponse();
    if (withDetails) {
      setConfigs(encryptedFieldMode, serviceTemplates);
    }
    return pageResponse;
  }

  private void setArtifactTypeAndInfraMappings(List<ServiceTemplate> serviceTemplates) {
    if (isEmpty(serviceTemplates)) {
      return;
    }
    String appId = serviceTemplates.get(0).getAppId();
    String envId = serviceTemplates.get(0).getEnvId();

    boolean infraRefactor = featureFlagService.isEnabled(FeatureName.INFRA_MAPPING_REFACTOR, appId);

    ImmutableMap<String, ServiceTemplate> serviceTemplateMap =
        Maps.uniqueIndex(serviceTemplates, ServiceTemplate::getUuid);

    List<String> serviceIds = new ArrayList<>();
    serviceTemplates.forEach(serviceTemplate -> {
      if (serviceTemplate.getServiceId() != null && !serviceIds.contains(serviceTemplate.getServiceId())) {
        serviceIds.add(serviceTemplate.getServiceId());
      }
    });

    List<Service> services = serviceResourceService.fetchServicesByUuids(appId, serviceIds);
    if (isNotEmpty(services)) {
      for (Service service : services) {
        if (isBlank(service.getUuid())) {
          logger.info("Null Service name {} uuid {} appId {} accountId {}", service.getName(), service.getUuid(),
              service.getAppId(), service.getAccountId());
        }
      }
    }

    ImmutableMap<String, Service> serviceMap;
    try {
      serviceMap = Maps.uniqueIndex(services, Service::getUuid);
    } catch (Exception ex) {
      logger.warn("Logging services in case of NPE");
      for (Service service : services) {
        logger.info("Service name {} uuid {} appId {} accountId {}", service.getName(), service.getUuid(),
            service.getAppId(), service.getAccountId());
      }

      throw ex;
    }

    serviceTemplateMap.forEach((serviceTemplateId, serviceTemplate) -> {
      Service tempService = serviceMap.get(serviceTemplate.getServiceId());
      serviceTemplate.setServiceArtifactType(tempService != null ? tempService.getArtifactType() : ArtifactType.OTHER);
    });
    if (infraRefactor) {
      serviceTemplateMap.forEach((templateId, serviceTemplate)
                                     -> serviceTemplate.setInfrastructureMappings(getInfraMappingsFromServiceTemplate(
                                         serviceTemplate.getAppId(), serviceTemplate.getUuid())));
    } else {
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
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.ServiceTemplateService#save(software.wings.beans.ServiceTemplate)
   */
  @Override
  public ServiceTemplate save(ServiceTemplate serviceTemplate) {
    return PersistenceValidator.duplicateCheck(
        () -> wingsPersistence.saveAndGet(ServiceTemplate.class, serviceTemplate), "name", serviceTemplate.getName());
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.ServiceTemplateService#update(software.wings.beans.ServiceTemplate)
   */
  @Override
  public ServiceTemplate update(ServiceTemplate serviceTemplate) {
    wingsPersistence.updateFields(ServiceTemplate.class, serviceTemplate.getUuid(),
        ImmutableMap.of("name", serviceTemplate.getName(), "description", serviceTemplate.getDescription()));
    return get(serviceTemplate.getAppId(), serviceTemplate.getEnvId(), serviceTemplate.getUuid(), true, MASKED);
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.ServiceTemplateService#get(java.lang.String, java.lang.String, java.lang.String)
   */
  @Override
  public ServiceTemplate get(String appId, String envId, String serviceTemplateId, boolean withDetails,
      EncryptedFieldMode encryptedFieldMode) {
    ServiceTemplate serviceTemplate = get(appId, serviceTemplateId);
    if (serviceTemplate != null) {
      setArtifactTypeAndInfraMappings(asList(serviceTemplate));
      if (withDetails) {
        populateServiceAndOverrideConfigFiles(serviceTemplate);
        populateServiceAndOverrideServiceVariables(serviceTemplate, encryptedFieldMode);
        populateServiceAndOverrideConfigMapYamls(serviceTemplate);
        populateServiceAndOverrideHelmValueYamls(serviceTemplate);
        populateServiceAndOverrideAppManifest(serviceTemplate);
        populateServiceAndOverrideValuesManifestFile(serviceTemplate);
      }
    }
    return serviceTemplate;
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.ServiceTemplateService#get(java.lang.String, java.lang.String, java.lang.String)
   */
  @Override
  public ServiceTemplate get(
      String appId, String serviceTemplateId, boolean withDetails, EncryptedFieldMode encryptedFieldMode) {
    ServiceTemplate serviceTemplate = get(appId, serviceTemplateId);
    if (serviceTemplate != null) {
      setArtifactTypeAndInfraMappings(asList(serviceTemplate));
      if (withDetails) {
        populateServiceAndOverrideConfigFiles(serviceTemplate);
        populateServiceAndOverrideServiceVariables(serviceTemplate, encryptedFieldMode);
        populateServiceAndOverrideConfigMapYamls(serviceTemplate);
        populateServiceAndOverrideHelmValueYamls(serviceTemplate);
        populateServiceAndOverrideAppManifest(serviceTemplate);
        populateServiceAndOverrideValuesManifestFile(serviceTemplate);
      }
    }
    return serviceTemplate;
  }

  @Override
  public ServiceTemplate get(String appId, String serviceTemplateId) {
    return wingsPersistence.getWithAppId(ServiceTemplate.class, appId, serviceTemplateId);
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
  public ServiceTemplate getOrCreate(@NotEmpty String appId, @NotEmpty String serviceId, @NotEmpty String envId) {
    // NOTE: This function is a workaround for the case where some service templates were not created properly. So
    // before critical operations that require a service template, we try to fetch a service template and if it's not
    // present we create one and return that.
    ServiceTemplate serviceTemplate = get(appId, serviceId, envId);
    if (serviceTemplate != null) {
      return serviceTemplate;
    }

    String serviceName = serviceResourceService.getName(appId, serviceId);
    if (serviceName == null || !environmentService.exist(appId, envId)) {
      return null;
    }

    return save(aServiceTemplate()
                    .withAppId(appId)
                    .withEnvId(envId)
                    .withServiceId(serviceId)
                    .withName(serviceName)
                    .withDefaultServiceTemplate(true)
                    .build());
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
                                       .filter(ServiceTemplateKeys.defaultServiceTemplate, true)
                                       .filter(ServiceTemplateKeys.name, oldServiceName);
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

  private void populateServiceAndOverrideServiceVariables(
      ServiceTemplate template, EncryptedFieldMode encryptedFieldMode) {
    List<ServiceVariable> serviceVariables = serviceVariableService.getServiceVariablesForEntity(
        template.getAppId(), template.getServiceId(), encryptedFieldMode);
    artifactStreamServiceBindingService.processServiceVariables(serviceVariables);
    template.setServiceVariables(serviceVariables);

    List<ServiceVariable> overrideServiceVariables = serviceVariableService.getServiceVariablesByTemplate(
        template.getAppId(), template.getEnvId(), template, encryptedFieldMode);

    ImmutableMap<String, ServiceVariable> serviceVariablesMap =
        Maps.uniqueIndex(serviceVariables, ServiceVariable::getUuid);

    overrideServiceVariables.forEach(serviceVariable -> {
      if (serviceVariable.getParentServiceVariableId() != null
          && serviceVariablesMap.containsKey(serviceVariable.getParentServiceVariableId())) {
        serviceVariable.setOverriddenServiceVariable(
            serviceVariablesMap.get(serviceVariable.getParentServiceVariableId()));
      }
    });
    artifactStreamServiceBindingService.processServiceVariables(overrideServiceVariables);
    template.setServiceVariablesOverrides(overrideServiceVariables);
  }

  private void populateServiceAndOverrideConfigMapYamls(ServiceTemplate template) {
    Environment env = environmentService.get(template.getAppId(), template.getEnvId(), false);
    if (env == null) {
      return;
    }
    Map<String, String> envConfigMaps = env.getConfigMapYamlByServiceTemplateId();
    if (isNotEmpty(envConfigMaps) && isNotBlank(envConfigMaps.get(template.getUuid()))) {
      template.setConfigMapYamlOverride(envConfigMaps.get(template.getUuid()));
    }
  }

  private void populateServiceAndOverrideHelmValueYamls(ServiceTemplate template) {
    Environment env = environmentService.get(template.getAppId(), template.getEnvId(), false);
    if (env == null) {
      return;
    }

    ApplicationManifest applicationManifest = applicationManifestService.getByEnvAndServiceId(
        template.getAppId(), template.getEnvId(), template.getServiceId(), AppManifestKind.VALUES);
    String valuesYaml = getValuesFileContent(applicationManifest);
    template.setHelmValueYamlOverride(valuesYaml);
  }

  private void populateServiceAndOverrideAppManifest(ServiceTemplate serviceTemplate) {
    Service service = serviceResourceService.get(serviceTemplate.getAppId(), serviceTemplate.getServiceId());
    if (service == null) {
      return;
    }

    AppManifestKind appManifestKind = AppManifestKind.VALUES;
    if (ArtifactType.PCF == service.getArtifactType()) {
      appManifestKind = AppManifestKind.PCF_OVERRIDE;
    }

    serviceTemplate.setValuesOverrideAppManifest(applicationManifestService.getAppManifest(
        serviceTemplate.getAppId(), serviceTemplate.getEnvId(), serviceTemplate.getServiceId(), appManifestKind));

    // This is Helm_Chart_Override which is only supported for Service level override
    if (service.isK8sV2()) {
      serviceTemplate.setHelmChartOverride(applicationManifestService.getAppManifest(
          serviceTemplate.getAppId(), serviceTemplate.getEnvId(), serviceTemplate.getServiceId(), HELM_CHART_OVERRIDE));

      ApplicationManifest ocParamsOverrideAppManifest =
          applicationManifestService.getAppManifest(serviceTemplate.getAppId(), serviceTemplate.getEnvId(),
              serviceTemplate.getServiceId(), AppManifestKind.OC_PARAMS);
      serviceTemplate.setOcParamsOverrideAppManifest(ocParamsOverrideAppManifest);

      if (ocParamsOverrideAppManifest != null) {
        serviceTemplate.setOcParamsOverrideFile(applicationManifestService.getManifestFileByFileName(
            ocParamsOverrideAppManifest.getUuid(), AppManifestKind.OC_PARAMS.getDefaultFileName()));
      }
    }
  }

  private void populateServiceAndOverrideValuesManifestFile(ServiceTemplate template) {
    Service service = serviceResourceService.get(template.getAppId(), template.getServiceId());
    if (service == null) {
      return;
    }

    AppManifestKind appManifestKind = AppManifestKind.VALUES;
    if (ArtifactType.PCF == service.getArtifactType()) {
      appManifestKind = AppManifestKind.PCF_OVERRIDE;
    }

    ApplicationManifest appManifest = applicationManifestService.getAppManifest(
        template.getAppId(), template.getEnvId(), template.getServiceId(), appManifestKind);
    if (appManifest == null) {
      return;
    }

    List<ManifestFile> manifestFiles =
        applicationManifestService.getManifestFilesByAppManifestId(appManifest.getAppId(), appManifest.getUuid());
    if (isEmpty(manifestFiles)) {
      return;
    }

    template.setValuesOverrideManifestFile(manifestFiles.get(0));
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
      executorService.submit(() -> environmentService.deleteConfigMapYamlByServiceTemplateId(appId, serviceTemplateId));
    }
  }

  @Override
  public void pruneByEnvironment(String appId, String envId) {
    List<Key<ServiceTemplate>> keys = wingsPersistence.createQuery(ServiceTemplate.class)
                                          .filter(ServiceTemplate.APP_ID_KEY, appId)
                                          .filter(ServiceTemplateKeys.envId, envId)
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
    List<Service> services = serviceResourceService.findServicesByAppInternal(env.getAppId());
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
    List<String> environments = environmentService.getEnvIdsByApp(service.getAppId());
    environments.forEach(environment
        -> save(aServiceTemplate()
                    .withAppId(service.getAppId())
                    .withEnvId(environment)
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
    ServiceTemplate serviceTemplate = get(appId, envId, templateId, false, OBTAIN_VALUE);
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
  public List<ServiceVariable> computeServiceVariables(String appId, String envId, String templateId,
      String workflowExecutionId, EncryptedFieldComputeMode encryptedFieldComputeMode) {
    EncryptedFieldMode encryptedFieldMode =
        encryptedFieldComputeMode == EncryptedFieldComputeMode.MASKED ? MASKED : OBTAIN_VALUE;

    ServiceTemplate serviceTemplate = get(appId, envId, templateId, false, encryptedFieldMode);
    if (serviceTemplate == null) {
      return new ArrayList<>();
    }

    List<ServiceVariable> serviceVariables =
        serviceVariableService.getServiceVariablesForEntity(appId, serviceTemplate.getServiceId(), encryptedFieldMode);
    List<ServiceVariable> allServiceVariables =
        serviceVariableService.getServiceVariablesForEntity(appId, envId, encryptedFieldMode);
    List<ServiceVariable> templateServiceVariables =
        serviceVariableService.getServiceVariablesForEntity(appId, serviceTemplate.getUuid(), encryptedFieldMode);

    final List<ServiceVariable> mergedVariables = overrideServiceSettings(
        overrideServiceSettings(serviceVariables, allServiceVariables, appId, workflowExecutionId, encryptedFieldMode),
        templateServiceVariables, appId, workflowExecutionId, encryptedFieldMode);

    if (encryptedFieldComputeMode == EncryptedFieldComputeMode.OBTAIN_VALUE) {
      obtainEncryptedValues(appId, workflowExecutionId, mergedVariables);
    }

    return mergedVariables;
  }

  @Override
  public String computeConfigMapYaml(String appId, String envId, String templateId) {
    ServiceTemplate serviceTemplate = get(appId, envId, templateId, false, OBTAIN_VALUE);
    if (serviceTemplate == null) {
      return null;
    }

    Service service = serviceResourceService.getWithDetails(appId, serviceTemplate.getServiceId());
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
      EncryptedFieldMode encryptedFieldMode) {
    List<ServiceVariable> mergedServiceSettings = existingServiceVariables;
    if (!existingServiceVariables.isEmpty() || !newServiceVariables.isEmpty()) {
      if (logger.isDebugEnabled()) {
        logger.debug("Service variables before overrides [{}]", existingServiceVariables.toString());
        logger.debug(
            "New override service variables [{}]", newServiceVariables != null ? newServiceVariables.toString() : null);
      }

      if (isNotEmpty(newServiceVariables)) {
        mergedServiceSettings = concat(newServiceVariables.stream(), existingServiceVariables.stream())
                                    .filter(new TreeSet<>(comparing(ServiceVariable::getName))::add)
                                    .collect(toList());
      }
    }

    if (logger.isDebugEnabled()) {
      logger.debug("Service variables after overrides [{}]", mergedServiceSettings.toString());
    }
    return mergedServiceSettings;
  }

  private void obtainEncryptedValues(String appId, String workflowExecutionId, List<ServiceVariable> serviceVariables) {
    String accountId = appService.get(appId).getAccountId();
    Map<EncryptionConfig, List<EncryptableSettingWithEncryptionDetails>> encryptableSettingDetailsMap = new HashMap<>();
    serviceVariables.forEach(serviceVariable -> {
      if (serviceVariable.getType() == Type.ENCRYPTED_TEXT) {
        if (isEmpty(serviceVariable.getAccountId())) {
          serviceVariable.setAccountId(accountId);
        }

        List<EncryptedDataDetail> encryptedDataDetails =
            secretManager.getEncryptionDetails(serviceVariable, appId, workflowExecutionId);
        if (isNotEmpty(encryptedDataDetails)) {
          EncryptableSettingWithEncryptionDetails encryptableSettingWithEncryptionDetails =
              EncryptableSettingWithEncryptionDetails.builder()
                  .encryptableSetting(serviceVariable)
                  .encryptedDataDetails(encryptedDataDetails)
                  .build();
          EncryptionConfig encryptionConfig = encryptedDataDetails.get(0).getEncryptionConfig();
          List<EncryptableSettingWithEncryptionDetails> encryptableSettingWithEncryptionDetailsList =
              encryptableSettingDetailsMap.get(encryptionConfig);
          if (encryptableSettingWithEncryptionDetailsList == null) {
            encryptableSettingWithEncryptionDetailsList = new ArrayList<>();
            encryptableSettingDetailsMap.put(encryptionConfig, encryptableSettingWithEncryptionDetailsList);
          }
          encryptableSettingWithEncryptionDetailsList.add(encryptableSettingWithEncryptionDetails);
        }
      }
    });
    // PL-3926: Run batch decrypt through one single delegate task if feature enabled. Also break down the full list
    // by smaller batches aggregated by the same secret manager.
    for (List<EncryptableSettingWithEncryptionDetails> encryptableSettingWithEncryptionDetailsList :
        encryptableSettingDetailsMap.values()) {
      managerDecryptionService.decrypt(accountId, encryptableSettingWithEncryptionDetailsList);
    }
  }

  @Override
  public ConfigFile computedConfigFileByRelativeFilePath(
      String appId, String envId, String templateId, String relativeFilePath) {
    ServiceTemplate serviceTemplate = get(appId, envId, templateId, false, OBTAIN_VALUE);
    if (serviceTemplate == null) {
      return null;
    }

    /* override order(left to right): Service -> Env: All Services -> Env: Service Template */

    ConfigFile serviceConfigFile = configService.getConfigFileForEntityByRelativeFilePath(
        appId, DEFAULT_TEMPLATE_ID, serviceTemplate.getServiceId(), envId, relativeFilePath);
    ConfigFile allServiceConfigFile = configService.getConfigFileForEntityByRelativeFilePath(
        appId, DEFAULT_TEMPLATE_ID, envId, envId, relativeFilePath);
    ConfigFile templateConfigFile = configService.getConfigFileForEntityByRelativeFilePath(
        appId, templateId, serviceTemplate.getUuid(), envId, relativeFilePath);
    List<ConfigFile> configFiles =
        overrideConfigFiles(overrideConfigFiles(serviceConfigFile != null ? asList(serviceConfigFile) : asList(),
                                allServiceConfigFile != null ? asList(allServiceConfigFile) : asList()),
            templateConfigFile != null ? asList(templateConfigFile) : asList());
    return configFiles.isEmpty() ? null : configFiles.get(0);
  }

  @Override
  public List<InfrastructureMapping> getInfraMappingsFromServiceTemplate(String appId, String serviceTemplateId) {
    ServiceTemplate serviceTemplate = get(appId, serviceTemplateId);
    if (serviceTemplate == null) {
      return null;
    }

    PageRequest<InfrastructureMapping> infraPageRequest =
        aPageRequest()
            .addFilter("appId", EQ, appId)
            .addFilter("envId", EQ, serviceTemplate.getEnvId())
            .addFilter("serviceId", EQ, serviceTemplate.getServiceId())
            .build();
    return infrastructureMappingService.list(infraPageRequest).getResponse();
  }

  private String getValuesFileContent(ApplicationManifest applicationManifest) {
    if (applicationManifest != null) {
      ManifestFile manifestFile =
          applicationManifestService.getManifestFileByFileName(applicationManifest.getUuid(), VALUES_YAML_KEY);
      if (manifestFile != null) {
        return manifestFile.getFileContent();
      }
    }

    return null;
  }
}
