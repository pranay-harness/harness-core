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
import static software.wings.beans.ErrorCode.COMMAND_DOES_NOT_EXIST;
import static software.wings.beans.ErrorCode.INVALID_REQUEST;
import static software.wings.beans.InformationNotification.Builder.anInformationNotification;
import static software.wings.beans.SearchFilter.Operator.EQ;
import static software.wings.beans.Setup.SetupStatus.INCOMPLETE;
import static software.wings.beans.command.Command.Builder.aCommand;
import static software.wings.beans.command.CommandUnitType.COMMAND;
import static software.wings.beans.command.ServiceCommand.Builder.aServiceCommand;
import static software.wings.dl.MongoHelper.setUnset;
import static software.wings.dl.PageRequest.Builder.aPageRequest;

import com.google.common.collect.ImmutableMap;

import de.danielbechler.diff.ObjectDifferBuilder;
import de.danielbechler.diff.node.DiffNode;
import de.danielbechler.diff.path.NodePath;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.query.UpdateOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.ConfigFile;
import software.wings.beans.EntityType;
import software.wings.beans.EntityVersion;
import software.wings.beans.EntityVersion.ChangeType;
import software.wings.beans.ErrorCode;
import software.wings.beans.SearchFilter;
import software.wings.beans.Service;
import software.wings.beans.ServiceTemplate;
import software.wings.beans.ServiceVariable;
import software.wings.beans.Setup.SetupStatus;
import software.wings.beans.Workflow;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.command.Command;
import software.wings.beans.command.CommandUnit;
import software.wings.beans.command.CommandUnitType;
import software.wings.beans.command.ServiceCommand;
import software.wings.beans.container.ContainerTask;
import software.wings.beans.container.ContainerTaskType;
import software.wings.common.NotificationMessageResolver.NotificationMessageType;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.CommandService;
import software.wings.service.intfc.ConfigService;
import software.wings.service.intfc.EntityVersionService;
import software.wings.service.intfc.NotificationService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.ServiceTemplateService;
import software.wings.service.intfc.ServiceVariableService;
import software.wings.service.intfc.SetupService;
import software.wings.service.intfc.WorkflowService;
import software.wings.stencils.DataProvider;
import software.wings.stencils.Stencil;
import software.wings.stencils.StencilPostProcessor;
import software.wings.utils.ArtifactType;
import software.wings.utils.Validator;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
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

  /**
   * {@inheritDoc}
   */
  @Override
  public PageResponse<Service> list(
      PageRequest<Service> request, boolean withBuildSource, boolean withServiceCommands) {
    PageResponse<Service> pageResponse = wingsPersistence.query(Service.class, request);

    if (withServiceCommands) {
      pageResponse.getResponse().forEach(service -> {
        service.getServiceCommands().forEach(serviceCommand
            -> serviceCommand.setCommand(commandService.getCommand(
                serviceCommand.getAppId(), serviceCommand.getUuid(), serviceCommand.getDefaultVersion())));
      });
    }

    SearchFilter appIdSearchFilter = request.getFilters()
                                         .stream()
                                         .filter(searchFilter -> searchFilter.getFieldName().equals("appId"))
                                         .findFirst()
                                         .orElse(null);
    if (withBuildSource && appIdSearchFilter != null) {
      List<ArtifactStream> artifactStreams =
          artifactStreamService.list(aPageRequest().addFilter(appIdSearchFilter).build()).getResponse();
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
    Service savedService = wingsPersistence.saveAndGet(Service.class, service);
    savedService = addDefaultCommands(savedService);
    serviceTemplateService.createDefaultTemplatesByService(savedService);
    notificationService.sendNotificationAsync(
        anInformationNotification()
            .withAppId(savedService.getAppId())
            .withNotificationTemplateId(NotificationMessageType.ENTITY_CREATE_NOTIFICATION.name())
            .withNotificationTemplateVariables(
                ImmutableMap.of("ENTITY_TYPE", "Service", "ENTITY_NAME", savedService.getName()))
            .build());
    return savedService;
  }

  @Override
  public Service clone(String appId, String originalServiceId, Service service) {
    Service originalService = get(appId, originalServiceId, true);
    Service clonedService = originalService.clone();
    clonedService.setName(service.getName());
    clonedService.setDescription(service.getDescription());

    Service savedCloneService = wingsPersistence.saveAndGet(Service.class, clonedService);

    originalService.getServiceCommands().forEach(serviceCommand -> {
      ServiceCommand clonedServiceCommand = serviceCommand.clone();
      addCommand(savedCloneService.getAppId(), savedCloneService.getUuid(), clonedServiceCommand);
    });

    List<ServiceTemplate> serviceTemplates = serviceTemplateService
                                                 .list(aPageRequest()
                                                           .addFilter("appId", EQ, originalService.getAppId())
                                                           .addFilter("serviceId", EQ, originalService.getUuid())
                                                           .build(),
                                                     false)
                                                 .getResponse();

    serviceTemplates.forEach(serviceTemplate -> {
      ServiceTemplate clonedServiceTemplate = serviceTemplate.clone();
      clonedServiceTemplate.setName(savedCloneService.getName());
      clonedServiceTemplate.setServiceId(savedCloneService.getUuid());
      serviceTemplateService.save(clonedServiceTemplate);
    });

    List<ArtifactStream> artifactStreams = artifactStreamService
                                               .list(aPageRequest()
                                                         .addFilter("appId", EQ, originalService.getAppId())
                                                         .addFilter("serviceId", EQ, originalService.getUuid())
                                                         .build())
                                               .getResponse();
    artifactStreams.forEach(originalArtifactStream -> {
      ArtifactStream clonedArtifactStream = originalArtifactStream.clone();
      clonedArtifactStream.setServiceId(savedCloneService.getUuid());
      artifactStreamService.create(clonedArtifactStream);
    });

    originalService.getConfigFiles().forEach(originalConfigFile -> {
      File file = configService.download(originalConfigFile.getAppId(), originalConfigFile.getUuid());
      ConfigFile clonedConfigFile = originalConfigFile.clone();
      clonedConfigFile.setEntityId(savedCloneService.getUuid());

      try {
        configService.save(clonedConfigFile, new FileInputStream(file));
      } catch (FileNotFoundException e) {
        logger.error("Error in cloning config file {}", originalConfigFile.toString());
        // Ignore and continue adding more files
      }
    });

    originalService.getServiceVariables().forEach(originalServiceVariable -> {
      serviceVariableService.getServiceVariablesForEntity(
          originalServiceVariable.getAppId(), originalServiceVariable.getTemplateId(), savedCloneService.getUuid());
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
    return addCommand(appId, serviceId, clonedServiceCommand);
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

    Command executableCommand = command;
    if (executableCommand == null) {
      return new ArrayList<>();
    }

    if (isNotBlank(command.getReferenceId())) {
      executableCommand = getCommandByNameAndVersion(
          appId, serviceId, command.getReferenceId(), commandNameVersionMap.get(command.getReferenceId()))
                              .getCommand();
      if (executableCommand == null) {
        throw new WingsException(COMMAND_DOES_NOT_EXIST);
      }
    }

    return executableCommand.getCommandUnits()
        .stream()
        .flatMap(commandUnit -> {
          if (COMMAND.equals(commandUnit.getCommandUnitType())) {
            return getFlattenCommandUnitList(appId, serviceId, commandUnit.getName(), commandNameVersionMap).stream();
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
          aServiceCommand().withTargetToAllEnv(true).withCommand(command).build());
    }

    return serviceToReturn;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Service update(Service service) {
    Service savedService = wingsPersistence.get(Service.class, service.getAppId(), service.getUuid());
    wingsPersistence.updateFields(Service.class, service.getUuid(),
        ImmutableMap.of("name", service.getName().trim(), "description", service.getDescription(), "artifactType",
            service.getArtifactType(), "appContainer", service.getAppContainer()));
    if (!savedService.getName().equals(service.getName())) {
      executorService.submit(()
                                 -> serviceTemplateService.updateDefaultServiceTemplateName(service.getAppId(),
                                     service.getUuid(), savedService.getName(), service.getName().trim()));
    }
    return wingsPersistence.get(Service.class, service.getAppId(), service.getUuid());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Service get(String appId, String serviceId) {
    return get(appId, serviceId, true);
  }

  @Override
  public Service get(String appId, String serviceId, boolean includeDetails) {
    Service service = wingsPersistence.get(Service.class, appId, serviceId);
    if (service != null && includeDetails) {
      service.setConfigFiles(configService.getConfigFilesForEntity(appId, DEFAULT_TEMPLATE_ID, service.getUuid()));
      service.setServiceVariables(
          serviceVariableService.getServiceVariablesForEntity(appId, DEFAULT_TEMPLATE_ID, service.getUuid()));
      service.setLastDeploymentActivity(activityService.getLastActivityForService(appId, serviceId));
      service.setLastProdDeploymentActivity(activityService.getLastProductionActivityForService(appId, serviceId));
      service.getServiceCommands().forEach(serviceCommand
          -> serviceCommand.setCommand(
              commandService.getCommand(appId, serviceCommand.getUuid(), serviceCommand.getDefaultVersion())));
    }
    return service;
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

    // Ensure service is safe to delete

    List<Workflow> workflows =
        workflowService.listWorkflows(aPageRequest().addFilter("appId", EQ, appId).build()).getResponse();

    List<Workflow> serviceWorkflows =
        workflows.stream()
            .filter(wfl -> wfl.getServices().stream().anyMatch(s -> serviceId.equals(s.getUuid())))
            .collect(Collectors.toList());

    if (serviceWorkflows != null && serviceWorkflows.size() > 0) {
      String workflowNames = serviceWorkflows.stream().map(Workflow::getName).collect(Collectors.joining(","));
      String message = String.format(
          "Service:[%s] couldn't be deleted. Remove Service reference from following workflows [" + workflowNames + "]",
          service.getName());
      throw new WingsException(INVALID_REQUEST, "message", message);
    } else {
      // safe to delete
      boolean deleted = wingsPersistence.delete(Service.class, serviceId);
      if (deleted) {
        executorService.submit(() -> {
          notificationService.sendNotificationAsync(
              anInformationNotification()
                  .withAppId(service.getAppId())
                  .withNotificationTemplateId(NotificationMessageType.ENTITY_DELETE_NOTIFICATION.name())
                  .withNotificationTemplateVariables(
                      ImmutableMap.of("ENTITY_TYPE", "Service", "ENTITY_NAME", service.getName()))
                  .build());
          serviceTemplateService.deleteByService(appId, serviceId);
          artifactStreamService.deleteByService(appId, serviceId);
          configService.deleteByEntityId(appId, DEFAULT_TEMPLATE_ID, serviceId);
          serviceVariableService.deleteByEntityId(appId, serviceId);
        });
      }
    }
  }

  @Override
  public void deleteByApp(String appId) {
    wingsPersistence.createQuery(Service.class)
        .field("appId")
        .equal(appId)
        .asList()
        .forEach(service -> delete(appId, service.getUuid()));
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
  public ContainerTask createContainerTask(ContainerTask containerTask) {
    boolean exist = exist(containerTask.getAppId(), containerTask.getServiceId());
    if (!exist) {
      throw new WingsException(INVALID_REQUEST, "message", "Service doesn't exists");
    }
    return wingsPersistence.saveAndGet(ContainerTask.class, containerTask);
  }

  @Override
  public void deleteContainerTask(String appId, String containerTaskId) {
    wingsPersistence.delete(ContainerTask.class, appId, containerTaskId);
  }

  @Override
  public ContainerTask updateContainerTask(ContainerTask containerTask) {
    return createContainerTask(containerTask);
  }

  @Override
  public PageResponse<ContainerTask> listContainerTasks(PageRequest<ContainerTask> pageRequest) {
    return wingsPersistence.query(ContainerTask.class, pageRequest);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Service addCommand(String appId, String serviceId, ServiceCommand serviceCommand) {
    Service service = wingsPersistence.get(Service.class, appId, serviceId);
    Validator.notNullCheck("service", service);

    if (!serviceCommand.getCommand().getGraph().isLinear()) {
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
        serviceCommand.getName(), ChangeType.CREATED, notes);

    command.transformGraph();
    command.setVersion(1L);
    command.setOriginEntityId(serviceCommand.getUuid());
    command.setAppId(appId);
    if (command.getCommandUnits() != null && command.getCommandUnits().size() > 0) {
      command.setDeploymentType(command.getCommandUnits().get(0).getDeploymentType());
    }

    commandService.save(command);

    service.getServiceCommands().add(serviceCommand);

    wingsPersistence.save(service);
    return get(appId, serviceId);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Service deleteCommand(String appId, String serviceId, String commandId) {
    Service service = wingsPersistence.get(Service.class, appId, serviceId);
    Validator.notNullCheck("service", service);

    wingsPersistence.update(
        wingsPersistence.createQuery(Service.class).field(ID_KEY).equal(serviceId).field("appId").equal(appId),
        wingsPersistence.createUpdateOperations(Service.class).removeAll("serviceCommands", commandId));

    return get(appId, serviceId);
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
      if (!serviceCommand.getCommand().getGraph().isLinear()) {
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
                serviceCommand.getName(), ChangeType.UPDATED, serviceCommand.getNotes());
        command.setVersion(Long.valueOf(entityVersion.getVersion().intValue()));
        // Copy the old command values
        command.setDeploymentType(oldCommand.getDeploymentType());
        commandService.save(command);

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
    return service.getServiceCommands()
        .stream()
        .filter(command -> equalsIgnoreCase(commandName, command.getName()))
        .findFirst()
        .orElse(null);
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
}
