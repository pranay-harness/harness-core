package software.wings.service.impl;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.data.structure.ListUtil.trimList;
import static java.time.Duration.ofSeconds;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.beans.Base.GLOBAL_ENV_ID;
import static software.wings.beans.EntityType.ENVIRONMENT;
import static software.wings.beans.EntityType.SERVICE;
import static software.wings.beans.EntityType.SERVICE_TEMPLATE;
import static software.wings.beans.Environment.Builder.anEnvironment;
import static software.wings.beans.Environment.EnvironmentType.NON_PROD;
import static software.wings.beans.Environment.EnvironmentType.PROD;
import static software.wings.beans.ErrorCode.INVALID_ARGUMENT;
import static software.wings.beans.ErrorCode.INVALID_REQUEST;
import static software.wings.beans.InformationNotification.Builder.anInformationNotification;
import static software.wings.beans.SearchFilter.Operator.EQ;
import static software.wings.beans.SearchFilter.Operator.IN;
import static software.wings.beans.ServiceVariable.DEFAULT_TEMPLATE_ID;
import static software.wings.beans.ServiceVariable.Type.ENCRYPTED_TEXT;
import static software.wings.dl.PageRequest.PageRequestBuilder.aPageRequest;
import static software.wings.yaml.YamlHelper.trimYaml;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.query.UpdateOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Application;
import software.wings.beans.ConfigFile;
import software.wings.beans.Environment;
import software.wings.beans.ErrorCode;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.Pipeline;
import software.wings.beans.Service;
import software.wings.beans.ServiceTemplate;
import software.wings.beans.ServiceVariable;
import software.wings.beans.Setup.SetupStatus;
import software.wings.beans.container.KubernetesPayload;
import software.wings.beans.stats.CloneMetadata;
import software.wings.beans.yaml.Change.ChangeType;
import software.wings.common.Constants;
import software.wings.common.NotificationMessageResolver.NotificationMessageType;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.exception.WingsException.ReportTarget;
import software.wings.lock.AcquiredLock;
import software.wings.lock.PersistentLocker;
import software.wings.scheduler.PruneEntityJob;
import software.wings.scheduler.QuartzScheduler;
import software.wings.service.impl.yaml.YamlChangeSetHelper;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ConfigService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.NotificationService;
import software.wings.service.intfc.PipelineService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.ServiceTemplateService;
import software.wings.service.intfc.ServiceVariableService;
import software.wings.service.intfc.SetupService;
import software.wings.service.intfc.WorkflowService;
import software.wings.service.intfc.ownership.OwnedByEnvironment;
import software.wings.service.intfc.yaml.EntityUpdateService;
import software.wings.stencils.DataProvider;
import software.wings.utils.BoundedInputStream;
import software.wings.utils.Validator;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;
import javax.validation.executable.ValidateOnExecution;
/**
 * Created by anubhaw on 4/1/16.
 */
@ValidateOnExecution
@Singleton
public class EnvironmentServiceImpl implements EnvironmentService, DataProvider {
  private static final Logger logger = LoggerFactory.getLogger(EnvironmentServiceImpl.class);

  @Inject private WingsPersistence wingsPersistence;

  @Inject private ActivityService activityService;
  @Inject private AppService appService;
  @Inject private ConfigService configService;
  @Inject private EntityUpdateService entityUpdateService;
  @Inject private ExecutorService executorService;
  @Inject private InfrastructureMappingService infrastructureMappingService;
  @Inject private NotificationService notificationService;
  @Inject private PipelineService pipelineService;
  @Inject private ServiceResourceService serviceResourceService;
  @Inject private ServiceTemplateService serviceTemplateService;
  @Inject private ServiceVariableService serviceVariableService;
  @Inject private SetupService setupService;
  @Inject private WorkflowService workflowService;
  @Inject private YamlChangeSetHelper yamlChangeSetHelper;
  @Inject private PersistentLocker persistentLocker;

  @Inject @Named("JobScheduler") private QuartzScheduler jobScheduler;

  /**
   * {@inheritDoc}
   */
  @Override
  public PageResponse<Environment> list(PageRequest<Environment> request, boolean withSummary) {
    PageResponse<Environment> pageResponse = wingsPersistence.query(Environment.class, request);
    if (pageResponse == null || pageResponse.getResponse() == null) {
      return pageResponse;
    }
    if (withSummary) {
      pageResponse.getResponse().forEach(environment -> {
        try {
          addServiceTemplates(environment);
        } catch (Exception e) {
          logger.error("Failed to add service templates to environment {} ", environment, e);
        }
      });
    }
    return pageResponse;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Environment get(String appId, String envId, boolean withSummary) {
    Environment environment = wingsPersistence.get(Environment.class, appId, envId);
    if (environment == null) {
      throw new WingsException(INVALID_ARGUMENT).addParam("args", "Environment doesn't exist");
    }
    if (withSummary) {
      addServiceTemplates(environment);
    }
    return environment;
  }

  @Override
  public Environment get(@NotEmpty String appId, @NotEmpty String envId, @NotNull SetupStatus status) {
    Environment environment = get(appId, envId, true);
    if (status == SetupStatus.INCOMPLETE) {
      environment.setSetup(setupService.getEnvironmentSetupStatus(environment));
    }
    return environment;
  }

  @Override
  public Environment getEnvironmentByName(String appId, String environmentName) {
    Environment environment = wingsPersistence.createQuery(Environment.class)
                                  .field("appId")
                                  .equal(appId)
                                  .field("name")
                                  .equal(environmentName)
                                  .get();
    if (environment != null) {
      addServiceTemplates(environment);
    }
    return environment;
  }

  @Override
  public boolean exist(@NotEmpty String appId, @NotEmpty String envId) {
    return wingsPersistence.createQuery(Environment.class)
               .field("appId")
               .equal(appId)
               .field(ID_KEY)
               .equal(envId)
               .getKey()
        != null;
  }

  private void addServiceTemplates(Environment environment) {
    PageRequest<ServiceTemplate> pageRequest = new PageRequest<>();
    pageRequest.addFilter("appId", EQ, environment.getAppId());
    pageRequest.addFilter("envId", EQ, environment.getUuid());
    environment.setServiceTemplates(serviceTemplateService.list(pageRequest, false, false).getResponse());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Map<String, String> getData(String appId, String... params) {
    PageRequest<Environment> pageRequest = new PageRequest<>();
    pageRequest.addFilter("appId", EQ, appId);
    return list(pageRequest, false).stream().collect(toMap(Environment::getUuid, Environment::getName));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Environment save(Environment environment) {
    environment.setKeywords(
        trimList(asList(environment.getName(), environment.getDescription(), environment.getEnvironmentType())));
    Environment savedEnvironment = Validator.duplicateCheck(
        () -> wingsPersistence.saveAndGet(Environment.class, environment), "name", environment.getName());
    serviceTemplateService.createDefaultTemplatesByEnv(savedEnvironment);
    notificationService.sendNotificationAsync(
        anInformationNotification()
            .withAppId(savedEnvironment.getAppId())
            .withNotificationTemplateId(NotificationMessageType.ENTITY_CREATE_NOTIFICATION.name())
            .withNotificationTemplateVariables(
                ImmutableMap.of("ENTITY_TYPE", "Environment", "ENTITY_NAME", savedEnvironment.getName()))
            .build());

    yamlChangeSetHelper.environmentYamlChangeAsync(savedEnvironment, ChangeType.ADD);

    return savedEnvironment;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Environment update(Environment environment) {
    Environment savedEnvironment =
        wingsPersistence.get(Environment.class, environment.getAppId(), environment.getUuid());

    String description = Optional.ofNullable(environment.getDescription()).orElse("");
    List<String> keywords =
        trimList(asList(environment.getName(), environment.getDescription(), environment.getEnvironmentType()));
    ImmutableMap<String, Object> paramMap = ImmutableMap.of("name", environment.getName(), "environmentType",
        environment.getEnvironmentType(), "description", description, "keywords", keywords);

    wingsPersistence.updateFields(Environment.class, environment.getUuid(), paramMap);

    Environment updatedEnvironment =
        wingsPersistence.get(Environment.class, environment.getAppId(), environment.getUuid());

    yamlChangeSetHelper.environmentUpdateYamlChangeAsync(savedEnvironment, updatedEnvironment);

    return updatedEnvironment;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void delete(String appId, String envId) {
    Environment environment = wingsPersistence.get(Environment.class, appId, envId);
    if (environment == null) {
      return;
    }
    ensureEnvironmentSafeToDelete(environment);
    delete(environment);
  }

  @Override
  public List<Service> getServicesWithOverrides(String appId, String envId) {
    List<Service> services = new ArrayList<>();
    Environment environment = get(appId, envId, true);
    if (environment == null) {
      return services;
    }
    List<ServiceTemplate> serviceTemplates = environment.getServiceTemplates();
    if (isEmpty(serviceTemplates)) {
      return services;
    }
    List<String> serviceIds = new ArrayList<>();
    for (ServiceTemplate serviceTemplate : serviceTemplates) {
      boolean includeServiceId = false;
      // For each service template id check if it has service or config overrides
      List<ServiceVariable> serviceVariableOverrides = serviceVariableService.getServiceVariablesByTemplate(
          environment.getAppId(), environment.getUuid(), serviceTemplate, true);
      if (isNotEmpty(serviceVariableOverrides)) {
        // This service template has at least on service overrides
        includeServiceId = true;
      }
      if (!includeServiceId) {
        // For each service template id check if it has service or config overrides
        List<ConfigFile> overrideConfigFiles = configService.getConfigFileByTemplate(
            environment.getAppId(), environment.getUuid(), serviceTemplate.getUuid());
        if (isNotEmpty(overrideConfigFiles)) {
          // This service template has at least one service overrides
          includeServiceId = true;
        }
      }
      if (includeServiceId) {
        serviceIds.add(serviceTemplate.getServiceId());
      }
    }
    if (!serviceIds.isEmpty()) {
      PageRequest<Service> pageRequest = aPageRequest()
                                             .withLimit(PageRequest.UNLIMITED)
                                             .addFilter("appId", EQ, environment.getAppId())
                                             .addFilter("uuid", IN, serviceIds.toArray())
                                             .addFieldsExcluded("appContainer")
                                             .build();
      services = serviceResourceService.list(pageRequest, false, false);
    }
    return services;
  }

  private void ensureEnvironmentSafeToDelete(Environment environment) {
    List<Pipeline> pipelines = pipelineService.listPipelines(
        aPageRequest()
            .withLimit(PageRequest.UNLIMITED)
            .addFilter("appId", EQ, environment.getAppId())
            .addFilter("pipelineStages.pipelineStageElements.properties.envId", EQ, environment.getUuid())
            .build());

    if (!pipelines.isEmpty()) {
      List<String> pipelineNames = pipelines.stream().map(Pipeline::getName).collect(Collectors.toList());
      throw new WingsException(INVALID_REQUEST, ReportTarget.USER)
          .addParam("message",
              String.format("Environment is referenced by %s pipeline%s [%s].", pipelines.size(),
                  pipelines.size() == 1 ? "" : "s", Joiner.on(", ").join(pipelineNames)));
    }
  }

  private void delete(Environment environment) {
    // YAML is identified by name that can be reused after deletion. Pruning yaml eventual consistent
    // may result in deleting object from a new application created after the first one was deleted,
    // or preexisting being renamed to the vacated name. This is why we have to do this synchronously.
    yamlChangeSetHelper.environmentYamlChange(environment, ChangeType.DELETE);

    // First lets make sure that we have persisted a job that will prone the descendant objects
    PruneEntityJob.addDefaultJob(
        jobScheduler, Environment.class, environment.getAppId(), environment.getUuid(), Duration.ofSeconds(5));

    // Do not add too much between these too calls (on top and bottom). We need to persist the job
    // before we delete the object to avoid leaving the objects unpruned in case of crash. Waiting
    // too much though will result in start the job before the object is deleted, this possibility is
    // handled, but this is still not good.

    // Now we are ready to delete the object.
    if (wingsPersistence.delete(environment)) {
      notificationService.sendNotificationAsync(
          anInformationNotification()
              .withAppId(environment.getAppId())
              .withNotificationTemplateId(NotificationMessageType.ENTITY_DELETE_NOTIFICATION.name())
              .withNotificationTemplateVariables(
                  ImmutableMap.of("ENTITY_TYPE", "Environment", "ENTITY_NAME", environment.getName()))
              .build());
    }

    // Note that if we failed to delete the object we left without the yaml. Likely the users
    // will not reconsider and start using the object as they never intended to delete it, but
    // probably they will retry. This is why there is no reason for us to regenerate it at this
    // point. We should have the necessary APIs elsewhere, if we find the users want it.
  }

  @Override
  public void pruneDescendingEntities(@NotEmpty String appId, @NotEmpty String envId) {
    List<OwnedByEnvironment> services =
        ServiceClassLocator.descendingServices(this, EnvironmentServiceImpl.class, OwnedByEnvironment.class);
    PruneEntityJob.pruneDescendingEntities(services, descending -> descending.pruneByEnvironment(appId, envId));
  }

  @Override
  public void pruneByApplication(String appId) {
    List<Environment> environments =
        wingsPersistence.createQuery(Environment.class).field("appId").equal(appId).asList();
    environments.forEach(environment -> {
      wingsPersistence.delete(environment);
      pruneDescendingEntities(appId, environment.getUuid());
    });
  }

  @Override
  public void createDefaultEnvironments(String appId) {
    save(anEnvironment().withAppId(appId).withName(Constants.DEV_ENV).withEnvironmentType(NON_PROD).build());
    save(anEnvironment().withAppId(appId).withName(Constants.QA_ENV).withEnvironmentType(NON_PROD).build());
    save(anEnvironment().withAppId(appId).withName(Constants.PROD_ENV).withEnvironmentType(PROD).build());
  }

  @Override
  public List<Environment> getEnvByApp(String appId) {
    return wingsPersistence.createQuery(Environment.class).field("appId").equal(appId).asList();
  }

  @Override
  public Environment cloneEnvironment(String appId, String envId, CloneMetadata cloneMetadata) {
    Validator.notNullCheck("cloneMetadata", cloneMetadata);
    Validator.notNullCheck("environment", cloneMetadata.getEnvironment());
    if (cloneMetadata.getTargetAppId() == null) {
      logger.info("Cloning environment envId {}  within the same appId {}", envId, appId);
      String envName = cloneMetadata.getEnvironment().getName();
      String description = cloneMetadata.getEnvironment().getDescription();
      if (envId == null) {
        envId = cloneMetadata.getEnvironment().getUuid();
      }
      Environment sourceEnvironment = get(appId, envId, true);
      Environment clonedEnvironment = sourceEnvironment.clone();
      if (isEmpty(description)) {
        description = "Cloned from environment " + sourceEnvironment.getName();
      }
      clonedEnvironment.setName(envName);
      clonedEnvironment.setDescription(description);
      // Create environment
      clonedEnvironment = save(clonedEnvironment);

      // Copy templates
      List<ServiceTemplate> serviceTemplates = sourceEnvironment.getServiceTemplates();
      if (serviceTemplates != null) {
        for (ServiceTemplate serviceTemplate : serviceTemplates) {
          // Verify if the service template already exists in the target app
          PageRequest<ServiceTemplate> serviceTemplatePageRequest =
              aPageRequest()
                  .withLimit(PageRequest.UNLIMITED)
                  .addFilter("appId", EQ, appId)
                  .addFilter("envId", EQ, clonedEnvironment.getUuid())
                  .addFilter("serviceId", EQ, serviceTemplate.getServiceId())
                  .build();

          List<ServiceTemplate> serviceTemplateList =
              serviceTemplateService.list(serviceTemplatePageRequest, false, false);
          ServiceTemplate clonedServiceTemplate = null;
          if (isNotEmpty(serviceTemplateList)) {
            clonedServiceTemplate = serviceTemplateList.get(0);
          }
          if (clonedServiceTemplate == null) {
            clonedServiceTemplate = serviceTemplate.clone();
            clonedServiceTemplate.setEnvId(clonedEnvironment.getUuid());
            clonedServiceTemplate = serviceTemplateService.save(clonedServiceTemplate);
          }
          serviceTemplate =
              serviceTemplateService.get(appId, serviceTemplate.getEnvId(), serviceTemplate.getUuid(), true, true);
          if (serviceTemplate != null) {
            // Clone Infrastructure Mappings
            List<InfrastructureMapping> infrastructureMappings = serviceTemplate.getInfrastructureMappings();
            if (infrastructureMappings != null) {
              for (InfrastructureMapping infrastructureMapping : infrastructureMappings) {
                try {
                  infrastructureMapping.setUuid(null);
                  infrastructureMapping.setEnvId(clonedEnvironment.getUuid());
                  infrastructureMapping.setServiceTemplateId(clonedServiceTemplate.getUuid());
                  infrastructureMappingService.save(infrastructureMapping);
                } catch (Exception e) {
                  logger.error("Failed to clone infrastructure mapping name {}, id {} of environment {}",
                      infrastructureMapping.getName(), infrastructureMapping.getUuid(),
                      infrastructureMapping.getEnvId(), e);
                }
              }
            }
          }
          // Clone Service Config Files
          cloneServiceVariables(clonedEnvironment, serviceTemplate.getServiceVariablesOverrides(),
              clonedServiceTemplate.getUuid(), null, null);
          // Clone Service Config File overrides
          cloneConfigFiles(
              clonedEnvironment, clonedServiceTemplate, serviceTemplate.getConfigFilesOverrides(), null, null);
        }
      }
      // Clone ALL service variable overrides
      PageRequest<ServiceVariable> serviceVariablePageRequest = aPageRequest()
                                                                    .withLimit(PageRequest.UNLIMITED)
                                                                    .addFilter("appId", EQ, appId)
                                                                    .addFilter("entityId", EQ, envId)
                                                                    .build();
      List<ServiceVariable> serviceVariables = serviceVariableService.list(serviceVariablePageRequest, false);
      cloneServiceVariables(clonedEnvironment, serviceVariables, null, null, null);
      logger.info("Cloning environment envId {}  within the same appId {} success", envId, appId);
      return clonedEnvironment;
    } else {
      String targetAppId = cloneMetadata.getTargetAppId();
      logger.info("Cloning environment from appId {} to appId {}", appId, targetAppId);
      Validator.notNullCheck("targetAppId", targetAppId);
      Application sourceApplication = appService.get(appId);
      Validator.notNullCheck("appId", appId);

      Map<String, String> serviceMapping = cloneMetadata.getServiceMapping();
      Validator.notNullCheck("serviceMapping", serviceMapping);

      validateServiceMapping(appId, targetAppId, serviceMapping);

      String envName = cloneMetadata.getEnvironment().getName();
      String description = cloneMetadata.getEnvironment().getDescription();
      if (envId == null) {
        envId = cloneMetadata.getEnvironment().getUuid();
      }
      Environment sourceEnvironment = get(appId, envId, true);
      if (isEmpty(description)) {
        description =
            "Cloned from environment " + sourceEnvironment.getName() + " of application " + sourceApplication.getName();
      }

      Environment clonedEnvironment = sourceEnvironment.clone();
      clonedEnvironment.setName(envName);
      clonedEnvironment.setDescription(description);
      clonedEnvironment.setAppId(targetAppId);

      // Create environment
      clonedEnvironment = save(clonedEnvironment);

      // Copy templates
      List<ServiceTemplate> serviceTemplates = sourceEnvironment.getServiceTemplates();
      if (serviceTemplates != null) {
        for (ServiceTemplate serviceTemplate : serviceTemplates) {
          String serviceId = serviceTemplate.getServiceId();
          String targetServiceId = serviceMapping.get(serviceId);
          if (targetServiceId == null) {
            continue;
          }
          Service targetService = serviceResourceService.get(targetAppId, targetServiceId);
          Validator.notNullCheck("Target Service", targetService);

          String clonedEnvironmentUuid = clonedEnvironment.getUuid();

          // Verify if the service template already exists in the target app
          PageRequest<ServiceTemplate> serviceTemplatePageRequest = aPageRequest()
                                                                        .withLimit(PageRequest.UNLIMITED)
                                                                        .addFilter("appId", EQ, targetAppId)
                                                                        .addFilter("envId", EQ, clonedEnvironmentUuid)
                                                                        .addFilter("serviceId", EQ, targetServiceId)
                                                                        .build();

          List<ServiceTemplate> serviceTemplateList =
              serviceTemplateService.list(serviceTemplatePageRequest, false, false);
          ServiceTemplate clonedServiceTemplate = null;
          if (isNotEmpty(serviceTemplateList)) {
            clonedServiceTemplate = serviceTemplateList.get(0);
          }
          if (clonedServiceTemplate == null) {
            clonedServiceTemplate = serviceTemplate.clone();
            clonedServiceTemplate.setAppId(targetAppId);
            clonedServiceTemplate.setEnvId(clonedEnvironmentUuid);
            clonedServiceTemplate.setServiceId(targetServiceId);
            clonedServiceTemplate.setName(targetService.getName());
            // Check if the service template exist before cloning
            clonedServiceTemplate = serviceTemplateService.save(clonedServiceTemplate);
          }
          serviceTemplate =
              serviceTemplateService.get(appId, serviceTemplate.getEnvId(), serviceTemplate.getUuid(), true, true);
          if (serviceTemplate != null) {
            // Clone Service Variable overrides
            cloneServiceVariables(clonedEnvironment, serviceTemplate.getServiceVariablesOverrides(),
                clonedServiceTemplate.getUuid(), targetAppId, targetServiceId);
            // Clone Service Config File overrides
            cloneConfigFiles(clonedEnvironment, clonedServiceTemplate, serviceTemplate.getConfigFilesOverrides(),
                targetAppId, targetServiceId);
          }
        }
        // Clone ALL service variable overrides
        PageRequest<ServiceVariable> serviceVariablePageRequest = aPageRequest()
                                                                      .withLimit(PageRequest.UNLIMITED)
                                                                      .addFilter("appId", EQ, appId)
                                                                      .addFilter("entityId", EQ, envId)
                                                                      .build();
        List<ServiceVariable> serviceVariables = serviceVariableService.list(serviceVariablePageRequest, false);
        cloneServiceVariables(clonedEnvironment, serviceVariables, null, targetAppId, null);
        logger.info("Cloning environment from appId {} to appId {}", appId, targetAppId);
      }
      return clonedEnvironment;
    }
  }

  private void cloneServiceVariables(Environment clonedEnvironment, List<ServiceVariable> serviceVariables,
      String serviceTemplateId, String targetAppId, String targetServiceId) {
    if (serviceVariables != null) {
      for (ServiceVariable serviceVariable : serviceVariables) {
        ServiceVariable clonedServiceVariable = serviceVariable.clone();
        if (ENCRYPTED_TEXT.equals(clonedServiceVariable.getType())) {
          clonedServiceVariable.setValue(clonedServiceVariable.getEncryptedValue().toCharArray());
        }
        if (targetAppId != null) {
          clonedServiceVariable.setAppId(targetAppId);
        }
        if (!clonedServiceVariable.getEnvId().equals(GLOBAL_ENV_ID)) {
          clonedServiceVariable.setEnvId(clonedEnvironment.getUuid());
        }
        if (!clonedServiceVariable.getTemplateId().equals(DEFAULT_TEMPLATE_ID)) {
          if (serviceTemplateId != null) {
            clonedServiceVariable.setTemplateId(serviceTemplateId);
          }
        }
        if (clonedServiceVariable.getEntityType().equals(SERVICE_TEMPLATE)) {
          if (serviceTemplateId != null) {
            clonedServiceVariable.setEntityId(serviceTemplateId);
          }
        }
        if (clonedServiceVariable.getEntityType().equals(SERVICE)) {
          if (targetServiceId != null) {
            clonedServiceVariable.setEntityId(targetServiceId);
          }
        }
        if (clonedServiceVariable.getEntityType().equals(ENVIRONMENT)) {
          clonedServiceVariable.setEntityId(clonedEnvironment.getUuid());
        }
        serviceVariableService.save(clonedServiceVariable);
      }
    }
  }

  private void cloneConfigFiles(Environment clonedEnvironment, ServiceTemplate clonedServiceTemplate,
      List<ConfigFile> configFiles, String targetAppId, String targetServiceId) {
    if (configFiles != null) {
      for (ConfigFile configFile : configFiles) {
        ConfigFile clonedConfigFile = configFile.clone();
        if (targetAppId != null) {
          clonedConfigFile.setAppId(targetAppId);
        }
        if (!clonedConfigFile.getEnvId().equals(GLOBAL_ENV_ID)) {
          clonedConfigFile.setEnvId(clonedEnvironment.getUuid());
        }
        if (!clonedConfigFile.getTemplateId().equals(DEFAULT_TEMPLATE_ID)) {
          clonedConfigFile.setTemplateId(clonedServiceTemplate.getUuid());
        }
        if (clonedConfigFile.getEntityType().equals(SERVICE_TEMPLATE)) {
          clonedConfigFile.setEntityId(clonedServiceTemplate.getUuid());
        }
        if (clonedConfigFile.getEntityType().equals(SERVICE)) {
          if (targetServiceId != null) {
            clonedConfigFile.setEntityId(targetServiceId);
          }
        }
        try {
          File file = configService.download(configFile.getAppId(), configFile.getUuid());
          configService.save(clonedConfigFile, new BoundedInputStream(new FileInputStream(file)));
        } catch (FileNotFoundException e) {
          logger.error("Error in cloning config file " + configFile.toString(), e);
          // Ignore and continue adding more files
        }
      }
    }
  }

  /**
   * Validates whether service id and mapped service are of same type
   *
   * @param serviceMapping
   */
  private void validateServiceMapping(String appId, String targetAppId, Map<String, String> serviceMapping) {
    if (serviceMapping != null) {
      Set<String> serviceIds = serviceMapping.keySet();
      for (String serviceId : serviceIds) {
        String targetServiceId = serviceMapping.get(serviceId);
        if (serviceId != null && targetServiceId != null) {
          Service oldService = serviceResourceService.get(appId, serviceId, false);
          Validator.notNullCheck("service", oldService);
          Service newService = serviceResourceService.get(targetAppId, targetServiceId, false);
          Validator.notNullCheck("targetService", newService);
          if (oldService.getArtifactType() != null
              && !oldService.getArtifactType().equals(newService.getArtifactType())) {
            throw new WingsException(ErrorCode.INVALID_REQUEST)
                .addParam("message",
                    "Target service  [" + oldService.getName() + " ] is not compatible with service ["
                        + newService.getName() + "]");
          }
        }
      }
    }
  }

  @Override
  public Environment setConfigMapYaml(String appId, String envId, KubernetesPayload kubernetesPayload) {
    Environment savedEnv = get(appId, envId, false);
    Validator.notNullCheck("Environment", savedEnv);

    String configMapYaml = trimYaml(kubernetesPayload.getAdvancedConfig());
    UpdateOperations<Environment> updateOperations;
    if (isNotBlank(configMapYaml)) {
      updateOperations = wingsPersistence.createUpdateOperations(Environment.class).set("configMapYaml", configMapYaml);
    } else {
      updateOperations = wingsPersistence.createUpdateOperations(Environment.class).unset("configMapYaml");
    }

    wingsPersistence.update(savedEnv, updateOperations);

    return get(appId, envId, false);
  }

  @Override
  public Environment setConfigMapYamlForService(
      String appId, String envId, String serviceTemplateId, KubernetesPayload kubernetesPayload) {
    try (
        AcquiredLock lock = persistentLocker.waitToAcquireLock(Environment.class, envId, ofSeconds(5), ofSeconds(10))) {
      if (lock == null) {
        throw new WingsException(ErrorCode.GENERAL_ERROR).addParam("args", "The persistent lock was not acquired.");
      }
      Environment savedEnv = get(appId, envId, false);
      Validator.notNullCheck("Environment", savedEnv);

      String configMapYaml = trimYaml(kubernetesPayload.getAdvancedConfig());
      Map<String, String> configMapYamls =
          Optional.ofNullable(savedEnv.getConfigMapYamlByServiceTemplateId()).orElse(new HashMap<>());

      if (isNotBlank(configMapYaml)) {
        configMapYamls.put(serviceTemplateId, configMapYaml);
      } else {
        configMapYamls.remove(serviceTemplateId);
      }
      UpdateOperations<Environment> updateOperations;
      if (isNotEmpty(configMapYamls)) {
        updateOperations = wingsPersistence.createUpdateOperations(Environment.class)
                               .set("configMapYamlByServiceTemplateId", configMapYamls);
      } else {
        updateOperations =
            wingsPersistence.createUpdateOperations(Environment.class).unset("configMapYamlByServiceTemplateId");
      }

      wingsPersistence.update(savedEnv, updateOperations);
    }

    return get(appId, envId, false);
  }
}
