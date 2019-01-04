package software.wings.service.impl;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.beans.SearchFilter.Operator.EQ;
import static io.harness.beans.SearchFilter.Operator.IN;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.data.structure.ListUtils.trimList;
import static io.harness.eraro.ErrorCode.GENERAL_ERROR;
import static io.harness.eraro.ErrorCode.INVALID_ARGUMENT;
import static io.harness.exception.WingsException.USER;
import static java.lang.String.format;
import static java.time.Duration.ofSeconds;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.atteo.evo.inflector.English.plural;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.beans.Base.GLOBAL_ENV_ID;
import static software.wings.beans.EntityType.ENVIRONMENT;
import static software.wings.beans.EntityType.SERVICE;
import static software.wings.beans.EntityType.SERVICE_TEMPLATE;
import static software.wings.beans.Environment.APP_ID_KEY;
import static software.wings.beans.Environment.Builder.anEnvironment;
import static software.wings.beans.Environment.ENVIRONMENT_TYPE_KEY;
import static software.wings.beans.Environment.EnvironmentType.NON_PROD;
import static software.wings.beans.Environment.EnvironmentType.PROD;
import static software.wings.beans.Environment.NAME_KEY;
import static software.wings.beans.InformationNotification.Builder.anInformationNotification;
import static software.wings.beans.ServiceVariable.DEFAULT_TEMPLATE_ID;
import static software.wings.beans.ServiceVariable.Type.ENCRYPTED_TEXT;
import static software.wings.common.Constants.VALUES_YAML_KEY;
import static software.wings.service.intfc.ServiceVariableService.EncryptedFieldMode.MASKED;
import static software.wings.service.intfc.ServiceVariableService.EncryptedFieldMode.OBTAIN_VALUE;
import static software.wings.utils.Validator.notNullCheck;
import static software.wings.yaml.YamlHelper.trimYaml;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.lock.AcquiredLock;
import io.harness.lock.PersistentLocker;
import io.harness.scheduler.PersistentScheduler;
import io.harness.validation.Create;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.Key;
import org.mongodb.morphia.query.UpdateOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.vyarus.guice.validator.group.annotation.ValidationGroups;
import software.wings.beans.Application;
import software.wings.beans.ConfigFile;
import software.wings.beans.EnvSummary;
import software.wings.beans.Environment;
import software.wings.beans.Event.Type;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.Service;
import software.wings.beans.ServiceTemplate;
import software.wings.beans.ServiceVariable;
import software.wings.beans.Setup.SetupStatus;
import software.wings.beans.appmanifest.ApplicationManifest;
import software.wings.beans.appmanifest.ManifestFile;
import software.wings.beans.appmanifest.StoreType;
import software.wings.beans.container.KubernetesPayload;
import software.wings.beans.stats.CloneMetadata;
import software.wings.common.Constants;
import software.wings.common.NotificationMessageResolver.NotificationMessageType;
import software.wings.dl.WingsPersistence;
import software.wings.scheduler.PruneEntityJob;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ApplicationManifestService;
import software.wings.service.intfc.ConfigService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.NotificationService;
import software.wings.service.intfc.PipelineService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.ServiceTemplateService;
import software.wings.service.intfc.ServiceVariableService;
import software.wings.service.intfc.TriggerService;
import software.wings.service.intfc.UsageRestrictionsService;
import software.wings.service.intfc.WorkflowService;
import software.wings.service.intfc.instance.InstanceService;
import software.wings.service.intfc.ownership.OwnedByEnvironment;
import software.wings.service.intfc.yaml.YamlPushService;
import software.wings.stencils.DataProvider;
import software.wings.utils.BoundedInputStream;
import software.wings.utils.Validator;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Function;
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
  // DO NOT DELETE THIS, PRUNE logic needs it
  @Inject private InstanceService instanceService;
  @Inject private AppService appService;
  @Inject private ConfigService configService;
  @Inject private InfrastructureMappingService infrastructureMappingService;
  @Inject private NotificationService notificationService;
  @Inject private PipelineService pipelineService;
  @Inject private ServiceResourceService serviceResourceService;
  @Inject private ServiceTemplateService serviceTemplateService;
  @Inject private ServiceVariableService serviceVariableService;
  @Inject private PersistentLocker persistentLocker;
  @Inject private WorkflowService workflowService;
  @Inject private TriggerService triggerService;
  @Inject @Named("BackgroundJobScheduler") private PersistentScheduler jobScheduler;
  @Inject private YamlPushService yamlPushService;
  @Inject private UsageRestrictionsService usageRestrictionsService;
  @Inject private ApplicationManifestService applicationManifestService;

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
          logger.error(format("Failed to add service templates to environment %s", environment.toString()), e);
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
    Environment environment = wingsPersistence.getWithAppId(Environment.class, appId, envId);
    if (environment == null) {
      throw new WingsException(INVALID_ARGUMENT).addParam("args", "Environment doesn't exist");
    }
    if (withSummary) {
      addServiceTemplates(environment);
    }
    return environment;
  }

  @Override
  public Environment get(String appId, String envId) {
    return wingsPersistence.getWithAppId(Environment.class, appId, envId);
  }

  @Override
  public Environment get(@NotEmpty String appId, @NotEmpty String envId, @NotNull SetupStatus status) {
    return get(appId, envId, true);
  }

  @Override
  public Environment getEnvironmentByName(String appId, String environmentName) {
    return getEnvironmentByName(appId, environmentName, true);
  }

  @Override
  public Environment getEnvironmentByName(String appId, String environmentName, boolean withServiceTemplates) {
    Environment environment =
        wingsPersistence.createQuery(Environment.class).filter("appId", appId).filter("name", environmentName).get();
    if (environment != null) {
      if (withServiceTemplates) {
        addServiceTemplates(environment);
      }
    }
    return environment;
  }

  @Override
  public boolean exist(@NotEmpty String appId, @NotEmpty String envId) {
    return wingsPersistence.createQuery(Environment.class).filter("appId", appId).filter(ID_KEY, envId).getKey()
        != null;
  }

  private void addServiceTemplates(Environment environment) {
    PageRequest<ServiceTemplate> pageRequest = new PageRequest<>();
    pageRequest.addFilter("appId", EQ, environment.getAppId());
    pageRequest.addFilter("envId", EQ, environment.getUuid());
    environment.setServiceTemplates(serviceTemplateService.list(pageRequest, false, OBTAIN_VALUE).getResponse());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Map<String, String> getData(String appId, Map<String, String> params) {
    PageRequest<Environment> pageRequest = new PageRequest<>();
    pageRequest.addFilter("appId", EQ, appId);
    return list(pageRequest, false).stream().collect(toMap(Environment::getUuid, Environment::getName));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @ValidationGroups(Create.class)
  public Environment save(Environment environment) {
    environment.setKeywords(trimList(environment.generateKeywords()));
    Environment savedEnvironment = Validator.duplicateCheck(
        () -> wingsPersistence.saveAndGet(Environment.class, environment), "name", environment.getName());
    serviceTemplateService.createDefaultTemplatesByEnv(savedEnvironment);
    sendNotifaction(savedEnvironment, NotificationMessageType.ENTITY_CREATE_NOTIFICATION);

    String accountId = appService.getAccountIdByAppId(savedEnvironment.getAppId());
    yamlPushService.pushYamlChangeSet(
        accountId, null, savedEnvironment, Type.CREATE, environment.isSyncFromGit(), false);

    return savedEnvironment;
  }

  private void sendNotifaction(Environment savedEnvironment, NotificationMessageType entityCreateNotification) {
    notificationService.sendNotificationAsync(anInformationNotification()
                                                  .withAppId(savedEnvironment.getAppId())
                                                  .withNotificationTemplateId(entityCreateNotification.name())
                                                  .withNotificationTemplateVariables(ImmutableMap.of("ENTITY_TYPE",
                                                      "Environment", "ENTITY_NAME", savedEnvironment.getName()))
                                                  .build());
  }

  @Override
  public Environment update(Environment environment) {
    return update(environment, false);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Environment update(Environment environment, boolean fromYaml) {
    Environment savedEnvironment =
        wingsPersistence.getWithAppId(Environment.class, environment.getAppId(), environment.getUuid());

    List<String> keywords = trimList(environment.generateKeywords());

    UpdateOperations<Environment> updateOperations =
        wingsPersistence.createUpdateOperations(Environment.class)
            .set("name", environment.getName().trim())
            .set("environmentType", environment.getEnvironmentType())
            .set("description", Optional.ofNullable(environment.getDescription()).orElse(""))
            .set("keywords", keywords);

    if (fromYaml) {
      if (isNotBlank(environment.getConfigMapYaml())) {
        updateOperations.set("configMapYaml", environment.getConfigMapYaml());
      } else {
        updateOperations.unset("configMapYaml");
      }

      if (isNotEmpty(environment.getConfigMapYamlByServiceTemplateId())) {
        updateOperations.set("configMapYamlByServiceTemplateId", environment.getConfigMapYamlByServiceTemplateId());
      } else {
        updateOperations.unset("configMapYamlByServiceTemplateId");
      }

      if (isNotBlank(environment.getHelmValueYaml())) {
        updateOperations.set("helmValueYaml", environment.getHelmValueYaml());
      } else {
        updateOperations.unset("helmValueYaml");
      }

      if (isNotEmpty(environment.getHelmValueYamlByServiceTemplateId())) {
        updateOperations.set("helmValueYamlByServiceTemplateId", environment.getHelmValueYamlByServiceTemplateId());
      } else {
        updateOperations.unset("helmValueYamlByServiceTemplateId");
      }
    }

    wingsPersistence.update(savedEnvironment, updateOperations);

    Environment updatedEnvironment =
        wingsPersistence.getWithAppId(Environment.class, environment.getAppId(), environment.getUuid());

    String accountId = appService.getAccountIdByAppId(savedEnvironment.getAppId());
    boolean isRename = !savedEnvironment.getName().equals(updatedEnvironment.getName());

    yamlPushService.pushYamlChangeSet(
        accountId, savedEnvironment, updatedEnvironment, Type.UPDATE, environment.isSyncFromGit(), isRename);

    return updatedEnvironment;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void delete(String appId, String envId) {
    delete(appId, envId, false);
  }

  @Override
  public void delete(String appId, String envId, boolean syncFromGit) {
    Environment environment = wingsPersistence.getWithAppId(Environment.class, appId, envId);
    if (environment == null) {
      return;
    }
    environment.setSyncFromGit(syncFromGit);
    ensureEnvironmentSafeToDelete(environment);
    delete(environment);
  }

  @Override
  public List<EnvSummary> obtainEnvironmentSummaries(String appId, List<String> envIds) {
    if (isEmpty(envIds)) {
      return new ArrayList<>();
    }
    List<Environment> environments = wingsPersistence.createQuery(Environment.class)
                                         .project(NAME_KEY, true)
                                         .project(ENVIRONMENT_TYPE_KEY, true)
                                         .project(APP_ID_KEY, true)
                                         .filter(APP_ID_KEY, appId)
                                         .field(Environment.ID_KEY)
                                         .in(envIds)
                                         .asList();

    List<EnvSummary> envSummaries = environments.stream()
                                        .map(environment
                                            -> EnvSummary.builder()
                                                   .uuid(environment.getUuid())
                                                   .name(environment.getName())
                                                   .environmentType(environment.getEnvironmentType())
                                                   .build())
                                        .collect(Collectors.toList());
    List<EnvSummary> orderedEnvSummaries = new ArrayList<>();
    Map<String, EnvSummary> envSummaryMap =
        envSummaries.stream().collect(Collectors.toMap(EnvSummary::getUuid, Function.identity()));
    for (String envId : envIds) {
      if (envSummaryMap.containsKey(envId)) {
        orderedEnvSummaries.add(envSummaryMap.get(envId));
      }
    }
    return orderedEnvSummaries;
  }

  @Override
  public List<Service> getServicesWithOverrides(String appId, String envId) {
    List<Service> services = new ArrayList<>();
    Environment environment = get(appId, envId, true);
    List<ServiceTemplate> serviceTemplates = environment.getServiceTemplates();
    if (isEmpty(serviceTemplates)) {
      return services;
    }
    List<String> serviceIds = new ArrayList<>();
    for (ServiceTemplate serviceTemplate : serviceTemplates) {
      boolean includeServiceId = false;
      // For each service template id check if it has service or config overrides
      List<ServiceVariable> serviceVariableOverrides = serviceVariableService.getServiceVariablesByTemplate(
          environment.getAppId(), environment.getUuid(), serviceTemplate, MASKED);
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
    List<String> refPipelines =
        pipelineService.obtainPipelineNamesReferencedByEnvironment(environment.getAppId(), environment.getUuid());
    if (refPipelines != null && refPipelines.size() > 0) {
      throw new InvalidRequestException(
          format("Environment is referenced by %d %s [%s].", refPipelines.size(),
              plural("pipeline", refPipelines.size()), Joiner.on(", ").join(refPipelines)),
          USER);
    }

    List<String> refWorkflows =
        workflowService.obtainWorkflowNamesReferencedByEnvironment(environment.getAppId(), environment.getUuid());
    if (refWorkflows != null && refWorkflows.size() > 0) {
      throw new InvalidRequestException(
          format("Environment is referenced by %d %s [%s].", refWorkflows.size(),
              plural("workflow", refWorkflows.size()), Joiner.on(", ").join(refWorkflows)),
          USER);
    }

    List<String> refTriggers =
        triggerService.obtainTriggerNamesReferencedByTemplatedEntityId(environment.getAppId(), environment.getUuid());
    if (refTriggers != null && refTriggers.size() > 0) {
      throw new InvalidRequestException(format("Environment is referenced by %d %s [%s].", refTriggers.size(),
                                            plural("trigger", refTriggers.size()), Joiner.on(", ").join(refTriggers)),
          USER);
    }
  }

  private void delete(Environment environment) {
    // YAML is identified by name that can be reused after deletion. Pruning yaml eventual consistent
    // may result in deleting object from a new application created after the first one was deleted,
    // or preexisting being renamed to the vacated name. This is why we have to do this synchronously.

    String accountId = appService.getAccountIdByAppId(environment.getAppId());
    yamlPushService.pushYamlChangeSet(accountId, environment, null, Type.DELETE, environment.isSyncFromGit(), false);

    // First lets make sure that we have persisted a job that will prone the descendant objects
    PruneEntityJob.addDefaultJob(
        jobScheduler, Environment.class, environment.getAppId(), environment.getUuid(), ofSeconds(5), ofSeconds(15));

    // Do not add too much between these too calls (on top and bottom). We need to persist the job
    // before we delete the object to avoid leaving the objects unpruned in case of crash. Waiting
    // too much though will result in start the job before the object is deleted, this possibility is
    // handled, but this is still not good.

    // HAR-6245: Need to first remove all the references to this environment from existing usage restrictions
    usageRestrictionsService.removeAppEnvReferences(accountId, environment.getAppId(), environment.getUuid());

    // Now we are ready to delete the object.
    if (wingsPersistence.delete(environment)) {
      sendNotifaction(environment, NotificationMessageType.ENTITY_DELETE_NOTIFICATION);
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
    List<Environment> environments = wingsPersistence.createQuery(Environment.class).filter("appId", appId).asList();
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
    PageRequest<Environment> pageRequest = aPageRequest().addFilter("appId", EQ, appId).build();
    return wingsPersistence.query(Environment.class, pageRequest).getResponse();
  }

  @Override
  public List<String> getEnvIdsByApp(String appId) {
    List<Key<Environment>> environmentKeyList =
        wingsPersistence.createQuery(Environment.class).filter("appId", appId).asKeyList();
    logger.info(environmentKeyList.size() + " environments found for appId " + appId);
    return environmentKeyList.stream().map(key -> (String) key.getId()).collect(Collectors.toList());
  }

  @Override
  public Environment cloneEnvironment(String appId, String envId, CloneMetadata cloneMetadata) {
    notNullCheck("cloneMetadata", cloneMetadata, USER);
    notNullCheck("environment", cloneMetadata.getEnvironment(), USER);
    if (cloneMetadata.getTargetAppId() == null) {
      envId = (envId == null) ? cloneMetadata.getEnvironment().getUuid() : envId;
      Environment sourceEnvironment = get(appId, envId, true);
      Environment clonedEnvironment = sourceEnvironment.cloneInternal();
      String description = cloneMetadata.getEnvironment().getDescription();
      if (isEmpty(description)) {
        description = "Cloned from environment " + sourceEnvironment.getName();
      }
      clonedEnvironment.setName(cloneMetadata.getEnvironment().getName());
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
              serviceTemplateService.list(serviceTemplatePageRequest, false, OBTAIN_VALUE);
          ServiceTemplate clonedServiceTemplate = null;
          if (isNotEmpty(serviceTemplateList)) {
            clonedServiceTemplate = serviceTemplateList.get(0);
          }
          if (clonedServiceTemplate == null) {
            clonedServiceTemplate = serviceTemplate.cloneInternal();
            clonedServiceTemplate.setEnvId(clonedEnvironment.getUuid());
            clonedServiceTemplate = serviceTemplateService.save(clonedServiceTemplate);
          }
          serviceTemplate =
              serviceTemplateService.get(appId, serviceTemplate.getEnvId(), serviceTemplate.getUuid(), true, MASKED);
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
          if (serviceTemplate != null) {
            // Clone Service Config Files
            cloneServiceVariables(clonedEnvironment, serviceTemplate.getServiceVariablesOverrides(),
                clonedServiceTemplate.getUuid(), null, null);
            // Clone Service Config File overrides
            cloneConfigFiles(
                clonedEnvironment, clonedServiceTemplate, serviceTemplate.getConfigFilesOverrides(), null, null);
          }
        }
      }
      // Clone ALL service variable overrides
      PageRequest<ServiceVariable> serviceVariablePageRequest = aPageRequest()
                                                                    .withLimit(PageRequest.UNLIMITED)
                                                                    .addFilter("appId", EQ, appId)
                                                                    .addFilter("entityId", EQ, envId)
                                                                    .build();
      List<ServiceVariable> serviceVariables = serviceVariableService.list(serviceVariablePageRequest, OBTAIN_VALUE);
      cloneServiceVariables(clonedEnvironment, serviceVariables, null, null, null);
      cloneAppManifests(clonedEnvironment.getAppId(), clonedEnvironment.getUuid(), envId);
      return clonedEnvironment;
    } else {
      String targetAppId = cloneMetadata.getTargetAppId();
      notNullCheck("targetAppId", targetAppId, USER);
      notNullCheck("appId", appId, USER);
      Application sourceApplication = appService.get(appId);
      Map<String, String> serviceMapping = cloneMetadata.getServiceMapping();
      notNullCheck("serviceMapping", serviceMapping, USER);

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

      Environment clonedEnvironment = sourceEnvironment.cloneInternal();
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
          notNullCheck("Target Service", targetService, USER);

          String clonedEnvironmentUuid = clonedEnvironment.getUuid();

          // Verify if the service template already exists in the target app
          PageRequest<ServiceTemplate> serviceTemplatePageRequest = aPageRequest()
                                                                        .withLimit(PageRequest.UNLIMITED)
                                                                        .addFilter("appId", EQ, targetAppId)
                                                                        .addFilter("envId", EQ, clonedEnvironmentUuid)
                                                                        .addFilter("serviceId", EQ, targetServiceId)
                                                                        .build();

          List<ServiceTemplate> serviceTemplateList =
              serviceTemplateService.list(serviceTemplatePageRequest, false, OBTAIN_VALUE);
          ServiceTemplate clonedServiceTemplate = null;
          if (isNotEmpty(serviceTemplateList)) {
            clonedServiceTemplate = serviceTemplateList.get(0);
          }
          if (clonedServiceTemplate == null) {
            clonedServiceTemplate = serviceTemplate.cloneInternal();
            clonedServiceTemplate.setAppId(targetAppId);
            clonedServiceTemplate.setEnvId(clonedEnvironmentUuid);
            clonedServiceTemplate.setServiceId(targetServiceId);
            clonedServiceTemplate.setName(targetService.getName());
            // Check if the service template exist before cloning
            clonedServiceTemplate = serviceTemplateService.save(clonedServiceTemplate);
          }
          serviceTemplate =
              serviceTemplateService.get(appId, serviceTemplate.getEnvId(), serviceTemplate.getUuid(), true, MASKED);
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
        List<ServiceVariable> serviceVariables = serviceVariableService.list(serviceVariablePageRequest, OBTAIN_VALUE);
        cloneServiceVariables(clonedEnvironment, serviceVariables, null, targetAppId, null);
        // ToDo anshul why do we have same thing in two places
        cloneAppManifests(clonedEnvironment.getAppId(), clonedEnvironment.getUuid(), envId);
        logger.info("Cloning environment from appId {} to appId {}", appId, targetAppId);
      }
      return clonedEnvironment;
    }
  }

  private void cloneServiceVariables(Environment clonedEnvironment, List<ServiceVariable> serviceVariables,
      String serviceTemplateId, String targetAppId, String targetServiceId) {
    if (serviceVariables != null) {
      for (ServiceVariable serviceVariable : serviceVariables) {
        ServiceVariable clonedServiceVariable = serviceVariable.cloneInternal();
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
        ConfigFile clonedConfigFile = configFile.cloneInternal();
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

  private void cloneAppManifests(String appId, String clonedEnvId, String originalEnvId) {
    List<ApplicationManifest> applicationManifests = applicationManifestService.getAllByEnvId(appId, originalEnvId);

    if (isEmpty(applicationManifests)) {
      return;
    }

    for (ApplicationManifest applicationManifest : applicationManifests) {
      ApplicationManifest applicationManifestNew = applicationManifest.cloneInternal();
      applicationManifestNew.setEnvId(clonedEnvId);

      ApplicationManifest createdAppManifest = applicationManifestService.create(applicationManifestNew);

      List<ManifestFile> manifestFiles =
          applicationManifestService.getManifestFilesByAppManifestId(appId, applicationManifest.getUuid());

      if (isEmpty(manifestFiles)) {
        return;
      }

      for (ManifestFile manifestFile : manifestFiles) {
        ManifestFile manifestFileNew = manifestFile.cloneInternal();
        applicationManifestService.upsertApplicationManifestFile(manifestFileNew, createdAppManifest, true);
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
      for (Entry<String, String> entry : serviceMapping.entrySet()) {
        String serviceId = entry.getKey();
        String targetServiceId = entry.getValue();
        if (serviceId != null && targetServiceId != null) {
          Service oldService = serviceResourceService.get(appId, serviceId, false);
          notNullCheck("service", oldService, USER);
          Service newService = serviceResourceService.get(targetAppId, targetServiceId, false);
          notNullCheck("targetService", newService, USER);
          if (oldService.getArtifactType() != null
              && !oldService.getArtifactType().equals(newService.getArtifactType())) {
            throw new InvalidRequestException("Target service  [" + oldService.getName()
                    + " ] is not compatible with service [" + newService.getName() + "]",
                USER);
          }
        }
      }
    }
  }

  @Override
  public Environment setConfigMapYaml(String appId, String envId, KubernetesPayload kubernetesPayload) {
    Environment savedEnv = get(appId, envId, false);
    notNullCheck("Environment", savedEnv, USER);

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
        throw new WingsException(GENERAL_ERROR, USER).addParam("message", "The persistent lock was not acquired.");
      }
      Environment savedEnv = get(appId, envId, false);
      notNullCheck("Environment", savedEnv, USER);

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

  @Override
  public Environment setHelmValueYaml(String appId, String envId, KubernetesPayload kubernetesPayload) {
    Environment savedEnv = get(appId, envId, false);
    notNullCheck("Environment", savedEnv, USER);

    String helmValueYaml = trimYaml(kubernetesPayload.getAdvancedConfig());
    UpdateOperations<Environment> updateOperations;
    if (isNotBlank(helmValueYaml)) {
      updateOperations = wingsPersistence.createUpdateOperations(Environment.class).set("helmValueYaml", helmValueYaml);
    } else {
      updateOperations = wingsPersistence.createUpdateOperations(Environment.class).unset("helmValueYaml");
    }

    wingsPersistence.update(savedEnv, updateOperations);

    return get(appId, envId, false);
  }

  @Override
  public Environment setHelmValueYamlForService(
      String appId, String envId, String serviceTemplateId, KubernetesPayload kubernetesPayload) {
    try (
        AcquiredLock lock = persistentLocker.waitToAcquireLock(Environment.class, envId, ofSeconds(5), ofSeconds(10))) {
      if (lock == null) {
        throw new WingsException(GENERAL_ERROR, USER).addParam("message", "The persistent lock was not acquired.");
      }
      Environment savedEnv = get(appId, envId, false);
      notNullCheck("Environment", savedEnv, USER);

      String helmValueYaml = trimYaml(kubernetesPayload.getAdvancedConfig());
      Map<String, String> helmValueYamls =
          Optional.ofNullable(savedEnv.getHelmValueYamlByServiceTemplateId()).orElse(new HashMap<>());

      if (isNotBlank(helmValueYaml)) {
        helmValueYamls.put(serviceTemplateId, helmValueYaml);
      } else {
        helmValueYamls.remove(serviceTemplateId);
      }
      UpdateOperations<Environment> updateOperations;
      if (isNotEmpty(helmValueYamls)) {
        updateOperations = wingsPersistence.createUpdateOperations(Environment.class)
                               .set("helmValueYamlByServiceTemplateId", helmValueYamls);
      } else {
        updateOperations =
            wingsPersistence.createUpdateOperations(Environment.class).unset("helmValueYamlByServiceTemplateId");
      }

      wingsPersistence.update(savedEnv, updateOperations);
    }

    return get(appId, envId, false);
  }

  @Override
  public ManifestFile createValues(String appId, String envId, String serviceId, ManifestFile manifestFile) {
    validateEnvAndServiceExists(appId, envId, serviceId);

    ApplicationManifest applicationManifest = applicationManifestService.getByEnvAndServiceId(appId, envId, serviceId);
    if (applicationManifest == null) {
      applicationManifest =
          ApplicationManifest.builder().storeType(StoreType.Local).envId(envId).serviceId(serviceId).build();
      applicationManifest.setAppId(appId);
      applicationManifest = applicationManifestService.create(applicationManifest);
    }

    manifestFile.setAppId(appId);
    manifestFile.setFileName(VALUES_YAML_KEY);
    manifestFile.setApplicationManifestId(applicationManifest.getUuid());
    manifestFile = applicationManifestService.upsertApplicationManifestFile(manifestFile, applicationManifest, true);

    return manifestFile;
  }

  @Override
  public ManifestFile updateValues(String appId, String envId, String serviceId, ManifestFile manifestFile) {
    validateEnvAndServiceExists(appId, envId, serviceId);

    ApplicationManifest appManifest = getAppManifest(appId, envId, serviceId);
    if (appManifest == null) {
      throw new InvalidRequestException(
          format("Application manifest doesn't exist for environment: %s and service: %s", envId, serviceId));
    }

    manifestFile.setAppId(appId);
    manifestFile.setFileName(VALUES_YAML_KEY);
    manifestFile.setApplicationManifestId(appManifest.getUuid());
    manifestFile = applicationManifestService.upsertApplicationManifestFile(manifestFile, appManifest, false);

    return manifestFile;
  }

  private void validateEnvAndServiceExists(String appId, String envId, String serviceId) {
    get(appId, envId, false);

    if (isNotBlank(serviceId)) {
      Service service = serviceResourceService.get(appId, serviceId, false);
      notNullCheck("Service", service, USER);
    }
  }

  private ApplicationManifest getAppManifest(String appId, String envId, String serviceId) {
    if (isNotBlank(envId) && isNotBlank(serviceId)) {
      return applicationManifestService.getByEnvAndServiceId(appId, envId, serviceId);
    } else if (isBlank(serviceId)) {
      return applicationManifestService.getByEnvId(appId, envId);
    } else {
      throw new InvalidRequestException(
          format("No valid application manifest exists for environment: %s and service: %s", envId, serviceId));
    }
  }
}
