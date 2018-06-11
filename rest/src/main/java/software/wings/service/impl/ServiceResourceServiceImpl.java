package software.wings.service.impl;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.data.structure.ListUtil.trimList;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Comparator.comparingDouble;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.lang3.StringUtils.equalsIgnoreCase;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.atteo.evo.inflector.English.plural;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.beans.ConfigFile.DEFAULT_TEMPLATE_ID;
import static software.wings.beans.EntityVersion.Builder.anEntityVersion;
import static software.wings.beans.InformationNotification.Builder.anInformationNotification;
import static software.wings.beans.SearchFilter.Operator.EQ;
import static software.wings.beans.ServiceVariable.Type.ENCRYPTED_TEXT;
import static software.wings.beans.Setup.SetupStatus.INCOMPLETE;
import static software.wings.beans.command.Command.Builder.aCommand;
import static software.wings.beans.command.CommandUnitType.COMMAND;
import static software.wings.beans.command.ServiceCommand.Builder.aServiceCommand;
import static software.wings.dl.MongoHelper.setUnset;
import static software.wings.dl.PageRequest.PageRequestBuilder.aPageRequest;
import static software.wings.dl.PageRequest.UNLIMITED;
import static software.wings.exception.WingsException.USER;
import static software.wings.yaml.YamlHelper.trimYaml;

import com.google.common.base.Joiner;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import de.danielbechler.diff.ObjectDifferBuilder;
import de.danielbechler.diff.node.DiffNode;
import de.danielbechler.diff.path.NodePath;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.harness.data.validator.EntityNameValidator;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.query.Sort;
import org.mongodb.morphia.query.UpdateOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.vyarus.guice.validator.group.annotation.ValidationGroups;
import software.wings.beans.Activity;
import software.wings.beans.AppContainer;
import software.wings.beans.CanaryOrchestrationWorkflow;
import software.wings.beans.CommandCategory;
import software.wings.beans.ConfigFile;
import software.wings.beans.EntityType;
import software.wings.beans.EntityVersion;
import software.wings.beans.ErrorCode;
import software.wings.beans.GraphNode;
import software.wings.beans.InfrastructureProvisioner;
import software.wings.beans.LambdaSpecification;
import software.wings.beans.LambdaSpecification.FunctionSpecification;
import software.wings.beans.OrchestrationWorkflow;
import software.wings.beans.PhaseStep;
import software.wings.beans.Service;
import software.wings.beans.ServiceTemplate;
import software.wings.beans.ServiceVariable;
import software.wings.beans.Setup.SetupStatus;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowPhase;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.command.AmiCommandUnit;
import software.wings.beans.command.AwsLambdaCommandUnit;
import software.wings.beans.command.CodeDeployCommandUnit;
import software.wings.beans.command.Command;
import software.wings.beans.command.CommandUnit;
import software.wings.beans.command.CommandUnitType;
import software.wings.beans.command.ServiceCommand;
import software.wings.beans.container.ContainerTask;
import software.wings.beans.container.ContainerTaskType;
import software.wings.beans.container.HelmChartSpecification;
import software.wings.beans.container.KubernetesPayload;
import software.wings.beans.container.PcfServiceSpecification;
import software.wings.beans.container.UserDataSpecification;
import software.wings.beans.yaml.Change.ChangeType;
import software.wings.common.NotificationMessageResolver.NotificationMessageType;
import software.wings.dl.HIterator;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.exception.InvalidRequestException;
import software.wings.exception.WingsException;
import software.wings.scheduler.PruneEntityJob;
import software.wings.scheduler.QuartzScheduler;
import software.wings.service.ServiceHelper;
import software.wings.service.impl.command.CommandHelper;
import software.wings.service.impl.yaml.YamlChangeSetHelper;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.CommandService;
import software.wings.service.intfc.ConfigService;
import software.wings.service.intfc.EntityVersionService;
import software.wings.service.intfc.InfrastructureProvisionerService;
import software.wings.service.intfc.NotificationService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.ServiceTemplateService;
import software.wings.service.intfc.ServiceVariableService;
import software.wings.service.intfc.SetupService;
import software.wings.service.intfc.TriggerService;
import software.wings.service.intfc.WorkflowService;
import software.wings.service.intfc.ownership.OwnedByService;
import software.wings.sm.ContextElement;
import software.wings.sm.ExecutionStatus;
import software.wings.stencils.DataProvider;
import software.wings.stencils.Stencil;
import software.wings.stencils.StencilCategory;
import software.wings.stencils.StencilPostProcessor;
import software.wings.utils.ArtifactType;
import software.wings.utils.BoundedInputStream;
import software.wings.utils.Validator;
import software.wings.utils.validation.Create;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;
import java.util.function.Predicate;
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
  @Inject private AppService appService;
  @Inject private ArtifactService artifactService;
  @Inject private ArtifactStreamService artifactStreamService;
  @Inject private CommandService commandService;
  @Inject private ConfigService configService;
  @Inject private EntityVersionService entityVersionService;
  @Inject private ExecutorService executorService;
  @Inject private NotificationService notificationService;
  @Inject private ServiceTemplateService serviceTemplateService;
  @Inject private ServiceVariableService serviceVariableService;
  @Inject private InfrastructureProvisionerService infrastructureProvisionerService;
  @Inject private SetupService setupService;
  @Inject private TriggerService triggerService;
  @Inject private WorkflowService workflowService;
  @Inject private YamlChangeSetHelper yamlChangeSetHelper;
  @Inject private StencilPostProcessor stencilPostProcessor;
  @Inject private ServiceHelper serviceHelper;
  @Inject @Named("JobScheduler") private QuartzScheduler jobScheduler;
  @Inject private CommandHelper commandHelper;

  /**
   * {@inheritDoc}
   */
  @Override
  public PageResponse<Service> list(
      PageRequest<Service> request, boolean withBuildSource, boolean withServiceCommands) {
    PageResponse<Service> pageResponse = wingsPersistence.query(Service.class, request);
    List<Service> services = pageResponse.getResponse();
    if (withServiceCommands) {
      setServiceCommands(services);
    }
    if (withBuildSource) {
      setArtifactStreams(services);
    }
    return pageResponse;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @ValidationGroups(Create.class)
  public Service save(Service service) {
    return save(service, false, true);
  }

  @Override
  @ValidationGroups(Create.class)
  public Service save(Service service, boolean createdFromYaml, boolean createDefaultCommands) {
    setKeyWords(service);
    Service savedService =
        Validator.duplicateCheck(() -> wingsPersistence.saveAndGet(Service.class, service), "name", service.getName());
    if (createDefaultCommands && !skipDefaultCommands(service)) {
      savedService = addDefaultCommands(savedService, !createdFromYaml);
    }

    serviceTemplateService.createDefaultTemplatesByService(savedService);
    notificationService.sendNotificationAsync(
        anInformationNotification()
            .withAppId(savedService.getAppId())
            .withNotificationTemplateId(NotificationMessageType.ENTITY_CREATE_NOTIFICATION.name())
            .withNotificationTemplateVariables(
                ImmutableMap.of("ENTITY_TYPE", "Service", "ENTITY_NAME", savedService.getName()))
            .build());

    if (!createdFromYaml) {
      yamlChangeSetHelper.serviceYamlChangeAsync(savedService, ChangeType.ADD);
    }

    return savedService;
  }

  private boolean skipDefaultCommands(Service service) {
    if (ArtifactType.PCF.equals(service.getArtifactType())) {
      return true;
    }

    return false;
  }

  @Override
  public Service clone(String appId, String originalServiceId, Service service) {
    Service originalService = get(appId, originalServiceId);
    Service clonedService = originalService.cloneInternal();
    clonedService.setName(service.getName());
    clonedService.setDescription(service.getDescription());
    setKeyWords(clonedService);
    Service savedCloneService = Validator.duplicateCheck(
        () -> wingsPersistence.saveAndGet(Service.class, clonedService), "name", service.getName());

    boolean shouldPushToYaml = !hasInternalCommands(originalService);
    originalService.getServiceCommands().forEach(serviceCommand -> {
      ServiceCommand clonedServiceCommand = serviceCommand.cloneInternal();
      addCommand(savedCloneService.getAppId(), savedCloneService.getUuid(), clonedServiceCommand, shouldPushToYaml);
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
      ServiceTemplate clonedServiceTemplate = serviceTemplate.cloneInternal();
      clonedServiceTemplate.setName(savedCloneService.getName());
      clonedServiceTemplate.setServiceId(savedCloneService.getUuid());
      serviceTemplateService.save(clonedServiceTemplate);
    });

    originalService.getConfigFiles().forEach(originalConfigFile -> {
      try {
        File file = configService.download(originalConfigFile.getAppId(), originalConfigFile.getUuid());
        ConfigFile clonedConfigFile = originalConfigFile.cloneInternal();
        clonedConfigFile.setEntityId(savedCloneService.getUuid());
        configService.save(clonedConfigFile, new BoundedInputStream(new FileInputStream(file)));
      } catch (FileNotFoundException e) {
        logger.error("Error in cloning config file " + originalConfigFile.toString(), e);
        // Ignore and continue adding more files
      }
    });

    originalService.getServiceVariables().forEach(originalServiceVariable -> {
      ServiceVariable clonedServiceVariable = originalServiceVariable.cloneInternal();
      if (ENCRYPTED_TEXT.equals(clonedServiceVariable.getType())) {
        clonedServiceVariable.setValue(clonedServiceVariable.getEncryptedValue().toCharArray());
      }
      clonedServiceVariable.setEntityId(savedCloneService.getUuid());
      serviceVariableService.save(clonedServiceVariable);
    });
    return savedCloneService;
  }

  private void setKeyWords(Service clonedService) {
    clonedService.setKeywords(trimList(clonedService.generateKeywords()));
  }

  @Override
  public Service cloneCommand(String appId, String serviceId, String commandName, ServiceCommand command) {
    // don't allow cloning of Docker commands
    Service service = getServiceWithServiceCommands(appId, serviceId);
    if (service.getArtifactType().equals(ArtifactType.DOCKER)) {
      throw new InvalidRequestException("Docker commands can not be cloned");
    }
    ServiceCommand oldServiceCommand = service.getServiceCommands()
                                           .stream()
                                           .filter(cmd -> equalsIgnoreCase(commandName, cmd.getName()))
                                           .findFirst()
                                           .orElse(null);
    ServiceCommand clonedServiceCommand = oldServiceCommand.cloneInternal();
    clonedServiceCommand.setName(command.getName());
    return addCommand(appId, serviceId, clonedServiceCommand, true);
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

  @Override
  public boolean hasInternalCommands(Service service) {
    boolean isInternal = false;
    ArtifactType artifactType = service.getArtifactType();
    AppContainer appContainer = service.getAppContainer();
    if (appContainer != null && appContainer.getFamily() != null) {
      isInternal = appContainer.getFamily().isInternal();
    } else if (artifactType != null) {
      isInternal = artifactType.isInternal();
    }
    return isInternal;
  }

  private Service addDefaultCommands(Service service, boolean pushToYaml) {
    List<Command> commands = emptyList();
    ArtifactType artifactType = service.getArtifactType();
    AppContainer appContainer = service.getAppContainer();
    if (appContainer != null && appContainer.getFamily() != null) {
      commands = appContainer.getFamily().getDefaultCommands(artifactType, appContainer);
    } else if (artifactType != null) {
      commands = artifactType.getDefaultCommands();
    }

    // Default Commands are pushed to yaml only if it matches both the conditions
    // 1) pushToYaml is true 2) commands are not internal. (Check hasInternalCommands()).
    boolean shouldPushCommandsToYaml = pushToYaml && !hasInternalCommands(service);

    Service serviceToReturn = service;
    for (Command command : commands) {
      serviceToReturn = addCommand(service.getAppId(), service.getUuid(),
          aServiceCommand().withTargetToAllEnv(true).withCommand(command).build(), shouldPushCommandsToYaml);
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

    List<String> keywords = trimList(service.generateKeywords());
    UpdateOperations<Service> updateOperations =
        wingsPersistence.createUpdateOperations(Service.class)
            .set("name", service.getName().trim())
            .set("description", Optional.ofNullable(service.getDescription()).orElse(""))
            .set("keywords", keywords);

    if (isNotBlank(service.getConfigMapYaml())) {
      updateOperations.set("configMapYaml", service.getConfigMapYaml());
    } else {
      updateOperations.unset("configMapYaml");
    }

    if (isNotBlank(service.getHelmValueYaml())) {
      updateOperations.set("helmValueYaml", service.getHelmValueYaml());
    } else {
      updateOperations.unset("helmValueYaml");
    }

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
    return getServiceByName(appId, serviceName, true);
  }

  @Override
  public Service getServiceByName(String appId, String serviceName, boolean withDetails) {
    Service service =
        wingsPersistence.createQuery(Service.class).filter("appId", appId).filter("name", serviceName).get();
    if (service != null) {
      if (withDetails) {
        setServiceDetails(service, appId);
      }
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
    return wingsPersistence.createQuery(Service.class).filter("appId", appId).filter(ID_KEY, serviceId).getKey()
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
    PruneEntityJob.addDefaultJob(
        jobScheduler, Service.class, service.getAppId(), service.getUuid(), Duration.ofSeconds(5));

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
    executorService.submit(() -> deleteCommands(appId, serviceId));

    List<OwnedByService> services =
        ServiceClassLocator.descendingServices(this, ServiceResourceServiceImpl.class, OwnedByService.class);
    PruneEntityJob.pruneDescendingEntities(services, descending -> descending.pruneByService(appId, serviceId));
  }

  private void ensureServiceSafeToDelete(Service service) {
    List<Workflow> workflows =
        workflowService
            .listWorkflows(aPageRequest().withLimit(UNLIMITED).addFilter("appId", EQ, service.getAppId()).build())
            .getResponse();

    List<Workflow> serviceWorkflows =
        workflows.stream()
            .filter(wfl -> wfl.getServices().stream().anyMatch(s -> service.getUuid().equals(s.getUuid())))
            .collect(toList());

    if (isNotEmpty(serviceWorkflows)) {
      String workflowNames = serviceWorkflows.stream().map(Workflow::getName).collect(joining(","));
      throw new InvalidRequestException(
          format("Service [%s] couldn't be deleted. Remove Service reference from the following "
                  + plural("workflow", serviceWorkflows.size()) + " [" + workflowNames + "]",
              service.getName()),
          USER);
    }

    List<InfrastructureProvisioner> provisioners = infrastructureProvisionerService.listByBlueprintDetails(
        service.getAppId(), null, service.getUuid(), null, null);

    if (isNotEmpty(provisioners)) {
      String infrastructureProvisionerNames =
          provisioners.stream().map(InfrastructureProvisioner::getName).collect(joining(","));
      throw new InvalidRequestException(
          format("Service [%s] couldn't be deleted. Remove Service reference from the following "
                  + plural("infrastructure provisioner", provisioners.size()) + " [" + infrastructureProvisionerNames
                  + "] ",
              service.getName()),
          USER);
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
      boolean deleted = wingsPersistence.delete(wingsPersistence.createQuery(Command.class)
                                                    .filter("appId", service.getAppId())
                                                    .filter("originEntityId", serviceCommand.getUuid()));
      if (deleted) {
        String accountId = appService.getAccountIdByAppId(service.getAppId());
        yamlChangeSetHelper.commandFileChange(accountId, service, serviceCommand, ChangeType.DELETE);
      }
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
            for (GraphNode step : phaseStep.getSteps()) {
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
      String message = format(
          "Command [%s] couldn't be deleted. Remove reference from the following workflows [" + sb.toString() + "]",
          serviceCommand.getName());
      throw new InvalidRequestException(message, USER);
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
    PageRequest<Service> pageRequest = aPageRequest().addFilter("appId", EQ, appId).build();
    return wingsPersistence.query(Service.class, pageRequest).getResponse();
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
      throw new InvalidRequestException("Service doesn't exist");
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
      String appId, String serviceId, String taskId, KubernetesPayload kubernetesPayload, boolean reset) {
    ContainerTask containerTask = wingsPersistence.createQuery(ContainerTask.class)
                                      .filter("appId", appId)
                                      .filter("serviceId", serviceId)
                                      .filter(ID_KEY, taskId)
                                      .get();
    if (reset) {
      containerTask.convertFromAdvanced();
    } else {
      containerTask.setAdvancedConfig(kubernetesPayload.getAdvancedConfig());
      containerTask.validateAdvanced();
    }
    return upsertContainerTask(containerTask, false, false);
  }

  @Override
  public PageResponse<ContainerTask> listContainerTasks(PageRequest<ContainerTask> pageRequest) {
    return wingsPersistence.query(ContainerTask.class, pageRequest);
  }

  @Override
  public HelmChartSpecification createHelmChartSpecification(HelmChartSpecification helmChartSpecification) {
    return upsertHelmChartSpecification(helmChartSpecification, true);
  }

  private HelmChartSpecification upsertHelmChartSpecification(
      HelmChartSpecification helmChartSpecification, boolean isCreate) {
    boolean exist = exist(helmChartSpecification.getAppId(), helmChartSpecification.getServiceId());
    if (!exist) {
      throw new InvalidRequestException("Service doesn't exist");
    }
    HelmChartSpecification persistedHelmChartSpecification =
        wingsPersistence.saveAndGet(HelmChartSpecification.class, helmChartSpecification);

    String appId = persistedHelmChartSpecification.getAppId();
    String accountId = appService.getAccountIdByAppId(appId);
    Service service = get(appId, persistedHelmChartSpecification.getServiceId());

    if (isCreate) {
      yamlChangeSetHelper.helmChartSpecificationYamlChangeAsync(
          accountId, service, persistedHelmChartSpecification, ChangeType.ADD);
    } else {
      yamlChangeSetHelper.helmChartSpecificationYamlChangeAsync(
          accountId, service, persistedHelmChartSpecification, ChangeType.MODIFY);
    }

    return persistedHelmChartSpecification;
  }

  @Override
  public void deleteHelmChartSpecification(String appId, String helmChartSpecificationId) {
    wingsPersistence.delete(HelmChartSpecification.class, appId, helmChartSpecificationId);
  }

  @Override
  public HelmChartSpecification updateHelmChartSpecification(HelmChartSpecification helmChartSpecification) {
    return upsertHelmChartSpecification(helmChartSpecification, false);
  }

  @Override
  public PageResponse<HelmChartSpecification> listHelmChartSpecifications(
      PageRequest<HelmChartSpecification> pageRequest) {
    return wingsPersistence.query(HelmChartSpecification.class, pageRequest);
  }

  @Override
  public PcfServiceSpecification createPcfServiceSpecification(PcfServiceSpecification pcfServiceSpecification) {
    return upsertPcfServiceSpecification(pcfServiceSpecification, true);
  }

  private PcfServiceSpecification upsertPcfServiceSpecification(
      PcfServiceSpecification pcfServiceSpecification, boolean isCreate) {
    boolean exist = exist(pcfServiceSpecification.getAppId(), pcfServiceSpecification.getServiceId());
    if (!exist) {
      throw new InvalidRequestException("Service doesn't exist");
    }

    serviceHelper.addPlaceholderTexts(pcfServiceSpecification);
    PcfServiceSpecification persistedPcfServiceSpecification =
        wingsPersistence.saveAndGet(PcfServiceSpecification.class, pcfServiceSpecification);

    String appId = persistedPcfServiceSpecification.getAppId();
    String accountId = appService.getAccountIdByAppId(appId);
    Service service = get(appId, persistedPcfServiceSpecification.getServiceId());

    if (isCreate) {
      yamlChangeSetHelper.pcfServiceSpecificationYamlChangeAsync(
          accountId, service, persistedPcfServiceSpecification, ChangeType.ADD);
    } else {
      yamlChangeSetHelper.pcfServiceSpecificationYamlChangeAsync(
          accountId, service, persistedPcfServiceSpecification, ChangeType.MODIFY);
    }

    return persistedPcfServiceSpecification;
  }

  @Override
  public void deletePCFServiceSpecification(String appId, String pCFServiceSpecificationId) {
    wingsPersistence.delete(PcfServiceSpecification.class, appId, pCFServiceSpecificationId);
  }

  @Override
  public PcfServiceSpecification updatePcfServiceSpecification(PcfServiceSpecification pcfServiceSpecification) {
    return upsertPcfServiceSpecification(pcfServiceSpecification, false);
  }

  @Override
  public PcfServiceSpecification resetToDefaultPcfServiceSpecification(
      PcfServiceSpecification pcfServiceSpecification) {
    boolean exist = exist(pcfServiceSpecification.getAppId(), pcfServiceSpecification.getServiceId());
    if (!exist) {
      throw new InvalidRequestException("Service doesn't exist");
    }
    pcfServiceSpecification.resetToDefaultManifestSpecification();
    return upsertPcfServiceSpecification(pcfServiceSpecification, false);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Service addCommand(String appId, String serviceId, ServiceCommand serviceCommand, boolean pushToYaml) {
    Service service = wingsPersistence.get(Service.class, appId, serviceId);
    Validator.notNullCheck("service", service);

    validateCommandName(serviceCommand.getCommand());
    addServiceCommand(appId, serviceId, serviceCommand, pushToYaml, service);

    wingsPersistence.save(service);
    return get(appId, serviceId);
  }

  @Override
  public Service updateCommandsOrder(String appId, String serviceId, List<ServiceCommand> serviceCommands) {
    Service service = wingsPersistence.get(Service.class, appId, serviceId);
    Validator.notNullCheck("service", service);
    if (isEmpty(serviceCommands)) {
      return service;
    }
    // Get the old service commands
    UpdateOperations<ServiceCommand> updateOperation = wingsPersistence.createUpdateOperations(ServiceCommand.class);

    double i = 0.0;
    for (ServiceCommand serviceCommand : serviceCommands) {
      setUnset(updateOperation, "order", i++);
      wingsPersistence.update(
          wingsPersistence.createQuery(ServiceCommand.class).filter(ID_KEY, serviceCommand.getUuid()), updateOperation);
    }
    return get(appId, serviceId);
  }

  private void validateCommandName(Command command) {
    if (command != null) {
      if (!EntityNameValidator.isValid(command.getName())) {
        throw new InvalidRequestException("Command Name can only have characters -, _, a-z, A-Z, 0-9 and space");
      }
    }
  }

  private void addServiceCommand(
      String appId, String serviceId, ServiceCommand serviceCommand, boolean pushToYaml, Service service) {
    serviceCommand.setDefaultVersion(1);
    serviceCommand.setServiceId(serviceId);
    serviceCommand.setAppId(appId);
    String notes = serviceCommand.getNotes();
    Command command = serviceCommand.getCommand();
    if (serviceCommand.getCommand().getGraph() != null) {
      if (!isLinearCommandGraph(serviceCommand)) {
        WingsException wingsException =
            new WingsException(ErrorCode.INVALID_PIPELINE, new IllegalArgumentException("Graph is not a pipeline"));
        wingsException.addParam("message", "Graph is not a linear pipeline");
        throw wingsException;
      }
      serviceCommand.setName(serviceCommand.getCommand().getGraph().getGraphName());
      command.transformGraph();
    } else {
      if (isEmpty(serviceCommand.getName())) {
        serviceCommand.setName(serviceCommand.getCommand().getName());
      }
    }

    serviceCommand = wingsPersistence.saveAndGet(ServiceCommand.class, serviceCommand);
    entityVersionService.newEntityVersion(appId, EntityType.COMMAND, serviceCommand.getUuid(), serviceId,
        serviceCommand.getName(), EntityVersion.ChangeType.CREATED, notes);

    command.setVersion(1L);
    command.setOriginEntityId(serviceCommand.getUuid());
    command.setAppId(appId);
    if (isNotEmpty(command.getCommandUnits())) {
      command.setDeploymentType(command.getCommandUnits().get(0).getDeploymentType());
    }
    // TODO: Set the graph to null after backward compatible change
    commandService.save(command, pushToYaml);
    service.getServiceCommands().add(serviceCommand);
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

    EntityVersion lastEntityVersion =
        entityVersionService.lastEntityVersion(appId, EntityType.COMMAND, serviceCommand.getUuid(), serviceId);

    if (serviceCommand.getCommand() != null) {
      validateCommandName(serviceCommand.getCommand());
      updateCommandInternal(appId, serviceId, serviceCommand, lastEntityVersion, false);
    }

    setUnset(updateOperation, "envIdVersionMap", serviceCommand.getEnvIdVersionMap());
    if (serviceCommand.getDefaultVersion() != null) {
      updateOperation.set("defaultVersion", serviceCommand.getDefaultVersion());
    }
    if (serviceCommand.getName() != null) {
      updateOperation.set("name", serviceCommand.getName());
    }

    String accountId = appService.getAccountIdByAppId(appId);
    wingsPersistence.update(
        wingsPersistence.createQuery(ServiceCommand.class).filter(ID_KEY, serviceCommand.getUuid()), updateOperation);
    // Fetching the service command from db just to make sure it has the latest info since multiple update operations
    // were performed.
    serviceCommand = commandService.getServiceCommand(appId, serviceCommand.getUuid());
    yamlChangeSetHelper.commandFileChangeAsync(accountId, service, serviceCommand, ChangeType.MODIFY);

    return get(appId, serviceId);
  }

  private void updateCommandInternal(String appId, String serviceId, ServiceCommand serviceCommand,
      EntityVersion lastEntityVersion, boolean pushToYaml) {
    Command command = aCommand().build();
    if (serviceCommand.getCommand().getGraph() != null) {
      if (!isLinearCommandGraph(serviceCommand)) {
        WingsException wingsException =
            new WingsException(ErrorCode.INVALID_PIPELINE, new IllegalArgumentException("Graph is not a pipeline"));
        wingsException.addParam("message", "Graph is not a linear pipeline");
        throw wingsException;
      }
      command.setGraph(serviceCommand.getCommand().getGraph());
      command.transformGraph();
    } else {
      command.setCommandUnits(serviceCommand.getCommand().getCommandUnits());
      command.setName(serviceCommand.getCommand().getName());
      command.setCommandType(serviceCommand.getCommand().getCommandType());
      if (isEmpty(serviceCommand.getName())) {
        serviceCommand.setName(serviceCommand.getCommand().getName());
      }
    }
    command.setOriginEntityId(serviceCommand.getUuid());
    command.setAppId(appId);
    command.setUuid(null);

    Command oldCommand = commandService.getCommand(appId, serviceCommand.getUuid(), lastEntityVersion.getVersion());

    DiffNode commandUnitDiff =
        ObjectDifferBuilder.buildDefault().compare(command.getCommandUnits(), oldCommand.getCommandUnits());

    if (commandUnitDiff.hasChanges()
        || isCommandUnitsOrderChanged(command.getCommandUnits(), oldCommand.getCommandUnits())) {
      EntityVersion entityVersion =
          entityVersionService.newEntityVersion(appId, EntityType.COMMAND, serviceCommand.getUuid(), serviceId,
              serviceCommand.getName(), EntityVersion.ChangeType.UPDATED, serviceCommand.getNotes());
      command.setVersion(Long.valueOf(entityVersion.getVersion().intValue()));
      if (command.getDeploymentType() == null) {
        // Copy the old command values
        command.setDeploymentType(oldCommand.getDeploymentType());
      }

      if (command.getCommandType() == null) {
        command.setCommandType(oldCommand.getCommandType());
      } else {
        command.setCommandType(serviceCommand.getCommand().getCommandType());
      }

      if (command.getArtifactType() == null) {
        command.setArtifactType(oldCommand.getArtifactType());
      }
      commandService.save(command, pushToYaml);

      if (serviceCommand.getSetAsDefault()) {
        serviceCommand.setDefaultVersion(entityVersion.getVersion());
      }
    } else {
      if (serviceCommand.getCommand().getGraph() != null) {
        ObjectDifferBuilder builder = ObjectDifferBuilder.startBuilding();
        builder.inclusion().exclude().node(NodePath.with("linearGraphIterator"));
        DiffNode graphDiff = builder.build().compare(command.getGraph(), oldCommand.getGraph());
        if (graphDiff.hasChanges()) {
          oldCommand.setGraph(command.getGraph());
          commandService.update(oldCommand, pushToYaml);
        }
      } else {
        // Check if Name and CommandType changes
        if (!oldCommand.getName().equals(command.getName())
            || !oldCommand.getCommandType().equals(command.getCommandType())) {
          UpdateOperations<Command> commandUpdateOperations = wingsPersistence.createUpdateOperations(Command.class);
          setUnset(commandUpdateOperations, "name", command.getName());
          setUnset(commandUpdateOperations, "commandType", command.getCommandType());
          wingsPersistence.update(wingsPersistence.createQuery(Command.class).filter(ID_KEY, oldCommand.getUuid()),
              commandUpdateOperations);
        }
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  @SuppressFBWarnings("RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE")
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
        .filter("appId", appId)
        .filter("serviceId", serviceId)
        .get();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<Stencil> getCommandStencils(@NotEmpty String appId, @NotEmpty String serviceId, String commandName) {
    return stencilPostProcessor.postProcess(
        asList(CommandUnitType.values()), appId, getEntityMap(serviceId, commandName));
  }

  private Map<String, String> getEntityMap(@NotEmpty String serviceId, String commandName) {
    Map<String, String> map = Maps.newHashMap();

    if (isNotEmpty(serviceId)) {
      map.put(EntityType.SERVICE.name(), serviceId);
    }

    if (isNotEmpty(commandName)) {
      map.put(EntityType.COMMAND.name(), commandName);
    }
    return map;
  }

  @Override
  public List<Stencil> getCommandStencils(
      String appId, String serviceId, String commandName, boolean onlyScriptCommands) {
    List<Stencil> stencils =
        stencilPostProcessor.postProcess(asList(CommandUnitType.values()), appId, getEntityMap(serviceId, commandName));
    if (onlyScriptCommands) {
      // Suppress Container commands
      Predicate<Stencil> predicate = stencil -> stencil.getStencilCategory() != StencilCategory.CONTAINERS;
      stencils = stencils.stream().filter(predicate).collect(toList());
      // Suppress CodeDeployCommands
      predicate = stencil
          -> !stencil.getTypeClass().isAssignableFrom(CodeDeployCommandUnit.class)
          && !stencil.getTypeClass().isAssignableFrom(AwsLambdaCommandUnit.class)
          && !stencil.getTypeClass().isAssignableFrom(AmiCommandUnit.class);
      stencils = stencils.stream().filter(predicate).collect(toList());
    }
    return stencils;
  }

  @Override
  public List<Stencil> getContainerTaskStencils(@NotEmpty String appId, @NotEmpty String serviceId) {
    return stencilPostProcessor.postProcess(asList(ContainerTaskType.values()), appId, getEntityMap(serviceId, null));
  }

  @Override
  public ContainerTask getContainerTaskByDeploymentType(String appId, String serviceId, String deploymentType) {
    return wingsPersistence.createQuery(ContainerTask.class)
        .filter("appId", appId)
        .filter("serviceId", serviceId)
        .filter("deploymentType", deploymentType)
        .get();
  }

  @Override
  public ContainerTask getContainerTaskById(String appId, String containerTaskId) {
    return wingsPersistence.get(ContainerTask.class, appId, containerTaskId);
  }

  @Override
  public HelmChartSpecification getHelmChartSpecification(String appId, String serviceId) {
    return wingsPersistence.createQuery(HelmChartSpecification.class)
        .filter("appId", appId)
        .filter("serviceId", serviceId)
        .get();
  }

  @Override
  public PcfServiceSpecification getPcfServiceSpecification(String appId, String serviceId) {
    return wingsPersistence.createQuery(PcfServiceSpecification.class)
        .filter("appId", appId)
        .filter("serviceId", serviceId)
        .get();
  }

  @Override
  public PcfServiceSpecification getExistingOrDefaultPcfServiceSpecification(String appId, String serviceId) {
    PcfServiceSpecification pcfServiceSpecification = getPcfServiceSpecification(appId, serviceId);
    if (pcfServiceSpecification == null) {
      pcfServiceSpecification = PcfServiceSpecification.builder().serviceId(serviceId).build();
      pcfServiceSpecification.setAppId(appId);
      pcfServiceSpecification.resetToDefaultManifestSpecification();
    }

    return pcfServiceSpecification;
  }

  @Override
  public HelmChartSpecification getHelmChartSpecificationById(String appId, String helmChartSpecificationId) {
    return wingsPersistence.get(HelmChartSpecification.class, appId, helmChartSpecificationId);
  }

  @Override
  public PcfServiceSpecification getPcfServiceSpecificationById(String appId, String pcfServiceSpecificationId) {
    return wingsPersistence.get(PcfServiceSpecification.class, appId, pcfServiceSpecificationId);
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
  public Map<String, String> getData(String appId, Map<String, String> params) {
    Service service = wingsPersistence.get(Service.class, appId, params.get(EntityType.SERVICE.name()));
    if (service == null) {
      return emptyMap();
    }

    List<ServiceCommand> serviceCommands = getServiceCommands(service.getAppId(), service.getUuid(), false);
    if (isEmpty(serviceCommands)) {
      return emptyMap();
    }

    return serviceCommands.stream()
        .filter(serviceCommand -> !StringUtils.equals(serviceCommand.getName(), params.get(EntityType.COMMAND.name())))
        .collect(toMap(ServiceCommand::getName, ServiceCommand::getName));
  }

  @Override
  public LambdaSpecification createLambdaSpecification(LambdaSpecification lambdaSpecification) {
    return upsertLambdaSpecification(lambdaSpecification, true);
  }

  private void validateLambdaSpecification(LambdaSpecification lambdaSpecification) {
    List<String> duplicateFunctionName =
        getFunctionAttributeDuplicateValues(lambdaSpecification, FunctionSpecification::getFunctionName);
    if (isNotEmpty(duplicateFunctionName)) {
      throw new InvalidRequestException("Function name should be unique. Duplicate function names: ["
          + Joiner.on(",").join(duplicateFunctionName) + "]");
    }

    /** Removed validation to check for duplicate handler names as part of HAR-3209 */
  }

  private List<String> getFunctionAttributeDuplicateValues(
      LambdaSpecification lambdaSpecification, Function<FunctionSpecification, String> getAttributeValue) {
    Map<String, Long> valueCountMap = lambdaSpecification.getFunctions().stream().collect(
        Collectors.groupingBy(getAttributeValue, Collectors.counting()));
    return valueCountMap.entrySet()
        .stream()
        .filter(stringLongEntry -> stringLongEntry.getValue() > 1)
        .map(Entry::getKey)
        .collect(toList());
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
        .filter("appId", appId)
        .filter("serviceId", serviceId)
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
    return getServiceCommandsByOrder(serviceCommands);
  }

  @Override
  public Service setConfigMapYaml(String appId, String serviceId, KubernetesPayload kubernetesPayload) {
    Service savedService = get(appId, serviceId, false);
    Validator.notNullCheck("Service", savedService);

    String configMapYaml = trimYaml(kubernetesPayload.getAdvancedConfig());
    UpdateOperations<Service> updateOperations;
    if (isNotBlank(configMapYaml)) {
      updateOperations = wingsPersistence.createUpdateOperations(Service.class).set("configMapYaml", configMapYaml);
    } else {
      updateOperations = wingsPersistence.createUpdateOperations(Service.class).unset("configMapYaml");
    }

    wingsPersistence.update(savedService, updateOperations);

    return get(appId, serviceId, false);
  }

  @Override
  public Service setHelmValueYaml(String appId, String serviceId, KubernetesPayload kubernetesPayload) {
    Service savedService = get(appId, serviceId, false);
    Validator.notNullCheck("Service", savedService);

    String helmValueYaml = trimYaml(kubernetesPayload.getAdvancedConfig());
    UpdateOperations<Service> updateOperations;
    if (isNotBlank(helmValueYaml)) {
      updateOperations = wingsPersistence.createUpdateOperations(Service.class).set("helmValueYaml", helmValueYaml);
    } else {
      updateOperations = wingsPersistence.createUpdateOperations(Service.class).unset("helmValueYaml");
    }

    wingsPersistence.update(savedService, updateOperations);

    return get(appId, serviceId, false);
  }

  @Override
  public List<Service> getServicesByUuids(String appId, List<String> serviceUuids) {
    if (isNotEmpty(serviceUuids)) {
      return wingsPersistence.createQuery(Service.class)
          .project("appContainer", false)
          .filter("appId", appId)
          .field("uuid")
          .in(serviceUuids)
          .asList();
    }
    return new ArrayList<>();
  }

  @Override
  public Artifact findPreviousArtifact(String appId, String workflowExecutionId, ContextElement instanceElement) {
    final Activity activity = wingsPersistence.createQuery(Activity.class)
                                  .filter(Activity.APP_ID_KEY, appId)
                                  .filter(Activity.SERVICE_INSTANCE_ID_KEY, instanceElement.getUuid())
                                  .filter(Activity.STATUS_KEY, ExecutionStatus.SUCCESS)
                                  .field(Activity.WORKFLOW_EXECUTION_ID_KEY)
                                  .notEqual(workflowExecutionId)
                                  .field(Activity.ARTIFACT_ID_KEY)
                                  .exists()
                                  .order(Sort.descending(Activity.CREATED_AT_KEY))
                                  .get();

    if (activity == null) {
      return null;
    }
    return artifactService.get(appId, activity.getArtifactId());
  }

  private boolean isCommandUnitsOrderChanged(List<CommandUnit> commandUnits, List<CommandUnit> oldCommandUnits) {
    if (commandUnits != null && oldCommandUnits != null) {
      if (commandUnits.size() == oldCommandUnits.size()) {
        List<String> commandNames = commandUnits.stream().map(commandUnit -> commandUnit.getName()).collect(toList());
        List<String> oldCommandNames =
            oldCommandUnits.stream().map(oldCommandUnit -> oldCommandUnit.getName()).collect(toList());
        return !commandNames.equals(oldCommandNames);
      }
    }
    return false;
  }

  public void setArtifactStreams(List<Service> services) {
    List<String> serviceIds = services.stream().map(service -> service.getUuid()).collect(toList());
    ArrayListMultimap<String, ArtifactStream> serviceToArtifactStreamMap = ArrayListMultimap.create();

    try (HIterator<ArtifactStream> iterator = new HIterator<>(
             wingsPersistence.createQuery(ArtifactStream.class).field("serviceId").in(serviceIds).fetch())) {
      while (iterator.hasNext()) {
        ArtifactStream artifactStream = iterator.next();
        serviceToArtifactStreamMap.put(artifactStream.getServiceId(), artifactStream);
      }
    }
    services.forEach(service -> service.setArtifactStreams(serviceToArtifactStreamMap.get(service.getUuid())));
  }

  public void setServiceCommands(List<Service> services) {
    List<String> serviceIds = services.stream().map(service -> service.getUuid()).collect(toList());
    ArrayListMultimap<String, ServiceCommand> serviceToServiceCommandMap = ArrayListMultimap.create();
    try (HIterator<ServiceCommand> iterator = new HIterator<>(
             wingsPersistence.createQuery(ServiceCommand.class).field("serviceId").in(serviceIds).fetch())) {
      while (iterator.hasNext()) {
        ServiceCommand serviceCommand = iterator.next();
        serviceToServiceCommandMap.put(serviceCommand.getServiceId(), serviceCommand);
      }
    }
    services.forEach((Service service) -> {
      try {
        List<ServiceCommand> serviceCommands = serviceToServiceCommandMap.get(service.getUuid());
        if (serviceCommands != null) {
          serviceCommands = getServiceCommandsByOrder(serviceCommands);

          serviceCommands.forEach((ServiceCommand serviceCommand)
                                      -> serviceCommand.setCommand(commandService.getCommand(serviceCommand.getAppId(),
                                          serviceCommand.getUuid(), serviceCommand.getDefaultVersion())));

          service.setServiceCommands(serviceCommands);
        }
      } catch (Exception e) {
        logger.error("Failed to retrieve service commands for serviceId {}  of appId  {}", service.getUuid(),
            service.getAppId(), e);
      }
    });
  }

  private List<ServiceCommand> getServiceCommandsByOrder(List<ServiceCommand> serviceCommands) {
    serviceCommands = serviceCommands.stream().sorted(comparingDouble(ServiceCommand::getOrder)).collect(toList());
    return serviceCommands;
  }

  @Override
  public List<CommandCategory> getCommandCategories(String appId, String serviceId, String commandName) {
    return commandHelper.getCommandCategories(appId, serviceId, commandName);
  }
}
