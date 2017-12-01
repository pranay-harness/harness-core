package software.wings.service.impl;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.lang3.StringUtils.equalsIgnoreCase;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.sshd.common.util.GenericUtils.isEmpty;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.beans.ConfigFile.DEFAULT_TEMPLATE_ID;
import static software.wings.beans.EntityVersion.Builder.anEntityVersion;
import static software.wings.beans.ErrorCode.INVALID_REQUEST;
import static software.wings.beans.InformationNotification.Builder.anInformationNotification;
import static software.wings.beans.SearchFilter.Operator.EQ;
import static software.wings.beans.Setup.SetupStatus.INCOMPLETE;
import static software.wings.beans.command.Command.Builder.aCommand;
import static software.wings.beans.command.CommandUnitType.COMMAND;
import static software.wings.beans.command.ServiceCommand.Builder.aServiceCommand;
import static software.wings.dl.MongoHelper.setUnset;
import static software.wings.dl.PageRequest.Builder.aPageRequest;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;

import de.danielbechler.diff.ObjectDifferBuilder;
import de.danielbechler.diff.node.DiffNode;
import de.danielbechler.diff.path.NodePath;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.query.UpdateOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Application;
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
import software.wings.beans.yaml.Change.ChangeType;
import software.wings.beans.yaml.GitFileChange;
import software.wings.common.NotificationMessageResolver.NotificationMessageType;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
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
import software.wings.service.intfc.yaml.EntityUpdateService;
import software.wings.service.intfc.yaml.YamlChangeSetService;
import software.wings.service.intfc.yaml.YamlDirectoryService;
import software.wings.stencils.DataProvider;
import software.wings.stencils.Stencil;
import software.wings.stencils.StencilPostProcessor;
import software.wings.utils.ArtifactType;
import software.wings.utils.BoundedInputStream;
import software.wings.utils.Misc;
import software.wings.utils.Validator;
import software.wings.yaml.gitSync.YamlGitConfig;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.validation.executable.ValidateOnExecution;

/**
 * Created by anubhaw on 3/25/16.
 */
@ValidateOnExecution
@Singleton
public class ServiceResourceServiceImpl implements ServiceResourceService, DataProvider {
  private final Logger logger = LoggerFactory.getLogger(ServiceResourceServiceImpl.class);

  @Inject private WingsPersistence wingsPersistence;
  @Inject private ConfigService configService;
  @Inject private ServiceTemplateService serviceTemplateService;
  @Inject private ExecutorService executorService;
  @Inject private StencilPostProcessor stencilPostProcessor;
  @Inject private ServiceVariableService serviceVariableService;
  @Inject private ActivityService activityService;
  @Inject private SetupService setupService;
  @Inject private NotificationService notificationService;
  @Inject private EntityVersionService entityVersionService;
  @Inject private CommandService commandService;
  @Inject private ArtifactStreamService artifactStreamService;
  @Inject private WorkflowService workflowService;
  @Inject private AppService appService;
  @Inject private YamlDirectoryService yamlDirectoryService;
  @Inject private EntityUpdateService entityUpdateService;
  @Inject private YamlChangeSetService yamlChangeSetService;
  @Inject private TriggerService triggerService;

  /**
   * {@inheritDoc}
   */
  @Override
  public PageResponse<Service> list(
      PageRequest<Service> request, boolean withBuildSource, boolean withServiceCommands) {
    PageResponse<Service> pageResponse = wingsPersistence.query(Service.class, request);

    if (withServiceCommands) {
      pageResponse.getResponse().forEach(service -> {
        try {
          service.getServiceCommands().forEach(serviceCommand
              -> serviceCommand.setCommand(commandService.getCommand(
                  serviceCommand.getAppId(), serviceCommand.getUuid(), serviceCommand.getDefaultVersion())));
        } catch (Exception e) {
          logger.error("Failed to retrieve service commands for serviceId {}  of appId  {}", service.getUuid(),
              service.getAppId(), e);
        }
      });
    }
    SearchFilter appIdSearchFilter = request.getFilters()
                                         .stream()
                                         .filter(searchFilter -> searchFilter.getFieldName().equals("appId"))
                                         .findFirst()
                                         .orElse(null);
    if (withBuildSource && appIdSearchFilter != null) {
      List<ArtifactStream> artifactStreams = new ArrayList<>();
      try {
        artifactStreams = artifactStreamService.list(aPageRequest().addFilter(appIdSearchFilter).build()).getResponse();
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
  public Service save(Service service, boolean fromYaml) {
    Service savedService =
        Validator.duplicateCheck(() -> wingsPersistence.saveAndGet(Service.class, service), "name", service.getName());
    if (!fromYaml) {
      savedService = addDefaultCommands(savedService);
    }
    serviceTemplateService.createDefaultTemplatesByService(savedService);
    notificationService.sendNotificationAsync(
        anInformationNotification()
            .withAppId(savedService.getAppId())
            .withNotificationTemplateId(NotificationMessageType.ENTITY_CREATE_NOTIFICATION.name())
            .withNotificationTemplateVariables(
                ImmutableMap.of("ENTITY_TYPE", "Service", "ENTITY_NAME", savedService.getName()))
            .build());

    Service finalSavedService = savedService;
    executorService.submit(() -> queueServiceYamlChangeSet(finalSavedService, ChangeType.ADD));
    return savedService;
  }

  @Override
  public Service clone(String appId, String originalServiceId, Service service) {
    Service originalService = get(appId, originalServiceId, true);
    Service clonedService = originalService.clone();
    clonedService.setName(service.getName());
    clonedService.setDescription(service.getDescription());

    Service savedCloneService = Validator.duplicateCheck(
        () -> wingsPersistence.saveAndGet(Service.class, clonedService), "name", service.getName());

    originalService.getServiceCommands().forEach(serviceCommand -> {
      ServiceCommand clonedServiceCommand = serviceCommand.clone();
      addCommand(savedCloneService.getAppId(), savedCloneService.getUuid(), clonedServiceCommand, false);
    });

    List<ServiceTemplate> serviceTemplates = serviceTemplateService
                                                 .list(aPageRequest()
                                                           .addFilter("appId", EQ, originalService.getAppId())
                                                           .addFilter("serviceId", EQ, originalService.getUuid())
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
      clonedServiceVariable.setEntityId(savedCloneService.getUuid());

      serviceVariableService.save(clonedServiceVariable);
    });
    return savedCloneService;
  }

  @Override
  public Service cloneCommand(String appId, String serviceId, String commandName, ServiceCommand command) {
    // don't allow cloning of Docker commands
    Service service = get(appId, serviceId);
    if (service.getArtifactType().equals(ArtifactType.DOCKER)) {
      throw new WingsException(INVALID_REQUEST, "message", "Docker commands can not be cloned");
    }
    ServiceCommand oldServiceCommand = service.getServiceCommands()
                                           .stream()
                                           .filter(cmd -> equalsIgnoreCase(commandName, cmd.getName()))
                                           .findFirst()
                                           .orElse(null);
    ServiceCommand clonedServiceCommand = oldServiceCommand.clone();
    clonedServiceCommand.getCommand().getGraph().setGraphName(command.getName());
    return addCommand(appId, serviceId, clonedServiceCommand, false);
  }

  @Override
  public List<CommandUnit> getFlattenCommandUnitList(String appId, String serviceId, String envId, String commandName) {
    Map<String, Integer> commandNameVersionMap =
        get(appId, serviceId)
            .getServiceCommands()
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

  private Service addDefaultCommands(Service service) {
    List<Command> commands = emptyList();
    if (service.getAppContainer() != null && service.getAppContainer().getFamily() != null) {
      commands = service.getAppContainer().getFamily().getDefaultCommands(
          service.getArtifactType(), service.getAppContainer());
    } else if (service.getArtifactType() != null) {
      commands = service.getArtifactType().getDefaultCommands();
    }

    Service serviceToReturn = service;
    for (Command command : commands) {
      serviceToReturn = addCommand(service.getAppId(), service.getUuid(),
          aServiceCommand().withTargetToAllEnv(true).withCommand(command).build(), true);
    }

    return serviceToReturn;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Service update(Service service) {
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
    }

    executorService.submit(() -> {
      if (!savedService.getName().equals(service.getName())) { // Service name changed
        executorService.submit(()
                                   -> serviceTemplateService.updateDefaultServiceTemplateName(service.getAppId(),
                                       service.getUuid(), savedService.getName(), service.getName().trim()));
        queueMoveServiceChange(savedService, service);
      } else {
        queueServiceYamlChangeSet(updatedService, ChangeType.MODIFY);
      }
    });
    return updatedService;
  }

  private void queueMoveServiceChange(Service oldService, Service newService) {
    String accountId = appService.getAccountIdByAppId(newService.getAppId());
    YamlGitConfig ygs = yamlDirectoryService.weNeedToPushChanges(accountId);
    if (ygs != null) {
      List<GitFileChange> changeSet = new ArrayList<>();

      String oldPath = yamlDirectoryService.getRootPathByService(oldService);
      String newPath = yamlDirectoryService.getRootPathByService(newService);

      changeSet.add(GitFileChange.Builder.aGitFileChange()
                        .withAccountId(accountId)
                        .withChangeType(ChangeType.RENAME)
                        .withFilePath(newPath)
                        .withOldFilePath(oldPath)
                        .build());
      changeSet.add(entityUpdateService.getServiceGitSyncFile(accountId, newService, ChangeType.MODIFY));
      yamlChangeSetService.queueChangeSet(ygs, changeSet);
    }
  }

  private void queueServiceYamlChangeSet(Service service, ChangeType crudType) {
    // check whether we need to push changes (through git sync)
    String accountId = appService.getAccountIdByAppId(service.getAppId());
    YamlGitConfig ygs = yamlDirectoryService.weNeedToPushChanges(accountId);
    if (ygs != null) {
      List<GitFileChange> changeSet = new ArrayList<>();

      changeSet.add(entityUpdateService.getServiceGitSyncFile(accountId, service, crudType));
      if (crudType.equals(ChangeType.ADD)) {
        service.getServiceCommands().forEach(serviceCommand
            -> changeSet.add(
                entityUpdateService.getCommandGitSyncFile(accountId, service, serviceCommand, ChangeType.ADD)));
      }
      yamlChangeSetService.queueChangeSet(ygs, changeSet);
    }
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
    service.setLastDeploymentActivity(activityService.getLastActivityForService(appId, service.getUuid()));
    service.setLastProdDeploymentActivity(
        activityService.getLastProductionActivityForService(appId, service.getUuid()));
    service.getServiceCommands().forEach(serviceCommand
        -> serviceCommand.setCommand(
            commandService.getCommand(appId, serviceCommand.getUuid(), serviceCommand.getDefaultVersion())));
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

    // safe to delete
    String serviceId = service.getUuid();
    String appId = service.getAppId();
    boolean deleted = wingsPersistence.delete(Service.class, serviceId);
    if (deleted) {
      executorService.submit(() -> deleteCommands(service));
      executorService.submit(() -> serviceTemplateService.deleteByService(appId, serviceId));
      executorService.submit(() -> artifactStreamService.deleteByService(appId, serviceId));
      executorService.submit(() -> configService.deleteByEntityId(appId, DEFAULT_TEMPLATE_ID, serviceId));
      executorService.submit(() -> serviceVariableService.deleteByEntityId(appId, serviceId));
      notificationService.sendNotificationAsync(
          anInformationNotification()
              .withAppId(service.getAppId())
              .withNotificationTemplateId(NotificationMessageType.ENTITY_DELETE_NOTIFICATION.name())
              .withNotificationTemplateVariables(
                  ImmutableMap.of("ENTITY_TYPE", "Service", "ENTITY_NAME", service.getName()))
              .build());

      executorService.submit(() -> queueServiceYamlChangeSet(service, ChangeType.DELETE));
    }
  }

  private void ensureServiceSafeToDelete(Service service) {
    List<Workflow> workflows =
        workflowService
            .listWorkflows(
                aPageRequest().withLimit(PageRequest.UNLIMITED).addFilter("appId", EQ, service.getAppId()).build())
            .getResponse();

    List<String> referencingWorkflowNames =
        workflows.stream()
            .filter(wfl -> {
              if (wfl.getOrchestrationWorkflow() != null
                  && wfl.getOrchestrationWorkflow() instanceof CanaryOrchestrationWorkflow) {
                Map<String, WorkflowPhase> workflowPhaseIdMap =
                    ((CanaryOrchestrationWorkflow) wfl.getOrchestrationWorkflow()).getWorkflowPhaseIdMap();
                return workflowPhaseIdMap.values().stream().anyMatch(
                    workflowPhase -> service.getUuid().equals(workflowPhase.getServiceId()));
              }
              return false;
            })
            .map(Workflow::getName)
            .collect(Collectors.toList());
    if (referencingWorkflowNames.size() > 0) {
      throw new WingsException(INVALID_REQUEST, "message",
          String.format("Service is in use by %s workflow%s [%s].", referencingWorkflowNames.size(),
              referencingWorkflowNames.size() == 1 ? "" : "s", Joiner.on(", ").join(referencingWorkflowNames)));
    }
  }

  private void deleteCommands(Service service) {
    service.getServiceCommands().forEach(serviceCommand -> deleteCommand(serviceCommand, service));
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

    wingsPersistence.update(wingsPersistence.createQuery(Service.class)
                                .field(ID_KEY)
                                .equal(service.getUuid())
                                .field("appId")
                                .equal(service.getAppId()),
        wingsPersistence.createUpdateOperations(Service.class).removeAll("serviceCommands", serviceCommand.getUuid()));

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
          yamlChangeSetService.queueChangeSet(ygs, changeSet);
        }
      });
    }
  }

  private void ensureServiceCommandSafeToDelete(Service service, ServiceCommand serviceCommand) {
    List<Workflow> workflows =
        workflowService
            .listWorkflows(
                aPageRequest().withLimit(PageRequest.UNLIMITED).addFilter("appId", EQ, service.getAppId()).build())
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
                    .append(":")
                    .append(workflowPhase.getName())
                    .append(":")
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
      throw new WingsException(INVALID_REQUEST, "message", message);
    }
  }

  @Override
  public void deleteByApp(Application application) {
    wingsPersistence.createQuery(Service.class)
        .field("appId")
        .equal(application.getUuid())
        .asList()
        .forEach(service -> {
          service.setEntityYamlPath(yamlDirectoryService.getRootPathByService(service, application.entityYamlPath));
          delete(service, true);
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
    boolean exist = exist(containerTask.getAppId(), containerTask.getServiceId());
    if (!exist) {
      throw new WingsException(INVALID_REQUEST, "message", "Service doesn't exists");
    }
    ContainerTask persistedContainerTask = wingsPersistence.saveAndGet(ContainerTask.class, containerTask);
    if (advanced) {
      return persistedContainerTask.convertToAdvanced();
    }
    return persistedContainerTask;
  }

  @Override
  public void deleteContainerTask(String appId, String containerTaskId) {
    wingsPersistence.delete(ContainerTask.class, appId, containerTaskId);
  }

  @Override
  public ContainerTask updateContainerTask(ContainerTask containerTask, boolean advanced) {
    return createContainerTask(containerTask, advanced);
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
      containerTask.validateAdvanced();
    }
    return createContainerTask(containerTask, false);
  }

  @Override
  public PageResponse<ContainerTask> listContainerTasks(PageRequest<ContainerTask> pageRequest) {
    return wingsPersistence.query(ContainerTask.class, pageRequest);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Service addCommand(String appId, String serviceId, ServiceCommand serviceCommand, boolean isDefaultCommand) {
    Service service = wingsPersistence.get(Service.class, appId, serviceId);
    Validator.notNullCheck("service", service);

    if (!isLinearCommandGraph(serviceCommand)) {
      final WingsException wingsException =
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
    if (command.getCommandUnits() != null && command.getCommandUnits().size() > 0) {
      command.setDeploymentType(command.getCommandUnits().get(0).getDeploymentType());
    }

    commandService.save(command, isDefaultCommand);

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
        final WingsException wingsException =
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
        commandService.save(command, false);

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
    Service service = get(appId, serviceId);
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
    Service service = get(appId, serviceId);
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
    Service service = get(appId, serviceId);
    ServiceCommand command = service.getServiceCommands()
                                 .stream()
                                 .filter(serviceCommand -> equalsIgnoreCase(commandName, serviceCommand.getName()))
                                 .findFirst()
                                 .get();
    command.setCommand(commandService.getCommand(appId, command.getUuid(), version));
    return command;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<Stencil> getCommandStencils(@NotEmpty String appId, @NotEmpty String serviceId, String commandName) {
    return stencilPostProcessor.postProcess(Arrays.asList(CommandUnitType.values()), appId, serviceId, commandName);
  }

  @Override
  public List<Stencil> getContainerTaskStencils(@NotEmpty String appId, @NotEmpty String serviceId) {
    return stencilPostProcessor.postProcess(Arrays.asList(ContainerTaskType.values()), appId, serviceId);
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
  public Map<String, String> getData(String appId, String... params) {
    Service service = get(appId, params[0]);
    if (isEmpty(service.getServiceCommands())) {
      return emptyMap();
    } else {
      return service.getServiceCommands()
          .stream()
          .filter(command -> !StringUtils.equals(command.getName(), params[1]))
          .collect(toMap(ServiceCommand::getName, ServiceCommand::getName));
    }
  }

  @Override
  public LambdaSpecification createLambdaSpecification(LambdaSpecification lambdaSpecification) {
    validateLambdaSpecification(lambdaSpecification);
    return wingsPersistence.saveAndGet(LambdaSpecification.class, lambdaSpecification);
  }

  private void validateLambdaSpecification(LambdaSpecification lambdaSpecification) {
    List<String> duplicateFunctionName =
        getFunctionAttributeDuplicateValues(lambdaSpecification, FunctionSpecification::getFunctionName);
    if (!Misc.isNullOrEmpty(duplicateFunctionName)) {
      throw new WingsException(INVALID_REQUEST, "message",
          "Function name should be unique. Duplicate function names: [" + Joiner.on(",").join(duplicateFunctionName)
              + "]");
    }
    List<String> duplicateHandlerName =
        getFunctionAttributeDuplicateValues(lambdaSpecification, FunctionSpecification::getHandler);
    if (!Misc.isNullOrEmpty(duplicateHandlerName)) {
      throw new WingsException(INVALID_REQUEST, "message",
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
    return createLambdaSpecification(lambdaSpecification);
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
    return service.getServiceCommands().stream().anyMatch(serviceCommand
        -> commandService.getCommand(service.getAppId(), serviceCommand.getUuid(), serviceCommand.getDefaultVersion())
               .isArtifactNeeded());
  }
}
