package software.wings.service.impl;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.lang3.StringUtils.equalsIgnoreCase;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.beans.ConfigFile.DEFAULT_TEMPLATE_ID;
import static software.wings.beans.EntityVersion.Builder.anEntityVersion;
import static software.wings.beans.ErrorCode.INVALID_REQUEST;
import static software.wings.beans.InformationNotification.Builder.anInformationNotification;
import static software.wings.beans.SearchFilter.Operator.EQ;
import static software.wings.beans.ServiceVariable.Type.ENCRYPTED_TEXT;
import static software.wings.beans.Setup.SetupStatus.INCOMPLETE;
import static software.wings.beans.command.Command.Builder.aCommand;
import static software.wings.beans.command.CommandUnitType.COMMAND;
import static software.wings.beans.command.ServiceCommand.Builder.aServiceCommand;
import static software.wings.dl.MongoHelper.setUnset;
import static software.wings.dl.PageRequest.Builder.aPageRequest;
import static software.wings.dl.PageRequest.UNLIMITED;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import de.danielbechler.diff.ObjectDifferBuilder;
import de.danielbechler.diff.node.DiffNode;
import de.danielbechler.diff.path.NodePath;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.query.UpdateOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.AppContainer;
import software.wings.beans.CanaryOrchestrationWorkflow;
import software.wings.beans.ConfigFile;
import software.wings.beans.EntityType;
import software.wings.beans.EntityVersion;
import software.wings.beans.ErrorCode;
import software.wings.beans.Graph;
import software.wings.beans.LambdaSpecification;
import software.wings.beans.LambdaSpecification.FunctionSpecification;
import software.wings.beans.OrchestrationWorkflow;
import software.wings.beans.PhaseStep;
import software.wings.beans.SearchFilter;
import software.wings.beans.Service;
import software.wings.beans.ServiceTemplate;
import software.wings.beans.ServiceVariable;
import software.wings.beans.Setup.SetupStatus;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowPhase;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.command.Command;
import software.wings.beans.command.CommandUnit;
import software.wings.beans.command.CommandUnitType;
import software.wings.beans.command.ServiceCommand;
import software.wings.beans.container.ContainerAdvancedPayload;
import software.wings.beans.container.ContainerTask;
import software.wings.beans.container.ContainerTaskType;
import software.wings.beans.container.UserDataSpecification;
import software.wings.beans.yaml.Change.ChangeType;
import software.wings.beans.yaml.GitFileChange;
import software.wings.common.NotificationMessageResolver.NotificationMessageType;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.scheduler.PruneEntityJob;
import software.wings.scheduler.QuartzScheduler;
import software.wings.service.impl.yaml.YamlChangeSetHelper;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.CommandService;
import software.wings.service.intfc.ConfigService;
import software.wings.service.intfc.EntityVersionService;
import software.wings.service.intfc.NotificationService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.ServiceTemplateService;
import software.wings.service.intfc.ServiceVariableService;
import software.wings.service.intfc.SetupService;
import software.wings.service.intfc.TriggerService;
import software.wings.service.intfc.WorkflowService;
import software.wings.service.intfc.ownership.OwnedByService;
import software.wings.service.intfc.yaml.EntityUpdateService;
import software.wings.service.intfc.yaml.YamlChangeSetService;
import software.wings.service.intfc.yaml.YamlDirectoryService;
import software.wings.stencils.DataProvider;
import software.wings.stencils.Stencil;
import software.wings.stencils.StencilPostProcessor;
import software.wings.utils.ArtifactType;
import software.wings.utils.BoundedInputStream;
import software.wings.utils.Validator;
import software.wings.yaml.gitSync.YamlGitConfig;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.validation.executable.ValidateOnExecution;

/**
 * Created by anubhaw on 3/25/16.
 */
@ValidateOnExecution
@Singleton
public class ServiceResourceServiceImpl implements ServiceResourceService, DataProvider {
  private static final Logger logger = LoggerFactory.getLogger(ServiceResourceServiceImpl.class);

  @Inject private WingsPersistence wingsPersistence;

  @Inject private ActivityService activityService;
  @Inject private AppService appService;
  @Inject private ArtifactStreamService artifactStreamService;
  @Inject private CommandService commandService;
  @Inject private ConfigService configService;
  @Inject private EntityUpdateService entityUpdateService;
  @Inject private EntityVersionService entityVersionService;
  @Inject private ExecutorService executorService;
  @Inject private NotificationService notificationService;
  @Inject private ServiceTemplateService serviceTemplateService;
  @Inject private ServiceVariableService serviceVariableService;
  @Inject private SetupService setupService;
  @Inject private TriggerService triggerService;
  @Inject private WorkflowService workflowService;
  @Inject private YamlChangeSetService yamlChangeSetService;
  @Inject private YamlDirectoryService yamlDirectoryService;

  @Inject private YamlChangeSetHelper yamlChangeSetHelper;

  @Inject private StencilPostProcessor stencilPostProcessor;

  @Inject @Named("JobScheduler") private QuartzScheduler jobScheduler;

  /**
   * {@inheritDoc}
   */
  @Override
  public PageResponse<Service> list(
      PageRequest<Service> request, boolean withBuildSource, boolean withServiceCommands) {
    PageResponse<Service> pageResponse = wingsPersistence.query(Service.class, request);

    SearchFilter appIdSearchFilter = request.getFilters()
                                         .stream()
                                         .filter(searchFilter -> searchFilter.getFieldName().equals("appId"))
                                         .findFirst()
                                         .orElse(null);
    if (withServiceCommands) {
      if (appIdSearchFilter != null) {
        PageRequest<ServiceCommand> serviceCommandPageRequest =
            aPageRequest().withLimit(UNLIMITED).addFilter(appIdSearchFilter).build();
        List<ServiceCommand> appServiceCommands =
            wingsPersistence.query(ServiceCommand.class, serviceCommandPageRequest).getResponse();
        Map<String, List<ServiceCommand>> serviceToServiceCommandMap =
            appServiceCommands.stream().collect(Collectors.groupingBy(ServiceCommand::getServiceId));
        pageResponse.getResponse().forEach(service -> {
          try {
            //            service.setServiceCommands(getServiceCommands(service.getAppId(), service.getUuid()));
            if (serviceToServiceCommandMap != null) {
              List<ServiceCommand> serviceCommands = serviceToServiceCommandMap.get(service.getUuid());
              if (serviceCommands != null) {
                serviceCommands.forEach(serviceCommand
                    -> serviceCommand.setCommand(commandService.getCommand(
                        serviceCommand.getAppId(), serviceCommand.getUuid(), serviceCommand.getDefaultVersion())));
                service.setServiceCommands(serviceCommands);
              } else {
                service.setServiceCommands(getServiceCommands(service.getAppId(), service.getUuid()));
              }
            }
          } catch (Exception e) {
            logger.error("Failed to retrieve service commands for serviceId {}  of appId  {}", service.getUuid(),
                service.getAppId(), e);
          }
        });
      } else {
        throw new WingsException("AppId field is required in Search Filter");
      }
    }
    if (withBuildSource && appIdSearchFilter != null) {
      List<ArtifactStream> artifactStreams = new ArrayList<>();
      try {
        artifactStreams =
            artifactStreamService.list(aPageRequest().addFilter(appIdSearchFilter).withLimit(UNLIMITED).build())
                .getResponse();
      } catch (Exception e) {
        logger.error("Failed to retrieve artifact streams", e);
      }
      Map<String, List<ArtifactStream>> serviceToBuildSourceMap =
          artifactStreams.stream().collect(Collectors.groupingBy(ArtifactStream::getServiceId));
      if (serviceToBuildSourceMap != null) {
        pageResponse.getResponse().forEach(service
            -> service.setArtifactStreams(serviceToBuildSourceMap.getOrDefault(service.getUuid(), emptyList())));
      }
    }
    return pageResponse;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Service save(Service service) {
    return save(service, false);
  }

  @Override
  public Service save(Service service, boolean serviceCreatedFromYaml) {
    Service savedService =
        Validator.duplicateCheck(() -> wingsPersistence.saveAndGet(Service.class, service), "name", service.getName());
    savedService = addDefaultCommands(savedService, serviceCreatedFromYaml);
    serviceTemplateService.createDefaultTemplatesByService(savedService);
    notificationService.sendNotificationAsync(
        anInformationNotification()
            .withAppId(savedService.getAppId())
            .withNotificationTemplateId(NotificationMessageType.ENTITY_CREATE_NOTIFICATION.name())
            .withNotificationTemplateVariables(
                ImmutableMap.of("ENTITY_TYPE", "Service", "ENTITY_NAME", savedService.getName()))
            .build());

    if (!serviceCreatedFromYaml) {
      yamlChangeSetHelper.serviceYamlChangeAsync(savedService, ChangeType.ADD);
    }

    return savedService;
  }

  @Override
  public Service clone(String appId, String originalServiceId, Service service) {
    Service originalService = get(appId, originalServiceId);
    Service clonedService = originalService.clone();
    clonedService.setName(service.getName());
    clonedService.setDescription(service.getDescription());

    Service savedCloneService = Validator.duplicateCheck(
        () -> wingsPersistence.saveAndGet(Service.class, clonedService), "name", service.getName());

    originalService.getServiceCommands().forEach(serviceCommand -> {
      ServiceCommand clonedServiceCommand = serviceCommand.clone();
      addCommand(savedCloneService.getAppId(), savedCloneService.getUuid(), clonedServiceCommand, true, false);
    });

    List<ServiceTemplate> serviceTemplates =
        serviceTemplateService
            .list(aPageRequest()
                      .addFilter(ServiceTemplate.APP_ID_KEY, EQ, originalService.getAppId())
                      .addFilter(ServiceTemplate.SERVICE_ID_KEY, EQ, originalService.getUuid())
                      .build(),
                false, false)
            .getResponse();

    serviceTemplates.forEach(serviceTemplate -> {
      ServiceTemplate clonedServiceTemplate = serviceTemplate.clone();
      clonedServiceTemplate.setName(savedCloneService.getName());
      clonedServiceTemplate.setServiceId(savedCloneService.getUuid());
      serviceTemplateService.save(clonedServiceTemplate);
    });

    originalService.getConfigFiles().forEach(originalConfigFile -> {
      try {
        File file = configService.download(originalConfigFile.getAppId(), originalConfigFile.getUuid());
        ConfigFile clonedConfigFile = originalConfigFile.clone();
        clonedConfigFile.setEntityId(savedCloneService.getUuid());
        configService.save(clonedConfigFile, new BoundedInputStream(new FileInputStream(file)));
      } catch (FileNotFoundException e) {
        logger.error("Error in cloning config file " + originalConfigFile.toString(), e);
        // Ignore and continue adding more files
      }
    });

    originalService.getServiceVariables().forEach(originalServiceVariable -> {
      ServiceVariable clonedServiceVariable = originalServiceVariable.clone();
      if (ENCRYPTED_TEXT.equals(clonedServiceVariable.getType())) {
        clonedServiceVariable.setValue(clonedServiceVariable.getEncryptedValue().toCharArray());
      }
      clonedServiceVariable.setEntityId(savedCloneService.getUuid());
      serviceVariableService.save(clonedServiceVariable);
    });
    return savedCloneService;
  }

  @Override
  public Service cloneCommand(String appId, String serviceId, String commandName, ServiceCommand command) {
    // don't allow cloning of Docker commands
    Service service = getServiceWithServiceCommands(appId, serviceId);
    if (service.getArtifactType().equals(ArtifactType.DOCKER)) {
      throw new WingsException(INVALID_REQUEST).addParam("message", "Docker commands can not be cloned");
    }
    ServiceCommand oldServiceCommand = service.getServiceCommands()
                                           .stream()
                                           .filter(cmd -> equalsIgnoreCase(commandName, cmd.getName()))
                                           .findFirst()
                                           .orElse(null);
    ServiceCommand clonedServiceCommand = oldServiceCommand.clone();
    clonedServiceCommand.getCommand().getGraph().setGraphName(command.getName());
    return addCommand(appId, serviceId, clonedServiceCommand, true, false);
  }

  @Override
  public List<CommandUnit> getFlattenCommandUnitList(String appId, String serviceId, String envId, String commandName) {
    Map<String, Integer> commandNameVersionMap =
        getServiceCommands(appId, serviceId, false)
            .stream()
            .filter(serviceCommand -> serviceCommand.getVersionForEnv(envId) != 0)
            .collect(toMap(ServiceCommand::getName, serviceCommand -> serviceCommand.getVersionForEnv(envId)));

    return getFlattenCommandUnitList(appId, serviceId, commandName, commandNameVersionMap);
  }

  private List<CommandUnit> getFlattenCommandUnitList(
      String appId, String serviceId, String commandName, Map<String, Integer> commandNameVersionMap) {
    int version = EntityVersion.INITIAL_VERSION;
    if (commandNameVersionMap != null) {
      version = commandNameVersionMap.get(commandName);
    }

    Command command = getCommandByNameAndVersion(appId, serviceId, commandName, version).getCommand();

    return command.getCommandUnits()
        .stream()
        .flatMap(commandUnit -> {
          if (COMMAND.equals(commandUnit.getCommandUnitType())) {
            String commandUnitName = isNotBlank(((Command) commandUnit).getReferenceId())
                ? ((Command) commandUnit).getReferenceId()
                : commandUnit.getName();
            return getFlattenCommandUnitList(appId, serviceId, commandUnitName, commandNameVersionMap).stream();
          } else {
            return Stream.of(commandUnit);
          }
        })
        .collect(toList());
  }

  private Service addDefaultCommands(Service service, boolean serviceCreatedFromYaml) {
    boolean pushToYaml = true;
    List<Command> commands = emptyList();
    ArtifactType artifactType = service.getArtifactType();
    AppContainer appContainer = service.getAppContainer();
    if (appContainer != null && appContainer.getFamily() != null) {
      commands = appContainer.getFamily().getDefaultCommands(artifactType, appContainer);
      pushToYaml = appContainer.getFamily().shouldPushCommandsToYaml();
    } else if (artifactType != null) {
      commands = artifactType.getDefaultCommands();
      pushToYaml = artifactType.shouldPushCommandsToYaml();
    }

    // This makes sure we only push commands to git when the service is not created from yaml and if the artifact type
    // has configurable / exposed commands. For services like docker, the commands are internal. For War, user could
    // configure it.
    boolean shouldPushCommandsToYaml = pushToYaml && !serviceCreatedFromYaml;

    Service serviceToReturn = service;
    for (Command command : commands) {
      serviceToReturn = addCommand(service.getAppId(), service.getUuid(),
          aServiceCommand().withTargetToAllEnv(true).withCommand(command).build(), true, shouldPushCommandsToYaml);
    }

    return serviceToReturn;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Service update(Service service) {
    return update(service, false);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Service update(Service service, boolean fromYaml) {
    Service savedService = get(service.getAppId(), service.getUuid(), false);
    Validator.notNullCheck("Service", savedService);

    UpdateOperations<Service> updateOperations =
        wingsPersistence.createUpdateOperations(Service.class)
            .set("name", service.getName().trim())
            .set("description", service.getDescription() == null ? "" : service.getDescription());

    wingsPersistence.update(savedService, updateOperations);
    Service updatedService = get(service.getAppId(), service.getUuid(), false);

    if (!savedService.getName().equals(service.getName())) {
      executorService.submit(() -> triggerService.updateByApp(service.getAppId()));
      serviceTemplateService.updateDefaultServiceTemplateName(
          service.getAppId(), service.getUuid(), savedService.getName(), service.getName().trim());
    }

    if (!fromYaml) {
      yamlChangeSetHelper.serviceUpdateYamlChangeAsync(service, savedService, updatedService);
    }

    return updatedService;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Service get(String appId, String serviceId) {
    return get(appId, serviceId, true);
  }

  @Override
  public Service getServiceByName(String appId, String serviceName) {
    Service service =
        wingsPersistence.createQuery(Service.class).field("appId").equal(appId).field("name").equal(serviceName).get();
    if (service != null) {
      setServiceDetails(service, appId);
    }
    return service;
  }

  @Override
  public Service get(String appId, String serviceId, boolean includeDetails) {
    Service service = wingsPersistence.get(Service.class, appId, serviceId);
    if (service != null && includeDetails) {
      setServiceDetails(service, appId);
    }
    return service;
  }

  private void setServiceDetails(Service service, String appId) {
    service.setConfigFiles(configService.getConfigFilesForEntity(appId, DEFAULT_TEMPLATE_ID, service.getUuid()));
    service.setServiceVariables(serviceVariableService.getServiceVariablesForEntity(appId, service.getUuid(), false));
    service.setServiceCommands(getServiceCommands(appId, service.getUuid()));
  }

  @Override
  public boolean exist(@NotEmpty String appId, @NotEmpty String serviceId) {
    return wingsPersistence.createQuery(Service.class)
               .field("appId")
               .equal(appId)
               .field(ID_KEY)
               .equal(serviceId)
               .getKey()
        != null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void delete(String appId, String serviceId) {
    Service service = wingsPersistence.get(Service.class, appId, serviceId);
    if (service == null) {
      return;
    }
    delete(service, false);
  }

  private void delete(Service service, boolean forceDelete) {
    if (!forceDelete) {
      // Ensure service is safe to delete
      ensureServiceSafeToDelete(service);
    }

    yamlChangeSetHelper.serviceYamlChange(service, ChangeType.DELETE);

    // First lets make sure that we have persisted a job that will prone the descendant objects
    PruneEntityJob.addDefaultJob(jobScheduler, Service.class, service.getAppId(), service.getUuid());

    // safe to delete
    if (wingsPersistence.delete(Service.class, service.getUuid())) {
      notificationService.sendNotificationAsync(
          anInformationNotification()
              .withAppId(service.getAppId())
              .withNotificationTemplateId(NotificationMessageType.ENTITY_DELETE_NOTIFICATION.name())
              .withNotificationTemplateVariables(
                  ImmutableMap.of("ENTITY_TYPE", "Service", "ENTITY_NAME", service.getName()))
              .build());
    }
  }

  @Override
  public void pruneDescendingEntities(@NotEmpty String appId, @NotEmpty String serviceId) {
    // TODO: Fix this one into the pattern
    executorService.submit(() -> deleteCommands(appId, serviceId));

    List<OwnedByService> services =
        ServiceClassLocator.descendingServices(this, ServiceResourceServiceImpl.class, OwnedByService.class);
    PruneEntityJob.pruneDescendingEntities(
        services, appId, serviceId, descending -> descending.pruneByService(appId, serviceId));
  }

  private void ensureServiceSafeToDelete(Service service) {
    List<Workflow> workflows =
        workflowService
            .listWorkflows(aPageRequest().withLimit(UNLIMITED).addFilter("appId", EQ, service.getAppId()).build())
            .getResponse();

    List<Workflow> serviceWorkflows =
        workflows.stream()
            .filter(wfl -> wfl.getServices().stream().anyMatch(s -> service.getUuid().equals(s.getUuid())))
            .collect(Collectors.toList());

    if (isNotEmpty(serviceWorkflows)) {
      String workflowNames = serviceWorkflows.stream().map(Workflow::getName).collect(Collectors.joining(","));
      String message =
          String.format("Service [%s] couldn't be deleted. Remove Service reference from the following workflows ["
                  + workflowNames + "]",
              service.getName());
      throw new WingsException(INVALID_REQUEST).addParam("message", message);
    }
  }

  private void deleteCommands(String appId, String serviceId) {
    getServiceCommands(appId, serviceId, false)
        .forEach(serviceCommand -> deleteCommand(appId, serviceId, serviceCommand.getUuid()));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Service deleteCommand(String appId, String serviceId, String commandId) {
    Service service = wingsPersistence.get(Service.class, appId, serviceId);
    Validator.notNullCheck("service", service);
    ServiceCommand serviceCommand = wingsPersistence.get(ServiceCommand.class, service.getAppId(), commandId);
    deleteCommand(serviceCommand, service);
    return get(service.getAppId(), service.getUuid());
  }

  private void deleteCommand(ServiceCommand serviceCommand, Service service) {
    ensureServiceCommandSafeToDelete(service, serviceCommand);
    deleteServiceCommand(service, serviceCommand);
  }

  private void deleteServiceCommand(Service service, ServiceCommand serviceCommand) {
    boolean serviceCommandDeleted = wingsPersistence.delete(serviceCommand);
    if (serviceCommandDeleted) {
      wingsPersistence.delete(wingsPersistence.createQuery(Command.class)
                                  .field("appId")
                                  .equal(service.getAppId())
                                  .field("originEntityId")
                                  .equal(serviceCommand.getUuid()));

      executorService.submit(() -> {
        String accountId = appService.getAccountIdByAppId(service.getAppId());
        YamlGitConfig ygs = yamlDirectoryService.weNeedToPushChanges(accountId);
        if (ygs != null) {
          List<GitFileChange> changeSet = new ArrayList<>();
          changeSet.add(
              entityUpdateService.getCommandGitSyncFile(accountId, service, serviceCommand, ChangeType.DELETE));
          yamlChangeSetService.saveChangeSet(ygs, changeSet);
        }
      });
    }
  }

  private void ensureServiceCommandSafeToDelete(Service service, ServiceCommand serviceCommand) {
    List<Workflow> workflows =
        workflowService
            .listWorkflows(aPageRequest().withLimit(UNLIMITED).addFilter("appId", EQ, service.getAppId()).build())
            .getResponse();
    if (workflows == null) {
      return;
    }
    StringBuilder sb = new StringBuilder();
    for (Workflow workflow : workflows) {
      OrchestrationWorkflow orchestrationWorkflow = workflow.getOrchestrationWorkflow();
      if (orchestrationWorkflow instanceof CanaryOrchestrationWorkflow) {
        if (orchestrationWorkflow.isServiceTemplatized()) {
          continue;
        }
        List<WorkflowPhase> workflowPhases = ((CanaryOrchestrationWorkflow) orchestrationWorkflow).getWorkflowPhases();
        for (WorkflowPhase workflowPhase : workflowPhases) {
          if (workflowPhase.checkServiceTemplatized() || !service.getUuid().equals(workflowPhase.getServiceId())) {
            continue;
          }
          for (PhaseStep phaseStep : workflowPhase.getPhaseSteps()) {
            if (phaseStep.getSteps() == null) {
              continue;
            }
            for (Graph.Node step : phaseStep.getSteps()) {
              if ("COMMAND".equals(step.getType())
                  && serviceCommand.getName().equals(step.getProperties().get("commandName"))) {
                sb.append(" (")
                    .append(workflow.getName())
                    .append(':')
                    .append(workflowPhase.getName())
                    .append(':')
                    .append(phaseStep.getName())
                    .append(") ");
              }
            }
          }
        }
      }
    }
    if (sb.length() > 0) {
      String message = String.format(
          "Command [%s] couldn't be deleted. Remove reference from the following workflows [" + sb.toString() + "]",
          serviceCommand.getName());
      throw new WingsException(INVALID_REQUEST).addParam("message", message);
    }
  }

  @Override
  public void pruneByApplication(String appId) {
    findServicesByApp(appId).forEach(service -> {
      wingsPersistence.delete(Service.class, service.getUuid());
      pruneDescendingEntities(appId, service.getUuid());
    });
  }

  @Override
  public List<Service> findServicesByApp(String appId) {
    return wingsPersistence.createQuery(Service.class).field("appId").equal(appId).asList();
  }

  @Override
  public Service get(String appId, String serviceId, SetupStatus status) {
    Service service = get(appId, serviceId);
    if (status == INCOMPLETE) {
      service.setSetup(setupService.getServiceSetupStatus(service));
    }
    return service;
  }

  @Override
  public ContainerTask createContainerTask(ContainerTask containerTask, boolean advanced) {
    return upsertContainerTask(containerTask, advanced, true);
  }

  private ContainerTask upsertContainerTask(ContainerTask containerTask, boolean advanced, boolean isCreate) {
    boolean exist = exist(containerTask.getAppId(), containerTask.getServiceId());
    if (!exist) {
      throw new WingsException(INVALID_REQUEST).addParam("message", "Service doesn't exist");
    }
    ContainerTask persistedContainerTask = wingsPersistence.saveAndGet(ContainerTask.class, containerTask);

    if (advanced) {
      return persistedContainerTask.convertToAdvanced();
    }

    String appId = persistedContainerTask.getAppId();
    String accountId = appService.getAccountIdByAppId(appId);
    Service service = get(appId, persistedContainerTask.getServiceId());

    if (isCreate) {
      yamlChangeSetHelper.containerTaskYamlChangeAsync(accountId, service, persistedContainerTask, ChangeType.ADD);
    } else {
      yamlChangeSetHelper.containerTaskYamlChangeAsync(accountId, service, persistedContainerTask, ChangeType.MODIFY);
    }

    return persistedContainerTask;
  }

  @Override
  public void deleteContainerTask(String appId, String containerTaskId) {
    wingsPersistence.delete(ContainerTask.class, appId, containerTaskId);
  }

  @Override
  public ContainerTask updateContainerTask(ContainerTask containerTask, boolean advanced) {
    return upsertContainerTask(containerTask, advanced, false);
  }

  @Override
  public ContainerTask updateContainerTaskAdvanced(
      String appId, String serviceId, String taskId, ContainerAdvancedPayload advancedPayload, boolean reset) {
    ContainerTask containerTask = wingsPersistence.createQuery(ContainerTask.class)
                                      .field("appId")
                                      .equal(appId)
                                      .field("serviceId")
                                      .equal(serviceId)
                                      .field(ID_KEY)
                                      .equal(taskId)
                                      .get();
    if (reset) {
      containerTask.convertFromAdvanced();
    } else {
      containerTask.setAdvancedType(advancedPayload.getAdvancedType());
      containerTask.setAdvancedConfig(advancedPayload.getAdvancedConfig());
      // Disabling advanced validation since it doesn't work when service variable expressions are used.
      // containerTask.validateAdvanced();
    }
    return upsertContainerTask(containerTask, false, false);
  }

  @Override
  public PageResponse<ContainerTask> listContainerTasks(PageRequest<ContainerTask> pageRequest) {
    return wingsPersistence.query(ContainerTask.class, pageRequest);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Service addCommand(
      String appId, String serviceId, ServiceCommand serviceCommand, boolean defaultCommand, boolean pushToYaml) {
    Service service = wingsPersistence.get(Service.class, appId, serviceId);
    Validator.notNullCheck("service", service);

    if (!isLinearCommandGraph(serviceCommand)) {
      WingsException wingsException =
          new WingsException(ErrorCode.INVALID_PIPELINE, new IllegalArgumentException("Graph is not a pipeline"));
      wingsException.addParam("message", "Graph is not a linear pipeline");
      throw wingsException;
    }

    serviceCommand.setDefaultVersion(1);
    serviceCommand.setServiceId(serviceId);
    serviceCommand.setAppId(appId);
    serviceCommand.setName(serviceCommand.getCommand().getGraph().getGraphName());

    Command command = serviceCommand.getCommand();
    String notes = serviceCommand.getNotes();

    serviceCommand = wingsPersistence.saveAndGet(ServiceCommand.class, serviceCommand);
    entityVersionService.newEntityVersion(appId, EntityType.COMMAND, serviceCommand.getUuid(), serviceId,
        serviceCommand.getName(), EntityVersion.ChangeType.CREATED, notes);

    command.transformGraph();
    command.setVersion(1L);
    command.setOriginEntityId(serviceCommand.getUuid());
    command.setAppId(appId);
    if (isNotEmpty(command.getCommandUnits())) {
      command.setDeploymentType(command.getCommandUnits().get(0).getDeploymentType());
    }

    commandService.save(command, defaultCommand, pushToYaml);
    service.getServiceCommands().add(serviceCommand);

    wingsPersistence.save(service);
    return get(appId, serviceId);
  }

  private boolean isLinearCommandGraph(ServiceCommand serviceCommand) {
    try {
      return serviceCommand.getCommand().getGraph().isLinear();
    } catch (Exception ex) {
      logger.error("Exception in validating command graph " + serviceCommand.getCommand(), ex);
      return false;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Service updateCommand(String appId, String serviceId, ServiceCommand serviceCommand) {
    Service service = wingsPersistence.get(Service.class, appId, serviceId);
    Validator.notNullCheck("service", service);

    UpdateOperations<ServiceCommand> updateOperation = wingsPersistence.createUpdateOperations(ServiceCommand.class);

    if (serviceCommand.getCommand() != null) {
      if (!isLinearCommandGraph(serviceCommand)) {
        WingsException wingsException =
            new WingsException(ErrorCode.INVALID_PIPELINE, new IllegalArgumentException("Graph is not a pipeline"));
        wingsException.addParam("message", "Graph is not a linear pipeline");
        throw wingsException;
      }

      EntityVersion lastEntityVersion =
          entityVersionService.lastEntityVersion(appId, EntityType.COMMAND, serviceCommand.getUuid(), serviceId);
      Command command = aCommand().withGraph(serviceCommand.getCommand().getGraph()).build();
      command.transformGraph();
      command.setOriginEntityId(serviceCommand.getUuid());
      command.setAppId(appId);
      command.setUuid(null);

      Command oldCommand = commandService.getCommand(appId, serviceCommand.getUuid(), lastEntityVersion.getVersion());

      DiffNode commandUnitDiff =
          ObjectDifferBuilder.buildDefault().compare(command.getCommandUnits(), oldCommand.getCommandUnits());
      ObjectDifferBuilder builder = ObjectDifferBuilder.startBuilding();
      builder.inclusion().exclude().node(NodePath.with("linearGraphIterator"));
      DiffNode graphDiff = builder.build().compare(command.getGraph(), oldCommand.getGraph());

      if (commandUnitDiff.hasChanges()) {
        EntityVersion entityVersion =
            entityVersionService.newEntityVersion(appId, EntityType.COMMAND, serviceCommand.getUuid(), serviceId,
                serviceCommand.getName(), EntityVersion.ChangeType.UPDATED, serviceCommand.getNotes());
        command.setVersion(Long.valueOf(entityVersion.getVersion().intValue()));
        // Copy the old command values
        command.setDeploymentType(oldCommand.getDeploymentType());
        command.setCommandType(oldCommand.getCommandType());
        command.setArtifactType(oldCommand.getArtifactType());
        commandService.save(command, false, true);

        if (serviceCommand.getSetAsDefault()) {
          serviceCommand.setDefaultVersion(entityVersion.getVersion());
        }
      } else if (graphDiff.hasChanges()) {
        oldCommand.setGraph(command.getGraph());
        commandService.update(oldCommand);
      }
    }

    setUnset(updateOperation, "envIdVersionMap", serviceCommand.getEnvIdVersionMap());
    setUnset(updateOperation, "defaultVersion", serviceCommand.getDefaultVersion());
    wingsPersistence.update(
        wingsPersistence.createQuery(ServiceCommand.class).field(ID_KEY).equal(serviceCommand.getUuid()),
        updateOperation);

    return get(appId, serviceId);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ServiceCommand getCommandByName(
      @NotEmpty String appId, @NotEmpty String serviceId, @NotEmpty String commandName) {
    Service service = getServiceWithServiceCommands(appId, serviceId);
    if (service != null) {
      return service.getServiceCommands()
          .stream()
          .filter(command -> equalsIgnoreCase(commandName, command.getName()))
          .findFirst()
          .orElse(null);
    }
    return null;
  }

  @Override
  public ServiceCommand getCommandByName(
      @NotEmpty String appId, @NotEmpty String serviceId, @NotEmpty String envId, @NotEmpty String commandName) {
    Service service = getServiceWithServiceCommands(appId, serviceId);

    ServiceCommand serviceCommand = service.getServiceCommands()
                                        .stream()
                                        .filter(command -> equalsIgnoreCase(commandName, command.getName()))
                                        .findFirst()
                                        .orElse(null);

    if (serviceCommand != null
        && (serviceCommand.getEnvIdVersionMap().get(envId) != null || serviceCommand.isTargetToAllEnv())) {
      serviceCommand.setCommand(commandService.getCommand(appId, serviceCommand.getUuid(),
          Optional
              .ofNullable(
                  Optional.ofNullable(serviceCommand.getEnvIdVersionMap()).orElse(Collections.emptyMap()).get(envId))
              .orElse(anEntityVersion().withVersion(serviceCommand.getDefaultVersion()).build())
              .getVersion()));
    } else {
      return null;
    }
    return serviceCommand;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ServiceCommand getCommandByNameAndVersion(
      @NotEmpty String appId, @NotEmpty String serviceId, @NotEmpty String commandName, int version) {
    ServiceCommand command = getServiceCommands(appId, serviceId, false)
                                 .stream()
                                 .filter(serviceCommand -> equalsIgnoreCase(commandName, serviceCommand.getName()))
                                 .findFirst()
                                 .get();
    command.setCommand(commandService.getCommand(appId, command.getUuid(), version));
    return command;
  }

  @Override
  public Service getServiceWithServiceCommands(String appId, String serviceId) {
    Service service = wingsPersistence.get(Service.class, appId, serviceId);
    service.setServiceCommands(getServiceCommands(appId, serviceId));
    service.getServiceCommands().forEach(serviceCommand
        -> serviceCommand.setCommand(
            commandService.getCommand(appId, serviceCommand.getUuid(), serviceCommand.getDefaultVersion())));
    return service;
  }

  @Override
  public UserDataSpecification createUserDataSpecification(UserDataSpecification userDataSpecification) {
    return upsertUserDataSpecification(userDataSpecification, true);
  }

  public UserDataSpecification upsertUserDataSpecification(
      UserDataSpecification userDataSpecification, boolean isCreate) {
    UserDataSpecification persistedUserDataSpec =
        wingsPersistence.saveAndGet(UserDataSpecification.class, userDataSpecification);
    String appId = persistedUserDataSpec.getAppId();
    String accountId = appService.getAccountIdByAppId(appId);
    Service service = get(appId, persistedUserDataSpec.getServiceId());
    if (isCreate) {
      yamlChangeSetHelper.userDataSpecYamlChangeAsync(accountId, service, persistedUserDataSpec, ChangeType.ADD);
    } else {
      yamlChangeSetHelper.userDataSpecYamlChangeAsync(accountId, service, persistedUserDataSpec, ChangeType.MODIFY);
    }
    return persistedUserDataSpec;
  }

  @Override
  public UserDataSpecification updateUserDataSpecification(UserDataSpecification userDataSpecification) {
    return upsertUserDataSpecification(userDataSpecification, false);
  }

  @Override
  public PageResponse<UserDataSpecification> listUserDataSpecification(PageRequest<UserDataSpecification> pageRequest) {
    return wingsPersistence.query(UserDataSpecification.class, pageRequest);
  }

  @Override
  public UserDataSpecification getUserDataSpecification(String appId, String serviceId) {
    return wingsPersistence.createQuery(UserDataSpecification.class)
        .field("appId")
        .equal(appId)
        .field("serviceId")
        .equal(serviceId)
        .get();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<Stencil> getCommandStencils(@NotEmpty String appId, @NotEmpty String serviceId, String commandName) {
    return stencilPostProcessor.postProcess(asList(CommandUnitType.values()), appId, serviceId, commandName);
  }

  @Override
  public List<Stencil> getContainerTaskStencils(@NotEmpty String appId, @NotEmpty String serviceId) {
    return stencilPostProcessor.postProcess(asList(ContainerTaskType.values()), appId, serviceId);
  }

  @Override
  public ContainerTask getContainerTaskByDeploymentType(String appId, String serviceId, String deploymentType) {
    return wingsPersistence.createQuery(ContainerTask.class)
        .field("appId")
        .equal(appId)
        .field("serviceId")
        .equal(serviceId)
        .field("deploymentType")
        .equal(deploymentType)
        .get();
  }

  @Override
  public ContainerTask getContainerTaskById(String appId, String containerTaskId) {
    return wingsPersistence.get(ContainerTask.class, appId, containerTaskId);
  }

  @Override
  public LambdaSpecification getLambdaSpecificationById(String appId, String lambdaSpecificationId) {
    return wingsPersistence.get(LambdaSpecification.class, appId, lambdaSpecificationId);
  }

  @Override
  public UserDataSpecification getUserDataSpecificationById(String appId, String userDataSpecificationId) {
    return wingsPersistence.get(UserDataSpecification.class, appId, userDataSpecificationId);
  }

  @Override
  public Map<String, String> getData(String appId, String... params) {
    Service service = wingsPersistence.get(Service.class, appId, params[0]);
    List<ServiceCommand> serviceCommands = getServiceCommands(service.getAppId(), service.getUuid(), false);
    if (isEmpty(serviceCommands)) {
      return emptyMap();
    } else {
      return serviceCommands.stream()
          .filter(serviceCommand -> !StringUtils.equals(serviceCommand.getName(), params[1]))
          .collect(toMap(ServiceCommand::getName, ServiceCommand::getName));
    }
  }

  @Override
  public LambdaSpecification createLambdaSpecification(LambdaSpecification lambdaSpecification) {
    return upsertLambdaSpecification(lambdaSpecification, true);
  }

  private void validateLambdaSpecification(LambdaSpecification lambdaSpecification) {
    List<String> duplicateFunctionName =
        getFunctionAttributeDuplicateValues(lambdaSpecification, FunctionSpecification::getFunctionName);
    if (isNotEmpty(duplicateFunctionName)) {
      throw new WingsException(INVALID_REQUEST)
          .addParam("message",
              "Function name should be unique. Duplicate function names: [" + Joiner.on(",").join(duplicateFunctionName)
                  + "]");
    }
    List<String> duplicateHandlerName =
        getFunctionAttributeDuplicateValues(lambdaSpecification, FunctionSpecification::getHandler);
    if (isNotEmpty(duplicateHandlerName)) {
      throw new WingsException(INVALID_REQUEST)
          .addParam("message",
              "Function Handler name should be unique. Duplicate function handlers: ["
                  + Joiner.on(",").join(duplicateHandlerName) + "]");
    }
  }

  private List<String> getFunctionAttributeDuplicateValues(
      LambdaSpecification lambdaSpecification, Function<FunctionSpecification, String> getAttributeValue) {
    Map<String, Long> valueCountMap = lambdaSpecification.getFunctions().stream().collect(
        Collectors.groupingBy(getAttributeValue, Collectors.counting()));
    return valueCountMap.entrySet()
        .stream()
        .filter(stringLongEntry -> stringLongEntry.getValue() > 1)
        .map(Entry::getKey)
        .collect(Collectors.toList());
  }

  @Override
  public LambdaSpecification updateLambdaSpecification(LambdaSpecification lambdaSpecification) {
    return upsertLambdaSpecification(lambdaSpecification, false);
  }

  private LambdaSpecification upsertLambdaSpecification(LambdaSpecification lambdaSpecification, boolean isCreate) {
    validateLambdaSpecification(lambdaSpecification);

    LambdaSpecification persistedLambdaSpec =
        wingsPersistence.saveAndGet(LambdaSpecification.class, lambdaSpecification);
    String appId = persistedLambdaSpec.getAppId();
    String accountId = appService.getAccountIdByAppId(appId);
    Service service = get(appId, persistedLambdaSpec.getServiceId());
    if (isCreate) {
      yamlChangeSetHelper.lamdbaSpecYamlChangeAsync(accountId, service, persistedLambdaSpec, ChangeType.ADD);
    } else {
      yamlChangeSetHelper.lamdbaSpecYamlChangeAsync(accountId, service, persistedLambdaSpec, ChangeType.MODIFY);
    }

    return persistedLambdaSpec;
  }

  @Override
  public PageResponse<LambdaSpecification> listLambdaSpecification(PageRequest<LambdaSpecification> pageRequest) {
    return wingsPersistence.query(LambdaSpecification.class, pageRequest);
  }

  @Override
  public LambdaSpecification getLambdaSpecification(String appId, String serviceId) {
    return wingsPersistence.createQuery(LambdaSpecification.class)
        .field("appId")
        .equal(appId)
        .field("serviceId")
        .equal(serviceId)
        .get();
  }

  @Override
  public boolean isArtifactNeeded(Service service) {
    return getServiceCommands(service.getAppId(), service.getUuid(), false)
        .stream()
        .anyMatch(serviceCommand
            -> commandService
                   .getCommand(service.getAppId(), serviceCommand.getUuid(), serviceCommand.getDefaultVersion())
                   .isArtifactNeeded());
  }

  @Override
  public List<ServiceCommand> getServiceCommands(String appId, String serviceId) {
    return getServiceCommands(appId, serviceId, true);
  }

  @Override
  public List<ServiceCommand> getServiceCommands(String appId, String serviceId, boolean withCommandDetails) {
    PageRequest<ServiceCommand> serviceCommandPageRequest =
        aPageRequest().withLimit(UNLIMITED).addFilter("appId", EQ, appId).addFilter("serviceId", EQ, serviceId).build();
    List<ServiceCommand> serviceCommands =
        wingsPersistence.query(ServiceCommand.class, serviceCommandPageRequest).getResponse();
    if (withCommandDetails) {
      serviceCommands.forEach(serviceCommand
          -> serviceCommand.setCommand(
              commandService.getCommand(appId, serviceCommand.getUuid(), serviceCommand.getDefaultVersion())));
    }
    return serviceCommands;
  }
}
