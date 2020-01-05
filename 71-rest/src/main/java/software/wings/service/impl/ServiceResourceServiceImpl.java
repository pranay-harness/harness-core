package software.wings.service.impl;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.beans.PageRequest.UNLIMITED;
import static io.harness.beans.SearchFilter.Operator.EQ;
import static io.harness.data.structure.CollectionUtils.emptyIfNull;
import static io.harness.data.structure.CollectionUtils.trimmedLowercaseSet;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.USER;
import static io.harness.mongo.MongoUtils.setUnset;
import static io.harness.pcf.model.PcfConstants.MANIFEST_YML;
import static io.harness.pcf.model.PcfConstants.VARS_YML;
import static io.harness.persistence.HQuery.excludeAuthority;
import static io.harness.validation.PersistenceValidator.duplicateCheck;
import static io.harness.validation.Validator.notNullCheck;
import static java.lang.String.format;
import static java.lang.String.join;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Comparator.comparingDouble;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.lang3.StringUtils.equalsIgnoreCase;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.atteo.evo.inflector.English.plural;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.api.DeploymentType.ECS;
import static software.wings.api.DeploymentType.HELM;
import static software.wings.api.DeploymentType.KUBERNETES;
import static software.wings.api.DeploymentType.PCF;
import static software.wings.api.DeploymentType.valueOf;
import static software.wings.beans.ConfigFile.DEFAULT_TEMPLATE_ID;
import static software.wings.beans.EntityVersion.Builder.anEntityVersion;
import static software.wings.beans.ServiceVariable.Type.ENCRYPTED_TEXT;
import static software.wings.beans.appmanifest.ManifestFile.VALUES_YAML_KEY;
import static software.wings.beans.command.Command.Builder.aCommand;
import static software.wings.beans.command.CommandUnitType.COMMAND;
import static software.wings.beans.command.ServiceCommand.Builder.aServiceCommand;
import static software.wings.helpers.ext.helm.HelmConstants.DEFAULT_HELM_VALUE_YAML;
import static software.wings.service.intfc.ServiceVariableService.EncryptedFieldMode.OBTAIN_VALUE;
import static software.wings.yaml.YamlHelper.trimYaml;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.io.Resources;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.danielbechler.diff.ObjectDifferBuilder;
import de.danielbechler.diff.node.DiffNode;
import io.harness.beans.ExecutionStatus;
import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.beans.SearchFilter.Operator;
import io.harness.data.structure.EmptyPredicate;
import io.harness.data.validator.EntityNameValidator;
import io.harness.eraro.ErrorCode;
import io.harness.event.handler.impl.EventPublishHelper;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.UnexpectedException;
import io.harness.exception.WingsException;
import io.harness.globalcontex.EntityOperationIdentifier;
import io.harness.globalcontex.EntityOperationIdentifier.entityOperation;
import io.harness.limits.Action;
import io.harness.limits.ActionType;
import io.harness.limits.LimitCheckerFactory;
import io.harness.limits.LimitEnforcementUtils;
import io.harness.limits.checker.StaticLimitCheckerWithDecrement;
import io.harness.limits.counter.service.CounterSyncer;
import io.harness.persistence.HIterator;
import io.harness.queue.QueuePublisher;
import io.harness.stream.BoundedInputStream;
import io.harness.validation.Create;
import io.harness.validation.PersistenceValidator;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.query.FindOptions;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.Sort;
import org.mongodb.morphia.query.UpdateOperations;
import ru.vyarus.guice.validator.group.annotation.ValidationGroups;
import software.wings.api.DeploymentType;
import software.wings.beans.AccountEvent;
import software.wings.beans.AccountEventType;
import software.wings.beans.Activity;
import software.wings.beans.Activity.ActivityKeys;
import software.wings.beans.AppContainer;
import software.wings.beans.CanaryOrchestrationWorkflow;
import software.wings.beans.CommandCategory;
import software.wings.beans.ConfigFile;
import software.wings.beans.EntityType;
import software.wings.beans.EntityVersion;
import software.wings.beans.Event.Type;
import software.wings.beans.FeatureName;
import software.wings.beans.GraphNode;
import software.wings.beans.InformationNotification;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.InfrastructureProvisioner;
import software.wings.beans.LambdaSpecification;
import software.wings.beans.LambdaSpecification.FunctionSpecification;
import software.wings.beans.LambdaSpecification.LambdaSpecificationKeys;
import software.wings.beans.OrchestrationWorkflow;
import software.wings.beans.PhaseStep;
import software.wings.beans.Service;
import software.wings.beans.Service.ServiceKeys;
import software.wings.beans.ServiceTemplate;
import software.wings.beans.ServiceVariable;
import software.wings.beans.Setup.SetupStatus;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowPhase;
import software.wings.beans.appmanifest.AppManifestKind;
import software.wings.beans.appmanifest.ApplicationManifest;
import software.wings.beans.appmanifest.ApplicationManifest.AppManifestSource;
import software.wings.beans.appmanifest.ManifestFile;
import software.wings.beans.appmanifest.StoreType;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.command.AmiCommandUnit;
import software.wings.beans.command.AwsLambdaCommandUnit;
import software.wings.beans.command.CodeDeployCommandUnit;
import software.wings.beans.command.Command;
import software.wings.beans.command.Command.CommandKeys;
import software.wings.beans.command.CommandUnit;
import software.wings.beans.command.CommandUnitType;
import software.wings.beans.command.ServiceCommand;
import software.wings.beans.container.ContainerTask;
import software.wings.beans.container.ContainerTask.ContainerTaskKeys;
import software.wings.beans.container.ContainerTaskType;
import software.wings.beans.container.EcsServiceSpecification;
import software.wings.beans.container.EcsServiceSpecification.EcsServiceSpecificationKeys;
import software.wings.beans.container.HelmChartSpecification;
import software.wings.beans.container.HelmChartSpecification.HelmChartSpecificationKeys;
import software.wings.beans.container.KubernetesPayload;
import software.wings.beans.container.PcfServiceSpecification;
import software.wings.beans.container.PcfServiceSpecification.PcfServiceSpecificationKeys;
import software.wings.beans.container.UserDataSpecification;
import software.wings.beans.container.UserDataSpecification.UserDataSpecificationKeys;
import software.wings.beans.template.Template;
import software.wings.beans.template.TemplateHelper;
import software.wings.beans.template.command.SshCommandTemplate;
import software.wings.common.NotificationMessageResolver.NotificationMessageType;
import software.wings.dl.WingsPersistence;
import software.wings.helpers.ext.helm.HelmHelper;
import software.wings.prune.PruneEntityListener;
import software.wings.prune.PruneEvent;
import software.wings.service.ServiceHelper;
import software.wings.service.impl.command.CommandHelper;
import software.wings.service.impl.template.SshCommandTemplateProcessor;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ApplicationManifestService;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.ArtifactStreamServiceBindingService;
import software.wings.service.intfc.CommandService;
import software.wings.service.intfc.ConfigService;
import software.wings.service.intfc.EntityVersionService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.HarnessTagService;
import software.wings.service.intfc.InfrastructureDefinitionService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.InfrastructureProvisionerService;
import software.wings.service.intfc.NotificationService;
import software.wings.service.intfc.PipelineService;
import software.wings.service.intfc.ResourceLookupService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.ServiceTemplateService;
import software.wings.service.intfc.ServiceVariableService;
import software.wings.service.intfc.TriggerService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.service.intfc.WorkflowService;
import software.wings.service.intfc.instance.InstanceService;
import software.wings.service.intfc.ownership.OwnedByService;
import software.wings.service.intfc.template.TemplateService;
import software.wings.service.intfc.yaml.YamlPushService;
import software.wings.sm.ContextElement;
import software.wings.stencils.DataProvider;
import software.wings.stencils.Stencil;
import software.wings.stencils.StencilCategory;
import software.wings.stencils.StencilPostProcessor;
import software.wings.utils.ApplicationManifestUtils;
import software.wings.utils.ArtifactType;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
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
@Slf4j
public class ServiceResourceServiceImpl implements ServiceResourceService, DataProvider {
  private static final String IISWEBSITE_KEYWORD = "iiswebsite";
  private static final String IISAPP_KEYWORD = "iisapp";
  private static final String IIS_INSTALL_COMMAND_NAME = "Install";
  private static final String INSTALL_IIS_WEBSITE_TEMPLATE_NAME = "Install IIS Website";
  private static final String INSTALL_IIS_APPLICATION_TEMPLATE_NAME = "Install IIS Application";
  private static String default_k8s_deployment_yaml;
  private static String default_k8s_namespace_yaml;
  private static String default_k8s_service_yaml;
  private static String default_k8s_values_yaml;
  private static String default_pcf_manifest_yml;
  private static String default_pcf_vars_yml;

  static {
    try {
      URL url = ServiceResourceServiceImpl.class.getClassLoader().getResource("default-k8s-manifests/deployment.yaml");
      default_k8s_deployment_yaml = Resources.toString(url, StandardCharsets.UTF_8);

      url = ServiceResourceServiceImpl.class.getClassLoader().getResource("default-k8s-manifests/namespace.yaml");
      default_k8s_namespace_yaml = Resources.toString(url, StandardCharsets.UTF_8);

      url = ServiceResourceServiceImpl.class.getClassLoader().getResource("default-k8s-manifests/service.yaml");
      default_k8s_service_yaml = Resources.toString(url, StandardCharsets.UTF_8);

      url = ServiceResourceServiceImpl.class.getClassLoader().getResource("default-k8s-manifests/values.yaml");
      default_k8s_values_yaml = Resources.toString(url, StandardCharsets.UTF_8);

      url = ServiceResourceServiceImpl.class.getClassLoader().getResource("default-pcf-manifests/manifest.yml");
      default_pcf_manifest_yml = Resources.toString(url, StandardCharsets.UTF_8);

      url = ServiceResourceServiceImpl.class.getClassLoader().getResource("default-pcf-manifests/vars.yml");
      default_pcf_vars_yml = Resources.toString(url, StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new RuntimeException("Failed to read default manifests", e);
    }
  }

  @Inject private CounterSyncer counterSyncer;
  @Inject private WingsPersistence wingsPersistence;
  @Inject private AppService appService;
  @Inject private ArtifactService artifactService;
  @Inject private ArtifactStreamService artifactStreamService;
  // DO NOT DELETE THIS, PRUNE logic needs it
  @Inject private InstanceService instanceService;
  @Inject private CommandService commandService;
  @Inject private ConfigService configService;
  @Inject private EntityVersionService entityVersionService;
  @Inject private ExecutorService executorService;
  @Inject private NotificationService notificationService;
  @Inject private ServiceTemplateService serviceTemplateService;
  @Inject private ServiceVariableService serviceVariableService;
  @Inject private InfrastructureProvisionerService infrastructureProvisionerService;
  @Inject private TriggerService triggerService;
  @Inject private WorkflowService workflowService;
  @Inject private StencilPostProcessor stencilPostProcessor;
  @Inject private ServiceHelper serviceHelper;
  @Inject private CommandHelper commandHelper;
  @Inject private TemplateService templateService;
  @Inject private TemplateHelper templateHelper;
  @Inject private HelmHelper helmHelper;
  @Inject private InfrastructureMappingService infrastructureMappingService;
  @Inject private YamlPushService yamlPushService;
  @Inject private PipelineService pipelineService;
  @Inject private SshCommandTemplateProcessor sshCommandTemplateProcessor;
  @Inject private ApplicationManifestService applicationManifestService;
  @Inject private LimitCheckerFactory limitCheckerFactory;
  @Inject private AuditServiceHelper auditServiceHelper;
  @Inject private FeatureFlagService featureFlagService;
  @Inject private ArtifactStreamServiceBindingService artifactStreamServiceBindingService;
  @Inject private InfrastructureDefinitionService infrastructureDefinitionService;
  @Inject private EventPublishHelper eventPublishHelper;

  @Inject private QueuePublisher<PruneEvent> pruneQueue;
  @Inject private ApplicationManifestUtils applicationManifestUtils;
  @Inject private HarnessTagService harnessTagService;
  @Inject private ResourceLookupService resourceLookupService;
  @Inject private WorkflowExecutionService workflowExecutionService;

  /**
   * {@inheritDoc}
   */
  @Override
  public PageResponse<Service> list(PageRequest<Service> request, boolean withBuildSource, boolean withServiceCommands,
      boolean withTags, String tagFilter) {
    PageResponse<Service> pageResponse =
        resourceLookupService.listWithTagFilters(request, tagFilter, EntityType.SERVICE, withTags);

    List<Service> services = pageResponse.getResponse();
    if (withServiceCommands) {
      setServiceCommands(services);
    }
    if (withBuildSource) {
      setArtifactStreams(services);
      setArtifactStreamBindings(services);
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
    String accountId = appService.getAccountIdByAppId(service.getAppId());
    service.setAccountId(accountId);

    // TODO: ASR: IMP: update the block below for artifact variables as service variable
    if (createdFromYaml) {
      if (featureFlagService.isEnabled(FeatureName.ARTIFACT_STREAM_REFACTOR, accountId)) {
        List<String> artifactStreamIds = service.getArtifactStreamIds();
        if (artifactStreamIds == null) {
          artifactStreamIds = new ArrayList<>();
        }
        service.setArtifactStreamIds(artifactStreamIds);
      }
    }

    StaticLimitCheckerWithDecrement checker = (StaticLimitCheckerWithDecrement) limitCheckerFactory.getInstance(
        new Action(accountId, ActionType.CREATE_SERVICE));

    return LimitEnforcementUtils.withLimitCheck(checker, () -> {
      setKeyWords(service);
      Service savedService =
          duplicateCheck(() -> wingsPersistence.saveAndGet(Service.class, service), "name", service.getName());

      // Mark this create op into GlobalAuditContext so nested entity creation can be related to it
      updateGlobalContextWithServiceCreationEvent(savedService);
      if (createDefaultCommands && !skipDefaultCommands(service)) {
        savedService = addDefaultCommands(savedService, !createdFromYaml);
      }

      savedService = createDefaultHelmValueYaml(savedService, createdFromYaml);
      serviceTemplateService.createDefaultTemplatesByService(savedService);
      createDefaultK8sManifests(savedService, service.isSyncFromGit());
      createDefaultPCFManifestsIfApplicable(savedService, service.isSyncFromGit());

      sendNotificationAsync(savedService, NotificationMessageType.ENTITY_CREATE_NOTIFICATION);

      yamlPushService.pushYamlChangeSet(accountId, null, savedService, Type.CREATE, service.isSyncFromGit(), false);

      if (!savedService.isSample()) {
        eventPublishHelper.publishAccountEvent(
            accountId, AccountEvent.builder().accountEventType(AccountEventType.SERVICE_CREATED).build(), true, true);
      }
      return savedService;
    });
  }

  private void updateGlobalContextWithServiceCreationEvent(Service savedService) {
    auditServiceHelper.addEntityOperationIdentifierDataToAuditContext(EntityOperationIdentifier.builder()
                                                                          .entityId(savedService.getUuid())
                                                                          .entityType(EntityType.SERVICE.name())
                                                                          .entityName(savedService.getName())
                                                                          .operation(entityOperation.CREATE)
                                                                          .build());
  }

  private void updateGlobalContextWithServiceDeletionEvent(Service savedService) {
    auditServiceHelper.addEntityOperationIdentifierDataToAuditContext(EntityOperationIdentifier.builder()
                                                                          .entityId(savedService.getUuid())
                                                                          .entityType(EntityType.SERVICE.name())
                                                                          .entityName(savedService.getName())
                                                                          .operation(entityOperation.DELETE)
                                                                          .build());
  }

  private void sendNotificationAsync(Service savedService, NotificationMessageType entityCreateNotification) {
    notificationService.sendNotificationAsync(InformationNotification.builder()
                                                  .appId(savedService.getAppId())
                                                  .notificationTemplateId(entityCreateNotification.name())
                                                  .notificationTemplateVariables(ImmutableMap.of(
                                                      "ENTITY_TYPE", "Service", "ENTITY_NAME", savedService.getName()))
                                                  .build());
  }

  private boolean skipDefaultCommands(Service service) {
    if (ArtifactType.PCF.equals(service.getArtifactType())) {
      return true;
    }

    return false;
  }

  @Override
  @ValidationGroups(Create.class)
  public Service clone(String appId, String originalServiceId, Service service) {
    String accountId = appService.getAccountIdByAppId(service.getAppId());

    if (isEmpty(service.getName()) || isEmpty(service.getName().trim())) {
      throw new InvalidRequestException("Service Name can not be empty", USER);
    }

    StaticLimitCheckerWithDecrement checker = (StaticLimitCheckerWithDecrement) limitCheckerFactory.getInstance(
        new Action(accountId, ActionType.CREATE_SERVICE));

    return LimitEnforcementUtils.withLimitCheck(checker, () -> {
      Service originalService = getWithDetails(appId, originalServiceId);
      Service clonedService = originalService.cloneInternal();
      clonedService.setName(service.getName());
      clonedService.setDescription(service.getDescription());
      setKeyWords(clonedService);
      Service savedCloneService =
          duplicateCheck(() -> wingsPersistence.saveAndGet(Service.class, clonedService), "name", service.getName());

      // Push this service to Git
      yamlPushService.pushYamlChangeSet(
          accountId, null, savedCloneService, Type.CREATE, service.isSyncFromGit(), false);
      updateGlobalContextWithServiceCreationEvent(savedCloneService);
      updateGlobalContextWithServiceCreationEvent(savedCloneService);

      boolean shouldPushToYaml = !hasInternalCommands(originalService);
      originalService.getServiceCommands().forEach(serviceCommand -> {
        ServiceCommand clonedServiceCommand = serviceCommand.cloneInternal();
        addCommand(savedCloneService.getAppId(), savedCloneService.getUuid(), clonedServiceCommand, shouldPushToYaml);
      });

      // Copy ContainerTask, HelmChartSpecification, PcfSpecification
      cloneServiceSpecifications(appId, originalService, savedCloneService.getUuid());

      List<ServiceTemplate> serviceTemplates =
          serviceTemplateService
              .list(aPageRequest()
                        .addFilter(ServiceTemplate.APP_ID_KEY, EQ, originalService.getAppId())
                        .addFilter(ServiceTemplate.SERVICE_ID_KEY, EQ, originalService.getUuid())
                        .build(),
                  false, OBTAIN_VALUE)
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
    });
  }

  private void cloneServiceSpecifications(String appId, Service originalService, String clonedServiceId) {
    String originalServiceId = originalService.getUuid();

    clonePcfSpecification(appId, clonedServiceId, originalServiceId);

    if (ArtifactType.DOCKER.equals(originalService.getArtifactType())) {
      cloneHelmChartSpecification(appId, clonedServiceId, originalServiceId);
      cloneContainerTasks(appId, clonedServiceId, originalServiceId);
    }

    cloneAppManifests(appId, clonedServiceId, originalServiceId);
  }

  private void cloneContainerTasks(String appId, String clonedServiceId, String originalServiceId) {
    List<ContainerTask> containerTasks = findContainerTaskForService(appId, originalServiceId);
    if (EmptyPredicate.isNotEmpty(containerTasks)) {
      containerTasks.forEach(containerTask -> {
        ContainerTask newContainerTask = containerTask.cloneInternal();
        newContainerTask.setServiceId(clonedServiceId);
        createContainerTask(newContainerTask, false);
      });
    }
  }

  private void cloneHelmChartSpecification(String appId, String clonedServiceId, String originalServiceId) {
    HelmChartSpecification helmChartSpecification = getHelmChartSpecification(appId, originalServiceId);
    if (helmChartSpecification != null) {
      HelmChartSpecification helmChartSpecificationNew = helmChartSpecification.cloneInternal();
      helmChartSpecificationNew.setServiceId(clonedServiceId);
      createHelmChartSpecification(helmChartSpecificationNew);
    }
  }

  private void clonePcfSpecification(String appId, String clonedServiceId, String originalServiceId) {
    PcfServiceSpecification pcfServiceSpecification = getPcfServiceSpecification(appId, originalServiceId);
    if (pcfServiceSpecification != null) {
      PcfServiceSpecification pcfServiceSpecificationNew = pcfServiceSpecification.cloneInternal();
      pcfServiceSpecificationNew.setServiceId(clonedServiceId);
      createPcfServiceSpecification(pcfServiceSpecificationNew);
    }
  }

  private void setKeyWords(Service clonedService) {
    clonedService.setKeywords(trimmedLowercaseSet(clonedService.generateKeywords()));
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
    List<ServiceCommand> serviceCommands = getServiceCommands(appId, serviceId, false);
    try {
      Map<String, Integer> commandNameVersionMap =
          serviceCommands.stream()
              .filter(serviceCommand -> serviceCommand.getVersionForEnv(envId) != 0)
              .collect(toMap(ServiceCommand::getName, serviceCommand -> serviceCommand.getVersionForEnv(envId)));
      return getFlattenCommandUnitList(appId, serviceId, commandName, commandNameVersionMap);
    } catch (IllegalStateException ex) {
      Service service = get(appId, serviceId);
      Set<String> serviceCommandNames = new HashSet<>();
      Set<String> duplicateNames = emptyIfNull(serviceCommands)
                                       .stream()
                                       .map(ServiceCommand::getName)
                                       .filter(name -> !serviceCommandNames.add(name))
                                       .collect(Collectors.toSet());
      if (isNotEmpty(duplicateNames)) {
        throw new InvalidRequestException(
            format("Duplicate service command name %s for service %s", duplicateNames.toString(), service.getName()),
            USER);
      } else {
        throw new UnexpectedException();
      }
    }
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
    String accountId = appService.getAccountIdByAppId(service.getAppId());
    List<Command> commands = emptyList();
    ArtifactType artifactType = service.getArtifactType();
    AppContainer appContainer = service.getAppContainer();
    if (appContainer != null && appContainer.getFamily() != null) {
      commands = appContainer.getFamily().getDefaultCommands(artifactType, appContainer);
    } else if (artifactType != null) {
      Command command;
      Template template;
      switch (artifactType) {
        case IIS:
          template = templateService.fetchTemplateByKeyword(accountId, IISWEBSITE_KEYWORD);
          command = sshCommandTemplateProcessor.fetchEntityFromTemplate(template, EntityType.COMMAND);
          if (command != null) {
            if (template.getName().equals(INSTALL_IIS_WEBSITE_TEMPLATE_NAME)) {
              command.setName(IIS_INSTALL_COMMAND_NAME);
            }
            commands = asList(command);
          }
          break;
        case IIS_APP:
        case IIS_VirtualDirectory:
          template = templateService.fetchTemplateByKeyword(accountId, IISAPP_KEYWORD);
          command = sshCommandTemplateProcessor.fetchEntityFromTemplate(template, EntityType.COMMAND);
          if (command != null) {
            if (template.getName().equals(INSTALL_IIS_APPLICATION_TEMPLATE_NAME)) {
              command.setName(IIS_INSTALL_COMMAND_NAME);
            }
            commands = asList(command);
          }
          break;
        default:
          commands = artifactType.getDefaultCommands();
      }
    }

    // Default Commands are pushed to yaml only if it matches both the conditions
    // 1) pushToYaml is true 2) commands are not internal. (Check hasInternalCommands()).
    boolean shouldPushCommandsToYaml = pushToYaml && !hasInternalCommands(service);

    Service serviceToReturn = service;
    for (Command command : commands) {
      serviceToReturn = addCommand(service.getAppId(), service.getUuid(),
          aServiceCommand()
              .withName(command.getName())
              .withTargetToAllEnv(true)
              .withTemplateVersion(command.getTemplateVersion())
              .withTemplateUuid(command.getTemplateId())
              .withCommand(command)
              .build(),
          shouldPushCommandsToYaml);
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
    // TODO: ASR: YAML: needs to be updated when bindings support is added to YAML
    Service savedService = get(service.getAppId(), service.getUuid(), false);
    notNullCheck("Service", savedService);

    Set<String> keywords = trimmedLowercaseSet(service.generateKeywords());
    UpdateOperations<Service> updateOperations =
        wingsPersistence.createUpdateOperations(Service.class)
            .set(ServiceKeys.name, service.getName())
            .set(ServiceKeys.description, Optional.ofNullable(service.getDescription()).orElse(""))
            .set(ServiceKeys.keywords, keywords);

    if (fromYaml) {
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
      if (featureFlagService.isEnabled(FeatureName.ARTIFACT_STREAM_REFACTOR, savedService.getAccountId())) {
        // TODO: ASR: IMP: update the block below for artifact variables as service variable
        List<String> artifactStreamIds = service.getArtifactStreamIds();
        if (artifactStreamIds == null) {
          artifactStreamIds = new ArrayList<>();
        }
        updateOperations.set("artifactStreamIds", artifactStreamIds);
      }
    }

    PersistenceValidator.duplicateCheck(
        () -> wingsPersistence.update(savedService, updateOperations), "name", service.getName());
    Service updatedService = get(service.getAppId(), service.getUuid(), false);

    if (!savedService.getName().equals(service.getName())) {
      executorService.submit(() -> triggerService.updateByApp(service.getAppId()));
      serviceTemplateService.updateDefaultServiceTemplateName(
          service.getAppId(), service.getUuid(), savedService.getName(), service.getName());
    }

    String accountId = appService.getAccountIdByAppId(service.getAppId());
    boolean isRename = !savedService.getName().equals(service.getName());
    yamlPushService.pushYamlChangeSet(
        accountId, savedService, updatedService, Type.UPDATE, service.isSyncFromGit(), isRename);

    return updatedService;
  }

  @Override
  public Service updateArtifactStreamIds(Service service, List<String> artifactStreamIds) {
    if (artifactStreamIds == null) {
      artifactStreamIds = new ArrayList<>();
    }

    Service savedService = get(service.getAppId(), service.getUuid(), false);
    notNullCheck("Service", savedService);

    UpdateOperations<Service> updateOperations =
        wingsPersistence.createUpdateOperations(Service.class).set(ServiceKeys.artifactStreamIds, artifactStreamIds);

    wingsPersistence.update(savedService, updateOperations);
    Service updatedService = get(service.getAppId(), service.getUuid(), false);

    String accountId = appService.getAccountIdByAppId(service.getAppId());
    yamlPushService.pushYamlChangeSet(
        accountId, savedService, updatedService, Type.UPDATE, service.isSyncFromGit(), false);

    return updatedService;
  }

  @Override
  public Service get(String serviceId) {
    return wingsPersistence.get(Service.class, serviceId);
  }

  @Override
  public Service getWithDetails(String appId, String serviceId) {
    return get(appId, serviceId, true);
  }

  @Override
  public Service get(String appId, String serviceId) {
    return get(appId, serviceId, false);
  }

  @Override
  public Service getServiceByName(String appId, String serviceName) {
    return getServiceByName(appId, serviceName, true);
  }

  @Override
  public Service getServiceByName(String appId, String serviceName, boolean withDetails) {
    Service service =
        wingsPersistence.createQuery(Service.class).filter("appId", appId).filter(ServiceKeys.name, serviceName).get();
    if (service != null) {
      if (withDetails) {
        setServiceDetails(service, appId);
      }
    }
    return service;
  }

  @Override
  public Service get(String appId, String serviceId, boolean includeDetails) {
    Service service = wingsPersistence.getWithAppId(Service.class, appId, serviceId);
    if (service != null && includeDetails) {
      setServiceDetails(service, appId);
    }
    return service;
  }

  private void setServiceDetails(Service service, String appId) {
    service.setConfigFiles(configService.getConfigFilesForEntity(appId, DEFAULT_TEMPLATE_ID, service.getUuid()));
    service.setServiceVariables(
        serviceVariableService.getServiceVariablesForEntity(appId, service.getUuid(), OBTAIN_VALUE));
    service.setServiceCommands(getServiceCommands(appId, service.getUuid()));
  }

  private void setArtifactStreamBindings(List<Service> services) {
    if (isEmpty(services)) {
      return;
    }

    services.forEach(service
        -> service.setArtifactStreamBindings(
            artifactStreamServiceBindingService.list(service.getAppId(), service.getUuid())));
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
    delete(appId, serviceId, false, false);
  }

  @Override
  public void deleteByYamlGit(String appId, String serviceId, boolean syncFromGit) {
    delete(appId, serviceId, false, syncFromGit);
  }

  private void delete(String appId, String serviceId, boolean forceDelete, boolean syncFromGit) {
    Service service = wingsPersistence.getWithAppId(Service.class, appId, serviceId);
    if (service == null) {
      return;
    }
    String accountId = appService.getAccountIdByAppId(service.getAppId());
    StaticLimitCheckerWithDecrement checker = (StaticLimitCheckerWithDecrement) limitCheckerFactory.getInstance(
        new Action(accountId, ActionType.CREATE_SERVICE));
    LimitEnforcementUtils.withCounterDecrement(checker, () -> {
      if (!forceDelete) {
        // Ensure service is safe to delete
        ensureServiceSafeToDelete(service);
      }

      // safe to delete
      if (wingsPersistence.delete(Service.class, service.getUuid())) {
        yamlPushService.pushYamlChangeSet(accountId, service, null, Type.DELETE, syncFromGit, false);
        pruneQueue.send(new PruneEvent(Service.class, service.getAppId(), service.getUuid()));
        updateGlobalContextWithServiceDeletionEvent(service);

        getServiceCommands(appId, serviceId, false)
            .forEach(serviceCommand -> deleteServiceCommand(service, serviceCommand, syncFromGit));
        sendNotificationAsync(service, NotificationMessageType.ENTITY_DELETE_NOTIFICATION);
        pruneDeploymentSpecifications(service);
      }
    });
  }

  private void pruneDeploymentSpecifications(Service service) {
    PcfServiceSpecification pcfServiceSpecification = getPcfServiceSpecification(service.getAppId(), service.getUuid());
    if (pcfServiceSpecification != null) {
      wingsPersistence.delete(HelmChartSpecification.class, service.getAppId(), pcfServiceSpecification.getUuid());
      auditServiceHelper.reportDeleteForAuditing(service.getAppId(), pcfServiceSpecification);
    }

    ContainerTask containerTask = getContainerTaskByDeploymentType(service.getAppId(), service.getUuid(), ECS.name());
    if (containerTask == null) {
      containerTask = getContainerTaskByDeploymentType(service.getAppId(), service.getUuid(), KUBERNETES.name());
    }
    if (containerTask != null) {
      wingsPersistence.delete(ContainerTask.class, service.getAppId(), containerTask.getUuid());
      auditServiceHelper.reportDeleteForAuditing(service.getAppId(), containerTask);
    }

    HelmChartSpecification helmChartSpecification = getHelmChartSpecification(service.getAppId(), service.getUuid());
    if (helmChartSpecification != null) {
      wingsPersistence.delete(HelmChartSpecification.class, service.getAppId(), helmChartSpecification.getUuid());
      auditServiceHelper.reportDeleteForAuditing(service.getAppId(), helmChartSpecification);
    }

    UserDataSpecification userDataSpecification = getUserDataSpecification(service.getAppId(), service.getUuid());
    if (userDataSpecification != null) {
      wingsPersistence.delete(UserDataSpecification.class, service.getAppId(), userDataSpecification.getUuid());
      auditServiceHelper.reportDeleteForAuditing(service.getAppId(), userDataSpecification);
    }

    EcsServiceSpecification ecsServiceSpecification = getEcsServiceSpecification(service.getAppId(), service.getUuid());
    if (ecsServiceSpecification != null) {
      wingsPersistence.delete(EcsServiceSpecification.class, service.getAppId(), ecsServiceSpecification.getUuid());
      auditServiceHelper.reportDeleteForAuditing(service.getAppId(), ecsServiceSpecification);
    }

    LambdaSpecification lambdaSpecification = getLambdaSpecification(service.getAppId(), service.getUuid());
    if (lambdaSpecification != null) {
      wingsPersistence.delete(LambdaSpecification.class, service.getAppId(), lambdaSpecification.getUuid());
      auditServiceHelper.reportDeleteForAuditing(service.getAppId(), lambdaSpecification);
    }
  }

  @Override
  public void pruneDescendingEntities(@NotEmpty String appId, @NotEmpty String serviceId) {
    List<OwnedByService> services =
        ServiceClassLocator.descendingServices(this, ServiceResourceServiceImpl.class, OwnedByService.class);
    PruneEntityListener.pruneDescendingEntities(services, descending -> descending.pruneByService(appId, serviceId));
  }

  private void ensureServiceSafeToDelete(Service service) {
    // Ensure service and and sevice commands referenced by workflow

    String accountId = service.getAccountId();
    if (featureFlagService.isEnabled(FeatureName.INFRA_MAPPING_REFACTOR, accountId)) {
      List<String> referencingInfraDefinitionNames =
          infrastructureDefinitionService.listNamesByScopedService(service.getAppId(), service.getUuid());
      if (isNotEmpty(referencingInfraDefinitionNames)) {
        throw new InvalidRequestException(
            format("Service %s is referenced by %s %s [%s].", service.getName(), referencingInfraDefinitionNames.size(),
                plural("Infrastructure Definition", referencingInfraDefinitionNames.size()),
                join(", ", referencingInfraDefinitionNames)),
            USER);
      }
    }

    List<String> referencingWorkflowNames =
        workflowService.obtainWorkflowNamesReferencedByService(service.getAppId(), service.getUuid());

    if (isNotEmpty(referencingWorkflowNames)) {
      throw new InvalidRequestException(
          format("Service %s is referenced by %s %s [%s].", service.getUuid(), referencingWorkflowNames.size(),
              plural("workflow", referencingWorkflowNames.size()), join(", ", referencingWorkflowNames)),
          USER);
    }

    if (!featureFlagService.isEnabled(FeatureName.INFRA_MAPPING_REFACTOR, accountId)) {
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

    List<String> refPipelines =
        pipelineService.obtainPipelineNamesReferencedByTemplatedEntity(service.getAppId(), service.getUuid());
    if (isNotEmpty(refPipelines)) {
      throw new InvalidRequestException(
          format("Service is referenced by %d %s [%s] as a workflow variable.", refPipelines.size(),
              plural("pipeline", refPipelines.size()), join(", ", refPipelines)),
          USER);
    }

    List<String> refTriggers =
        triggerService.obtainTriggerNamesReferencedByTemplatedEntityId(service.getAppId(), service.getUuid());
    if (isNotEmpty(refTriggers)) {
      throw new InvalidRequestException(
          format("Service is referenced by %d %s [%s] as a workflow variable.", refTriggers.size(),
              plural("trigger", refTriggers.size()), join(", ", refTriggers)),
          USER);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Service deleteCommand(String appId, String serviceId, String commandId) {
    return deleteCommand(appId, serviceId, commandId, false);
  }

  @Override
  public Service deleteByYamlGit(String appId, String serviceId, String commandId, boolean syncFromGit) {
    return deleteCommand(appId, serviceId, commandId, syncFromGit);
  }

  @Override
  public boolean exists(String appId, String serviceId) {
    return wingsPersistence.createQuery(Service.class)
               .filter(Service.APP_ID_KEY, appId)
               .filter(Service.ID_KEY, serviceId)
               .getKey()
        != null;
  }

  @Override
  public List<String> fetchServiceNamesByUuids(String appId, List<String> serviceUuids) {
    if (isNotEmpty(serviceUuids)) {
      List<Service> services = wingsPersistence.createQuery(Service.class)
                                   .project(ServiceKeys.name, true)
                                   .project(ServiceKeys.accountId, true)
                                   .project(ServiceKeys.appId, true)
                                   .filter(ServiceKeys.appId, appId)
                                   .field("uuid")
                                   .in(serviceUuids)
                                   .asList();

      List<String> orderedServices = new ArrayList<>();
      Map<String, String> servicesMap = services.stream().collect(Collectors.toMap(Service::getUuid, Service::getName));
      for (String serviceId : serviceUuids) {
        if (servicesMap.containsKey(serviceId)) {
          orderedServices.add(servicesMap.get(serviceId));
        }
      }
      return orderedServices;
    }
    return new ArrayList<>();
  }

  private Service deleteCommand(String appId, String serviceId, String commandId, boolean syncFromGit) {
    Service service = wingsPersistence.getWithAppId(Service.class, appId, serviceId);
    notNullCheck("service", service);
    ServiceCommand serviceCommand = wingsPersistence.getWithAppId(ServiceCommand.class, service.getAppId(), commandId);

    ensureServiceCommandSafeToDelete(service, serviceCommand);
    deleteServiceCommand(service, serviceCommand, syncFromGit);
    return getWithDetails(service.getAppId(), service.getUuid());
  }

  private void deleteServiceCommand(Service service, ServiceCommand serviceCommand, boolean syncFromGit) {
    boolean serviceCommandDeleted = wingsPersistence.delete(serviceCommand);
    if (serviceCommandDeleted) {
      boolean deleted = wingsPersistence.delete(wingsPersistence.createQuery(Command.class)
                                                    .filter("appId", service.getAppId())
                                                    .filter(CommandKeys.originEntityId, serviceCommand.getUuid()));
      if (deleted) {
        String accountId = appService.getAccountIdByAppId(service.getAppId());
        yamlPushService.pushYamlChangeSet(accountId, serviceCommand, null, Type.DELETE, syncFromGit, false);
      }
    }
  }

  private void ensureServiceCommandSafeToDelete(Service service, ServiceCommand serviceCommand) {
    List<Workflow> workflows =
        workflowService
            .listWorkflows(
                aPageRequest().withLimit(UNLIMITED).addFilter("appId", Operator.EQ, service.getAppId()).build())
            .getResponse();
    StringBuilder sb = new StringBuilder();
    for (Workflow workflow : workflows) {
      OrchestrationWorkflow orchestrationWorkflow = workflow.getOrchestrationWorkflow();
      if (orchestrationWorkflow instanceof CanaryOrchestrationWorkflow) {
        List<WorkflowPhase> workflowPhases = ((CanaryOrchestrationWorkflow) orchestrationWorkflow).getWorkflowPhases();

        // May happen if no phase created for Canary workflow
        if (isEmpty(workflowPhases)) {
          continue;
        }

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
      String message = format("Command [%s] couldn't be deleted. Remove reference from the following workflows [%s]",
          serviceCommand.getName(), sb.toString());
      throw new InvalidRequestException(message, USER);
    }
  }

  @Override
  public void pruneByApplication(String appId) {
    String accountId = null;

    List<Service> services = findServicesByAppInternal(appId);

    for (Service service : services) {
      wingsPersistence.delete(Service.class, service.getUuid());
      accountId = service.getAccountId();
      auditServiceHelper.reportDeleteForAuditing(service.getAppId(), service);
      pruneDeploymentSpecifications(service);
      pruneDescendingEntities(appId, service.getUuid());
      harnessTagService.pruneTagLinks(service.getAccountId(), service.getUuid());
    }

    if (!StringUtils.isEmpty(accountId)) {
      counterSyncer.syncServiceCount(accountId);
    }
  }

  @Override
  public List<Service> findServicesByApp(String appId) {
    PageRequest<Service> pageRequest = aPageRequest().addFilter(ServiceKeys.appId, EQ, appId).build();
    return wingsPersistence.query(Service.class, pageRequest).getResponse();
  }

  @Override
  public List<Service> findServicesByAppInternal(String appId) {
    return wingsPersistence.createQuery(Service.class).filter(ServiceKeys.appId, appId).asList();
  }

  @Override
  public Service get(String appId, String serviceId, SetupStatus status) {
    return getWithDetails(appId, serviceId);
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
    Service service = getWithDetails(appId, persistedContainerTask.getServiceId());

    Type type = isCreate ? Type.CREATE : Type.UPDATE;
    yamlPushService.pushYamlChangeSet(accountId, service, persistedContainerTask, type, containerTask.isSyncFromGit());

    return persistedContainerTask;
  }

  @Override
  public void deleteContainerTask(String appId, String containerTaskId) {
    String accountId = appService.getAccountIdByAppId(appId);
    ContainerTask containerTask = wingsPersistence.get(ContainerTask.class, containerTaskId);

    if (containerTask == null) {
      return;
    }

    Service service = getWithDetails(appId, containerTask.getServiceId());
    if (wingsPersistence.delete(ContainerTask.class, appId, containerTaskId)) {
      yamlPushService.pushYamlChangeSet(accountId, service, containerTask, Type.DELETE, containerTask.isSyncFromGit());
    }
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
                                      .filter(ContainerTaskKeys.serviceId, serviceId)
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
  public EcsServiceSpecification getEcsServiceSpecification(String appId, String serviceId) {
    return wingsPersistence.createQuery(EcsServiceSpecification.class)
        .filter("appId", appId)
        .filter(EcsServiceSpecificationKeys.serviceId, serviceId)
        .get();
  }

  @Override
  public EcsServiceSpecification createEcsServiceSpecification(EcsServiceSpecification ecsServiceSpecification) {
    return upsertEcsServiceSpecification(ecsServiceSpecification, true);
  }

  @Override
  public void deleteEcsServiceSpecification(String appId, String ecsServiceSpecificationId) {
    EcsServiceSpecification ecsServiceSpecification =
        wingsPersistence.get(EcsServiceSpecification.class, ecsServiceSpecificationId);
    if (ecsServiceSpecification == null) {
      return;
    }

    String accountId = appService.getAccountIdByAppId(appId);
    Service service = getWithDetails(appId, ecsServiceSpecification.getServiceId());

    if (wingsPersistence.delete(EcsServiceSpecification.class, appId, ecsServiceSpecificationId)) {
      yamlPushService.pushYamlChangeSet(
          accountId, service, ecsServiceSpecification, Type.DELETE, ecsServiceSpecification.isSyncFromGit());
    }
  }

  @Override
  public EcsServiceSpecification getEcsServiceSpecificationById(String appId, String ecsServiceSpecificationId) {
    return wingsPersistence.getWithAppId(EcsServiceSpecification.class, appId, ecsServiceSpecificationId);
  }

  @Override
  public EcsServiceSpecification updateEcsServiceSpecification(EcsServiceSpecification ecsServiceSpecification) {
    return upsertEcsServiceSpecification(ecsServiceSpecification, false);
  }

  @Override
  public EcsServiceSpecification resetToDefaultEcsServiceSpecification(String appId, String serviceId) {
    boolean exist = exist(appId, serviceId);
    if (!exist) {
      throw new InvalidRequestException("Service doesn't exist");
    }

    EcsServiceSpecification specification = getEcsServiceSpecification(appId, serviceId);
    if (specification == null) {
      EcsServiceSpecification ecsServiceSpecification = EcsServiceSpecification.builder().serviceId(serviceId).build();
      ecsServiceSpecification.setAppId(appId);
      ecsServiceSpecification.resetToDefaultSpecification();
      return ecsServiceSpecification;
    } else {
      specification.resetToDefaultSpecification();
      return upsertEcsServiceSpecification(specification, false);
    }
  }

  @Override
  public EcsServiceSpecification getExistingOrDefaultEcsServiceSpecification(String appId, String serviceId) {
    EcsServiceSpecification ecsServiceSpecification = getEcsServiceSpecification(appId, serviceId);
    if (ecsServiceSpecification == null) {
      ecsServiceSpecification = EcsServiceSpecification.builder().serviceId(serviceId).build();
      ecsServiceSpecification.setAppId(appId);
      ecsServiceSpecification.resetToDefaultSpecification();
    }
    return ecsServiceSpecification;
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
    Service service = getWithDetails(appId, persistedHelmChartSpecification.getServiceId());

    Type type = isCreate ? Type.CREATE : Type.UPDATE;
    yamlPushService.pushYamlChangeSet(
        accountId, service, persistedHelmChartSpecification, type, helmChartSpecification.isSyncFromGit());

    return persistedHelmChartSpecification;
  }

  private EcsServiceSpecification upsertEcsServiceSpecification(
      EcsServiceSpecification ecsServiceSpecification, boolean isCreate) {
    boolean exist = exist(ecsServiceSpecification.getAppId(), ecsServiceSpecification.getServiceId());
    if (!exist) {
      throw new InvalidRequestException("Service doesn't exist");
    }

    Type type = Type.CREATE;
    EcsServiceSpecification specification =
        getEcsServiceSpecification(ecsServiceSpecification.getAppId(), ecsServiceSpecification.getServiceId());
    if (specification != null) {
      ecsServiceSpecification.setUuid(specification.getUuid());
      type = Type.UPDATE;
    }

    EcsServiceSpecification serviceSpecification =
        wingsPersistence.saveAndGet(EcsServiceSpecification.class, ecsServiceSpecification);

    String appId = serviceSpecification.getAppId();
    String accountId = appService.getAccountIdByAppId(appId);
    Service service = getWithDetails(appId, serviceSpecification.getServiceId());

    yamlPushService.pushYamlChangeSet(
        accountId, service, serviceSpecification, type, serviceSpecification.isSyncFromGit());

    return serviceSpecification;
  }

  @Override
  public void deleteHelmChartSpecification(String appId, String helmChartSpecificationId) {
    deleteHelmChartSpecification(getHelmChartSpecificationById(appId, helmChartSpecificationId));
  }

  @Override
  public void deleteHelmChartSpecification(HelmChartSpecification helmChartSpecification) {
    if (helmChartSpecification == null) {
      return;
    }

    String appId = helmChartSpecification.getAppId();
    String accountId = appService.getAccountIdByAppId(helmChartSpecification.getAppId());
    Service service = getWithDetails(appId, helmChartSpecification.getServiceId());
    wingsPersistence.delete(HelmChartSpecification.class, appId, helmChartSpecification.getUuid());
    yamlPushService.pushYamlChangeSet(
        accountId, service, helmChartSpecification, Type.DELETE, helmChartSpecification.isSyncFromGit());
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
    upsertPCFSpecInManifestFile(pcfServiceSpecification);

    String appId = persistedPcfServiceSpecification.getAppId();
    String accountId = appService.getAccountIdByAppId(appId);
    Service service = getWithDetails(appId, persistedPcfServiceSpecification.getServiceId());

    Type type = isCreate ? Type.CREATE : Type.UPDATE;
    yamlPushService.pushYamlChangeSet(
        accountId, service, persistedPcfServiceSpecification, type, pcfServiceSpecification.isSyncFromGit());

    return persistedPcfServiceSpecification;
  }

  @Override
  public void deletePCFServiceSpecification(String appId, String pCFServiceSpecificationId) {
    PcfServiceSpecification pcfServiceSpecification =
        wingsPersistence.get(PcfServiceSpecification.class, pCFServiceSpecificationId);
    if (pcfServiceSpecification == null) {
      return;
    }

    String accountId = appService.getAccountIdByAppId(appId);
    Service service = getWithDetails(appId, pcfServiceSpecification.getServiceId());

    if (wingsPersistence.delete(PcfServiceSpecification.class, appId, pCFServiceSpecificationId)) {
      yamlPushService.pushYamlChangeSet(
          accountId, service, pcfServiceSpecification, Type.DELETE, pcfServiceSpecification.isSyncFromGit());
    }
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
    Service service = wingsPersistence.getWithAppId(Service.class, appId, serviceId);
    notNullCheck("service", service);

    verifyDuplicateServiceCommandName(appId, serviceId, serviceCommand);
    validateCommandName(serviceCommand.getCommand());
    addServiceCommand(appId, serviceId, serviceCommand, pushToYaml);

    return getWithDetails(appId, serviceId);
  }

  @Override
  public Service updateCommandsOrder(String appId, String serviceId, List<ServiceCommand> serviceCommands) {
    Service service = wingsPersistence.getWithAppId(Service.class, appId, serviceId);
    notNullCheck("service", service);
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
    return getWithDetails(appId, serviceId);
  }

  private void validateCommandName(Command command) {
    if (command != null) {
      if (!EntityNameValidator.isValid(command.getName())) {
        throw new InvalidRequestException("Command Name can only have characters -, _, a-z, A-Z, 0-9 and space");
      }
    }
  }

  private void addServiceCommand(String appId, String serviceId, ServiceCommand serviceCommand, boolean pushToYaml) {
    serviceCommand.setDefaultVersion(1);
    serviceCommand.setServiceId(serviceId);
    serviceCommand.setAppId(appId);
    String notes = serviceCommand.getNotes();
    Command command = serviceCommand.getCommand();
    if (serviceCommand.getTemplateUuid() != null) {
      command = (Command) templateService.constructEntityFromTemplate(
          serviceCommand.getTemplateUuid(), serviceCommand.getTemplateVersion(), EntityType.COMMAND);
      command.setAppId(appId);
      if (isNotEmpty(serviceCommand.getName())) {
        command.setName(serviceCommand.getName());
      }
    } else if (Objects.isNull(command)) {
      throw new InvalidRequestException(format("Underlying command is null for service command "
              + "%s[%s]",
          serviceCommand.getName(), serviceCommand.getUuid()));
    } else if (serviceCommand.getCommand().getGraph() != null) {
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

    command.setSyncFromGit(serviceCommand.isSyncFromGit());

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
    command.setGraph(null);
    commandService.save(command, pushToYaml);
  }

  private boolean isLinearCommandGraph(ServiceCommand serviceCommand) {
    try {
      return serviceCommand.getCommand().getGraph().isLinear();
    } catch (Exception ex) {
      logger.error("Exception in validating command graph " + serviceCommand.getCommand(), ex);
      return false;
    }
  }

  @Override
  public Service updateCommand(String appId, String serviceId, ServiceCommand serviceCommand) {
    return updateCommand(appId, serviceId, serviceCommand, false);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Service updateCommand(String appId, String serviceId, ServiceCommand serviceCommand, boolean fromTemplate) {
    Service service = wingsPersistence.getWithAppId(Service.class, appId, serviceId);
    notNullCheck("Service was deleted", service, USER);

    ServiceCommand savedServiceCommand = commandService.getServiceCommand(appId, serviceCommand.getUuid());
    if (!StringUtils.equals(savedServiceCommand.getName(), serviceCommand.getName())) {
      verifyDuplicateServiceCommandName(appId, serviceId, serviceCommand);
    }

    UpdateOperations<ServiceCommand> updateOperation = wingsPersistence.createUpdateOperations(ServiceCommand.class);

    EntityVersion lastEntityVersion =
        entityVersionService.lastEntityVersion(appId, EntityType.COMMAND, serviceCommand.getUuid(), serviceId);
    if (updateLinkedTemplateServiceCommand(appId, serviceCommand, updateOperation, lastEntityVersion, fromTemplate)) {
      updateCommandInternal(appId, serviceId, serviceCommand, lastEntityVersion, false, true);
    } else if (serviceCommand.getCommand() != null) {
      validateCommandName(serviceCommand.getCommand());
      updateCommandInternal(appId, serviceId, serviceCommand, lastEntityVersion, false, fromTemplate);
    } else {
      logger.info(
          "Underlying command is null for service command {}[{}]", serviceCommand.getName(), serviceCommand.getUuid());
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

    boolean syncFromGit = serviceCommand.isSyncFromGit();
    serviceCommand = commandService.getServiceCommand(appId, serviceCommand.getUuid());

    boolean isRename = !savedServiceCommand.getName().equals(serviceCommand.getName());
    yamlPushService.pushYamlChangeSet(
        accountId, savedServiceCommand, serviceCommand, Type.UPDATE, syncFromGit, isRename);

    return getWithDetails(appId, serviceId);
  }

  private boolean updateLinkedTemplateServiceCommand(String appId, ServiceCommand serviceCommand,
      UpdateOperations<ServiceCommand> updateOperation, EntityVersion lastEntityVersion, boolean fromTemplate) {
    if (serviceCommand.getTemplateUuid() != null && serviceCommand.getTemplateVersion() != null) {
      ServiceCommand oldServiceCommand = commandService.getServiceCommand(appId, serviceCommand.getUuid());
      notNullCheck("Service command [" + serviceCommand + "] does not exist", oldServiceCommand, USER);
      if (oldServiceCommand.getTemplateUuid() != null && oldServiceCommand.getTemplateVersion() != null) {
        if (!serviceCommand.getTemplateVersion().equals(oldServiceCommand.getTemplateVersion())) {
          Template template =
              templateService.get(serviceCommand.getTemplateUuid(), serviceCommand.getTemplateVersion());
          notNullCheck("Linked template does  not exist", template, USER);
          SshCommandTemplate sshCommandTemplate = (SshCommandTemplate) template.getTemplateObject();
          Command newcommand = aCommand().build();
          newcommand.setOriginEntityId(serviceCommand.getUuid());
          newcommand.setAppId(appId);
          newcommand.setTemplateVariables(template.getVariables());
          newcommand.setCommandType(sshCommandTemplate.getCommandType());
          newcommand.setCommandUnits(sshCommandTemplate.getCommandUnits());
          serviceCommand.setCommand(newcommand);
          updateOperation.set("templateVersion", serviceCommand.getTemplateVersion());
        } else {
          if (serviceCommand.getCommand() != null) {
            if (!fromTemplate) {
              Command oldCommand =
                  commandService.getCommand(appId, serviceCommand.getUuid(), lastEntityVersion.getVersion());
              notNullCheck("Command" + serviceCommand.getName() + "] does not exist", oldCommand, USER);
              serviceCommand.getCommand().setCommandUnits(oldCommand.getCommandUnits());
              return false;
            }
          } else {
            return false;
          }
        }
        return true;
      }
    }
    return false;
  }

  private void updateCommandInternal(String appId, String serviceId, ServiceCommand serviceCommand,
      EntityVersion lastEntityVersion, boolean pushToYaml, boolean fromTemplate) {
    Command newcommand = aCommand().build();
    Command command = serviceCommand.getCommand();

    newcommand.setCommandUnits(command.getCommandUnits());
    newcommand.setCommandType(command.getCommandType());
    if (isEmpty(serviceCommand.getName())) {
      serviceCommand.setName(command.getName());
      newcommand.setName(command.getName());
    } else {
      newcommand.setName(serviceCommand.getName());
    }
    newcommand.setTemplateVariables(command.getTemplateVariables());
    newcommand.setOriginEntityId(serviceCommand.getUuid());
    newcommand.setAppId(appId);
    newcommand.setUuid(null);

    Command oldCommand = commandService.getCommand(appId, serviceCommand.getUuid(), lastEntityVersion.getVersion());
    notNullCheck("Command" + serviceCommand.getName() + "] does not exist", oldCommand, USER);

    DiffNode commandUnitDiff =
        ObjectDifferBuilder.buildDefault().compare(newcommand.getCommandUnits(), oldCommand.getCommandUnits());

    boolean variablesChanged =
        templateHelper.variablesChanged(newcommand.getTemplateVariables(), oldCommand.getTemplateVariables());
    if (variablesChanged) {
      newcommand.setTemplateVariables(templateHelper.overrideVariables(
          newcommand.getTemplateVariables(), oldCommand.getTemplateVariables(), fromTemplate));
    }

    if (commandUnitDiff.hasChanges()
        || isCommandUnitsSizeChanged(newcommand.getCommandUnits(), oldCommand.getCommandUnits())
        || isCommandUnitsOrderChanged(newcommand.getCommandUnits(), oldCommand.getCommandUnits()) || variablesChanged
        || fromTemplate) {
      EntityVersion entityVersion =
          entityVersionService.newEntityVersion(appId, EntityType.COMMAND, serviceCommand.getUuid(), serviceId,
              serviceCommand.getName(), EntityVersion.ChangeType.UPDATED, serviceCommand.getNotes());
      newcommand.setVersion(Long.valueOf(entityVersion.getVersion().intValue()));
      if (newcommand.getDeploymentType() == null) {
        // Copy the old newcommand values
        newcommand.setDeploymentType(oldCommand.getDeploymentType());
      }
      if (newcommand.getCommandType() == null) {
        newcommand.setCommandType(oldCommand.getCommandType());
      } else {
        newcommand.setCommandType(command.getCommandType());
      }
      if (newcommand.getName() == null) {
        newcommand.setName(oldCommand.getName());
      }
      if (newcommand.getArtifactType() == null) {
        newcommand.setArtifactType(oldCommand.getArtifactType());
      }
      commandService.save(newcommand, pushToYaml);
      if (serviceCommand.getSetAsDefault() || fromTemplate) {
        serviceCommand.setDefaultVersion(entityVersion.getVersion());
      }
    } else {
      // Check if Name and CommandType changes
      if (!oldCommand.getName().equals(newcommand.getName())
          || !oldCommand.getCommandType().equals(newcommand.getCommandType())) {
        UpdateOperations<Command> commandUpdateOperations = wingsPersistence.createUpdateOperations(Command.class);
        setUnset(commandUpdateOperations, "name", newcommand.getName());
        setUnset(commandUpdateOperations, "commandType", newcommand.getCommandType());
        wingsPersistence.update(
            wingsPersistence.createQuery(Command.class).filter(ID_KEY, oldCommand.getUuid()), commandUpdateOperations);
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ServiceCommand getCommandByName(
      @NotEmpty String appId, @NotEmpty String serviceId, @NotEmpty String commandName) {
    Service service = getServiceWithServiceCommands(appId, serviceId);
    return service.getServiceCommands()
        .stream()
        .filter(command -> equalsIgnoreCase(commandName, command.getName()))
        .findFirst()
        .orElse(null);
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
    Optional<ServiceCommand> commandOptional =
        getServiceCommands(appId, serviceId, false)
            .stream()
            .filter(serviceCommand -> equalsIgnoreCase(commandName, serviceCommand.getName()))
            .findFirst();
    if (commandOptional.isPresent()) {
      ServiceCommand command = commandOptional.get();
      command.setCommand(commandService.getCommand(appId, command.getUuid(), version));
      return command;
    } else {
      throw new InvalidRequestException(format("Could not find command %s version %s", commandName, version));
    }
  }

  @Override
  public Service getServiceWithServiceCommands(String appId, String serviceId) {
    return getServiceWithServiceCommands(appId, serviceId, true);
  }

  @Override
  public Service getServiceWithServiceCommands(String appId, String serviceId, boolean shouldThrow) {
    Service service = wingsPersistence.getWithAppId(Service.class, appId, serviceId);
    if (shouldThrow) {
      notNullCheck("service", service);
    } else if (service == null) {
      return null;
    }
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
    Service service = getWithDetails(appId, persistedUserDataSpec.getServiceId());

    Type type = isCreate ? Type.CREATE : Type.UPDATE;
    yamlPushService.pushYamlChangeSet(
        accountId, service, persistedUserDataSpec, type, userDataSpecification.isSyncFromGit());

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
        .filter(UserDataSpecificationKeys.serviceId, serviceId)
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
        .filter(ContainerTaskKeys.serviceId, serviceId)
        .filter(ContainerTaskKeys.deploymentType, deploymentType)
        .get();
  }

  @Override
  public ContainerTask getContainerTaskById(String appId, String containerTaskId) {
    return wingsPersistence.getWithAppId(ContainerTask.class, appId, containerTaskId);
  }

  @Override
  public HelmChartSpecification getHelmChartSpecification(String appId, String serviceId) {
    return wingsPersistence.createQuery(HelmChartSpecification.class)
        .filter("appId", appId)
        .filter(HelmChartSpecificationKeys.serviceId, serviceId)
        .get();
  }

  @Override
  public PcfServiceSpecification getPcfServiceSpecification(String appId, String serviceId) {
    return wingsPersistence.createQuery(PcfServiceSpecification.class)
        .filter("appId", appId)
        .filter(PcfServiceSpecificationKeys.serviceId, serviceId)
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
    return wingsPersistence.getWithAppId(HelmChartSpecification.class, appId, helmChartSpecificationId);
  }

  @Override
  public PcfServiceSpecification getPcfServiceSpecificationById(String appId, String pcfServiceSpecificationId) {
    return wingsPersistence.getWithAppId(PcfServiceSpecification.class, appId, pcfServiceSpecificationId);
  }

  @Override
  public LambdaSpecification getLambdaSpecificationById(String appId, String lambdaSpecificationId) {
    return wingsPersistence.getWithAppId(LambdaSpecification.class, appId, lambdaSpecificationId);
  }

  @Override
  public UserDataSpecification getUserDataSpecificationById(String appId, String userDataSpecificationId) {
    return wingsPersistence.getWithAppId(UserDataSpecification.class, appId, userDataSpecificationId);
  }

  @Override
  public Map<String, String> getData(String appId, Map<String, String> params) {
    Service service = wingsPersistence.getWithAppId(Service.class, appId, params.get(EntityType.SERVICE.name()));
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
      throw new InvalidRequestException(
          "Function name should be unique. Duplicate function names: [" + join(",", duplicateFunctionName) + "]");
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
    Service service = getWithDetails(appId, persistedLambdaSpec.getServiceId());

    Type type = isCreate ? Type.CREATE : Type.UPDATE;
    yamlPushService.pushYamlChangeSet(
        accountId, service, persistedLambdaSpec, type, lambdaSpecification.isSyncFromGit());

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
        .filter(LambdaSpecificationKeys.serviceId, serviceId)
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
    notNullCheck("Service", savedService);

    String configMapYaml = trimYaml(kubernetesPayload.getAdvancedConfig());
    UpdateOperations<Service> updateOperations;
    if (isNotBlank(configMapYaml)) {
      updateOperations = wingsPersistence.createUpdateOperations(Service.class).set("configMapYaml", configMapYaml);
    } else {
      updateOperations = wingsPersistence.createUpdateOperations(Service.class).unset("configMapYaml");
    }

    wingsPersistence.update(savedService, updateOperations);
    Service updatedService = get(appId, serviceId, false);

    String accountId = appService.getAccountIdByAppId(updatedService.getAppId());
    yamlPushService.pushYamlChangeSet(accountId, updatedService, updatedService, Type.UPDATE, false, false);

    return updatedService;
  }

  @Override
  public Service setHelmValueYaml(String appId, String serviceId, KubernetesPayload kubernetesPayload) {
    ApplicationManifest applicationManifest =
        applicationManifestService.getByServiceId(appId, serviceId, AppManifestKind.VALUES);

    ManifestFile manifestFile = null;
    if (applicationManifest != null) {
      manifestFile =
          applicationManifestService.getManifestFileByFileName(applicationManifest.getUuid(), VALUES_YAML_KEY);
    }

    if (manifestFile == null) {
      manifestFile = ManifestFile.builder().build();
      manifestFile.setFileContent(kubernetesPayload.getAdvancedConfig());

      createValuesYaml(appId, serviceId, manifestFile);
    } else {
      manifestFile.setFileContent(kubernetesPayload.getAdvancedConfig());
      updateValuesYaml(appId, serviceId, manifestFile.getUuid(), manifestFile);
    }

    return get(appId, serviceId, false);
  }

  @Override
  public Service deleteHelmValueYaml(String appId, String serviceId) {
    ApplicationManifest applicationManifest =
        applicationManifestService.getByServiceId(appId, serviceId, AppManifestKind.VALUES);

    if (applicationManifest != null) {
      applicationManifestService.deleteAppManifest(appId, applicationManifest.getUuid());
    }

    deleteHelmValuesFromService(appId, serviceId);

    return get(appId, serviceId, false);
  }

  @Override
  public List<Service> fetchServicesByUuids(String appId, List<String> serviceUuids) {
    if (isNotEmpty(serviceUuids)) {
      serviceUuids = serviceUuids.stream().distinct().collect(toList());
      List<Service> services = wingsPersistence.createQuery(Service.class)
                                   .project("appContainer", false)
                                   .filter(ServiceKeys.appId, appId)
                                   .field("uuid")
                                   .in(serviceUuids)
                                   .asList();

      List<Service> orderedServices = new ArrayList<>();
      Map<String, Service> servicesMap =
          services.stream().collect(Collectors.toMap(Service::getUuid, Function.identity()));
      for (String serviceId : serviceUuids) {
        if (servicesMap.containsKey(serviceId)) {
          orderedServices.add(servicesMap.get(serviceId));
        }
      }
      return orderedServices;
    }
    return new ArrayList<>();
  }

  @Override
  public List<Service> fetchServicesByUuidsByAccountId(String accountId, List<String> serviceUuids) {
    if (isNotEmpty(serviceUuids)) {
      serviceUuids = serviceUuids.stream().distinct().collect(toList());
      List<Service> services = wingsPersistence.createQuery(Service.class)
                                   .project(ServiceKeys.appContainer, false)
                                   .filter(ServiceKeys.accountId, accountId)
                                   .field(ServiceKeys.uuid)
                                   .in(serviceUuids)
                                   .asList();

      List<Service> orderedServices = new ArrayList<>();
      Map<String, Service> servicesMap =
          services.stream().collect(Collectors.toMap(Service::getUuid, Function.identity()));
      for (String serviceId : serviceUuids) {
        if (servicesMap.containsKey(serviceId)) {
          orderedServices.add(servicesMap.get(serviceId));
        }
      }
      return orderedServices;
    }
    return new ArrayList<>();
  }

  @Override
  public Artifact findPreviousArtifact(String appId, String workflowExecutionId, ContextElement instanceElement) {
    FindOptions findOptions = new FindOptions();
    if (workflowExecutionService.checkIfOnDemand(appId, workflowExecutionId)) {
      findOptions = findOptions.skip(1);
    }
    Activity activity = wingsPersistence.createQuery(Activity.class)
                            .filter(ActivityKeys.appId, appId)
                            .filter(ActivityKeys.serviceInstanceId, instanceElement.getUuid())
                            .filter(ActivityKeys.status, ExecutionStatus.SUCCESS)
                            .field(ActivityKeys.workflowExecutionId)
                            .notEqual(workflowExecutionId)
                            .field(ActivityKeys.artifactId)
                            .exists()
                            .order(Sort.descending(ActivityKeys.createdAt))
                            .get(findOptions);

    if (activity == null) {
      return null;
    }
    return artifactService.getWithSource(activity.getArtifactId());
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

  private boolean isCommandUnitsSizeChanged(List<CommandUnit> commandUnits, List<CommandUnit> oldCommandUnits) {
    if (commandUnits != null && oldCommandUnits != null) {
      if (commandUnits.size() != oldCommandUnits.size()) {
        return true;
      }
    }
    return false;
  }

  private void setArtifactStreams(List<Service> services) {
    services.forEach(service
        -> service.setArtifactStreams(
            artifactStreamService.listByIds(artifactStreamServiceBindingService.listArtifactStreamIds(service))));
  }

  public void setServiceCommands(List<Service> services) {
    List<String> serviceIds = services.stream().map(service -> service.getUuid()).collect(toList());
    ArrayListMultimap<String, ServiceCommand> serviceToServiceCommandMap = ArrayListMultimap.create();
    try (HIterator<ServiceCommand> iterator =
             new HIterator<>(wingsPersistence.createQuery(ServiceCommand.class, excludeAuthority)
                                 .field("serviceId")
                                 .in(serviceIds)
                                 .fetch())) {
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

  private List<ContainerTask> findContainerTaskForService(String appId, String serviceId) {
    PageRequest<ContainerTask> pageRequest =
        aPageRequest().addFilter("appId", EQ, appId).addFilter("serviceId", EQ, serviceId).build();
    return wingsPersistence.query(ContainerTask.class, pageRequest).getResponse();
  }

  private Service createDefaultHelmValueYaml(Service service, boolean createdFromYaml) {
    if (createdFromYaml) {
      return service;
    }

    if (HELM.equals(service.getDeploymentType())) {
      ManifestFile manifestFile = ManifestFile.builder().fileContent(DEFAULT_HELM_VALUE_YAML).build();

      createValuesYaml(service.getAppId(), service.getUuid(), manifestFile);
      service.setHelmValueYaml(DEFAULT_HELM_VALUE_YAML);
    }

    return service;
  }

  @Override
  public boolean checkArtifactNeededForHelm(String appId, String serviceTemplateId) {
    List<String> valueOverridesYamlFiles = applicationManifestUtils.getHelmValuesYamlFiles(appId, serviceTemplateId);
    if (isEmpty(valueOverridesYamlFiles)) {
      return false;
    }

    for (String valueYamlFile : valueOverridesYamlFiles) {
      if (HelmHelper.isArtifactReferencedInValuesYaml(valueYamlFile)) {
        return true;
      }
    }

    return false;
  }

  @Override
  public void updateArtifactVariableNamesForHelm(
      String appId, String serviceTemplateId, Set<String> serviceArtifactVariableNames) {
    List<String> valueOverridesYamlFiles = applicationManifestUtils.getHelmValuesYamlFiles(appId, serviceTemplateId);
    if (isEmpty(valueOverridesYamlFiles)) {
      return;
    }

    for (String valueYamlFile : valueOverridesYamlFiles) {
      HelmHelper.updateArtifactVariableNamesReferencedInValuesYaml(valueYamlFile, serviceArtifactVariableNames);
    }
  }

  private void createDefaultK8sManifests(Service service, boolean createdFromGit) {
    if (createdFromGit || !service.isK8sV2()) {
      return;
    }

    if (applicationManifestService.getManifestByServiceId(service.getAppId(), service.getUuid()) != null) {
      return;
    }

    ApplicationManifest applicationManifest = ApplicationManifest.builder()
                                                  .serviceId(service.getUuid())
                                                  .storeType(StoreType.Local)
                                                  .kind(AppManifestKind.K8S_MANIFEST)
                                                  .build();
    applicationManifest.setAppId(service.getAppId());

    applicationManifestService.create(applicationManifest);

    ManifestFile defaultDeploymentSpec =
        ManifestFile.builder().fileName("templates/deployment.yaml").fileContent(default_k8s_deployment_yaml).build();
    defaultDeploymentSpec.setAppId(service.getAppId());
    applicationManifestService.createManifestFileByServiceId(defaultDeploymentSpec, service.getUuid());

    ManifestFile defaultNamespaceSpec =
        ManifestFile.builder().fileName("templates/namespace.yaml").fileContent(default_k8s_namespace_yaml).build();
    defaultNamespaceSpec.setAppId(service.getAppId());
    applicationManifestService.createManifestFileByServiceId(defaultNamespaceSpec, service.getUuid());

    ManifestFile defaultServiceSpec =
        ManifestFile.builder().fileName("templates/service.yaml").fileContent(default_k8s_service_yaml).build();
    defaultServiceSpec.setAppId(service.getAppId());
    applicationManifestService.createManifestFileByServiceId(defaultServiceSpec, service.getUuid());

    ManifestFile defaultValues =
        ManifestFile.builder().fileName("values.yaml").fileContent(default_k8s_values_yaml).build();
    defaultValues.setAppId(service.getAppId());
    applicationManifestService.createManifestFileByServiceId(defaultValues, service.getUuid());
  }

  private void createDefaultPCFManifestsIfApplicable(Service service, boolean createdFromGit) {
    if (!PCF.equals(service.getDeploymentType()) || createdFromGit) {
      return;
    }

    if (featureFlagService.isEnabled(FeatureName.INFRA_MAPPING_REFACTOR, service.getAccountId())) {
      createDefaultPcfV2Manifests(service);
    } else {
      createDefaultPcfSpec(service);
    }
  }

  private void createDefaultPcfSpec(Service service) {
    PcfServiceSpecification pcfServiceSpecification =
        PcfServiceSpecification.builder().serviceId(service.getUuid()).build();
    pcfServiceSpecification.setAppId(service.getAppId());
    pcfServiceSpecification.resetToDefaultManifestSpecification();

    createPcfServiceSpecification(pcfServiceSpecification);
  }

  @Override
  public void createDefaultPcfV2Manifests(Service service) {
    if (applicationManifestService.getManifestByServiceId(service.getAppId(), service.getUuid()) != null) {
      return;
    }

    ApplicationManifest applicationManifest = ApplicationManifest.builder()
                                                  .serviceId(service.getUuid())
                                                  .storeType(StoreType.Local)
                                                  .kind(AppManifestKind.K8S_MANIFEST)
                                                  .build();
    applicationManifest.setAppId(service.getAppId());

    applicationManifestService.create(applicationManifest);

    ManifestFile defaultManifestSpec =
        ManifestFile.builder().fileName(MANIFEST_YML).fileContent(default_pcf_manifest_yml).build();
    defaultManifestSpec.setAppId(service.getAppId());
    applicationManifestService.createManifestFileByServiceId(defaultManifestSpec, service.getUuid());

    ManifestFile varsManifestSpec = ManifestFile.builder().fileName(VARS_YML).fileContent(default_pcf_vars_yml).build();
    varsManifestSpec.setAppId(service.getAppId());
    applicationManifestService.createManifestFileByServiceId(varsManifestSpec, service.getUuid());
  }

  /*
    Remove this method once the PCFSpec migration to manifest files is done
   */
  @Override
  public void upsertPCFSpecInManifestFile(PcfServiceSpecification pcfServiceSpecification) {
    try {
      String serviceId = pcfServiceSpecification.getServiceId();
      String appId = pcfServiceSpecification.getAppId();

      ApplicationManifest applicationManifest = applicationManifestService.getManifestByServiceId(appId, serviceId);

      if (applicationManifest == null) {
        applicationManifest = ApplicationManifest.builder()
                                  .serviceId(serviceId)
                                  .storeType(StoreType.Local)
                                  .kind(AppManifestKind.K8S_MANIFEST)
                                  .build();
        applicationManifest.setAppId(appId);
        applicationManifest = applicationManifestService.create(applicationManifest);
      }

      ManifestFile manifestFile =
          applicationManifestService.getManifestFileByFileName(applicationManifest.getUuid(), MANIFEST_YML);

      if (manifestFile == null) {
        manifestFile =
            ManifestFile.builder().fileName(MANIFEST_YML).applicationManifestId(applicationManifest.getUuid()).build();
        manifestFile.setAppId(appId);
      }

      manifestFile.setFileContent(pcfServiceSpecification.getManifestYaml());

      boolean isCreate = isBlank(manifestFile.getUuid());
      applicationManifestService.upsertApplicationManifestFile(manifestFile, applicationManifest, isCreate);
    } catch (Exception ex) {
      logger.warn("Failed to update the manifest file for PCF spec. ", ex);
    }
  }

  private void cloneAppManifests(String appId, String clonedServiceId, String originalServiceId) {
    List<ApplicationManifest> applicationManifests =
        applicationManifestService.listAppManifests(appId, originalServiceId);
    if (isEmpty(applicationManifests)) {
      return;
    }
    for (ApplicationManifest applicationManifest : applicationManifests) {
      ApplicationManifest applicationManifestNew = applicationManifest.cloneInternal();
      applicationManifestNew.setServiceId(clonedServiceId);
      applicationManifestService.create(applicationManifestNew);

      applicationManifestService.cloneManifestFiles(appId, applicationManifest, applicationManifestNew);
    }
  }

  @Override
  public DeploymentType getDeploymentType(InfrastructureMapping infraMapping, Service service, String serviceId) {
    if (service != null && service.getDeploymentType() != null) {
      return service.getDeploymentType();
    }

    if (isNotBlank(serviceId)) {
      service = get(infraMapping.getAppId(), serviceId, false);
      if (service != null && service.getDeploymentType() != null) {
        return service.getDeploymentType();
      }
    }

    if (isBlank(infraMapping.getDeploymentType())) {
      String msg = new StringBuilder("Deployment type does not exist for")
                       .append((service != null) ? " serviceId " + service.getUuid() : "")
                       .append(isNotBlank(serviceId) ? " serviceId " + serviceId : "")
                       .append(" infraMappingId " + infraMapping.getUuid())
                       .toString();

      throw new InvalidRequestException(msg, USER);
    }

    return valueOf(infraMapping.getDeploymentType());
  }

  @Override
  public void setK8v2ServiceFromAppManifest(
      ApplicationManifest applicationManifest, AppManifestSource appManifestSource) {
    if (!AppManifestSource.SERVICE.equals(appManifestSource)) {
      return;
    }

    if (exists(applicationManifest.getAppId(), applicationManifest.getServiceId())) {
      UpdateOperations<Service> updateOperations =
          wingsPersistence.createUpdateOperations(Service.class).set("isK8sV2", true);

      Query<Service> query = wingsPersistence.createQuery(Service.class)
                                 .filter(Service.APP_ID_KEY, applicationManifest.getAppId())
                                 .filter(Service.ID_KEY, applicationManifest.getServiceId())
                                 .filter(ServiceKeys.deploymentType, KUBERNETES.name());

      wingsPersistence.update(query, updateOperations);
    }
  }

  @Override
  public void setPcfV2ServiceFromAppManifestIfRequired(
      ApplicationManifest applicationManifest, AppManifestSource appManifestSource) {
    if (!AppManifestSource.SERVICE.equals(appManifestSource)) {
      return;
    }

    if (exists(applicationManifest.getAppId(), applicationManifest.getServiceId())) {
      UpdateOperations<Service> updateOperations =
          wingsPersistence.createUpdateOperations(Service.class).set(ServiceKeys.isPcfV2, true);

      Query<Service> query = wingsPersistence.createQuery(Service.class)
                                 .filter(Service.APP_ID_KEY, applicationManifest.getAppId())
                                 .filter(Service.ID_KEY, applicationManifest.getServiceId())
                                 .filter(ServiceKeys.deploymentType, PCF.name());

      wingsPersistence.update(query, updateOperations);
    }
  }

  @Override
  public ManifestFile createValuesYaml(String appId, String serviceId, ManifestFile manifestFile) {
    ApplicationManifest appManifest =
        applicationManifestService.getAppManifest(appId, null, serviceId, AppManifestKind.VALUES);
    if (appManifest == null) {
      appManifest = ApplicationManifest.builder()
                        .storeType(StoreType.Local)
                        .serviceId(serviceId)
                        .kind(AppManifestKind.VALUES)
                        .build();
      appManifest.setAppId(appId);
      appManifest = applicationManifestService.create(appManifest);
    }
    manifestFile.setAppId(appId);
    manifestFile.setFileName(VALUES_YAML_KEY);
    manifestFile.setApplicationManifestId(appManifest.getUuid());
    return applicationManifestService.upsertApplicationManifestFile(manifestFile, appManifest, true);
  }

  @Override
  public ManifestFile getValuesYaml(String appId, String serviceId, String manifestFileId) {
    return applicationManifestService.getManifestFileById(appId, manifestFileId);
  }

  @Override
  public ManifestFile updateValuesYaml(
      String appId, String serviceId, String manifestFileId, ManifestFile manifestFile) {
    ApplicationManifest appManifest =
        applicationManifestService.getAppManifest(appId, null, serviceId, AppManifestKind.VALUES);
    if (appManifest == null) {
      throw new InvalidRequestException("Application Manifest not found", USER);
    }
    ManifestFile savedManifestFIle = applicationManifestService.getManifestFileById(appId, manifestFileId);
    if (savedManifestFIle == null) {
      throw new InvalidRequestException("Manifest file does not exist with given id", USER);
    }
    manifestFile.setUuid(manifestFileId);
    manifestFile.setAppId(appId);
    manifestFile.setFileName(VALUES_YAML_KEY);
    manifestFile.setApplicationManifestId(appManifest.getUuid());
    return applicationManifestService.upsertApplicationManifestFile(manifestFile, appManifest, false);
  }

  @Override
  public void deleteValuesYaml(String appId, String serviceId, String manifestFileId) {
    applicationManifestService.deleteManifestFileById(appId, manifestFileId);
  }

  @Override
  public ApplicationManifest createValuesAppManifest(String appId, String serviceId, ApplicationManifest appManifest) {
    if (isNotEmpty(appManifest.getUuid())) {
      throw new InvalidRequestException("Application Manifest already has an id", USER);
    }
    appManifest.setServiceId(serviceId);
    appManifest.setAppId(appId);
    return applicationManifestService.create(appManifest);
  }

  @Override
  public ApplicationManifest getValuesAppManifest(String appId, String serviceId, String appManifestId) {
    return applicationManifestService.getById(appId, appManifestId);
  }

  @Override
  public ApplicationManifest updateValuesAppManifest(
      String appId, String serviceId, String appManifestId, ApplicationManifest applicationManifest) {
    ApplicationManifest savedAppManifest = applicationManifestService.getById(appId, appManifestId);
    if (savedAppManifest == null) {
      throw new InvalidRequestException("No App Manifest exists with the given id", USER);
    }
    applicationManifest.setUuid(appManifestId);
    applicationManifest.setServiceId(savedAppManifest.getServiceId());
    applicationManifest.setAppId(appId);
    return applicationManifestService.update(applicationManifest);
  }

  @Override
  public void deleteValuesAppManifest(String appId, String serviceId, String appManifestId) {
    applicationManifestService.deleteAppManifest(appId, appManifestId);
  }

  private Service updateServiceWithHelmValues(Service service) {
    if (service == null) {
      return null;
    }

    service.setHelmValueYaml(null);

    ApplicationManifest appManifest =
        applicationManifestService.getAppManifest(service.getAppId(), null, service.getUuid(), AppManifestKind.VALUES);
    if (appManifest != null) {
      ManifestFile manifestFile =
          applicationManifestService.getManifestFileByFileName(appManifest.getUuid(), VALUES_YAML_KEY);
      if (manifestFile != null) {
        service.setHelmValueYaml(manifestFile.getFileContent());
      }
    }

    return service;
  }

  @Override
  public Service getWithHelmValues(String appId, String serviceId, SetupStatus status) {
    Service service = getWithDetails(appId, serviceId);

    return updateServiceWithHelmValues(service);
  }

  @Override
  public Service updateWithHelmValues(Service service) {
    service = update(service, false);

    return updateServiceWithHelmValues(service);
  }

  private void deleteHelmValuesFromService(String appId, String serviceId) {
    UpdateOperations<Service> updateOperations =
        wingsPersistence.createUpdateOperations(Service.class).unset("helmValueYaml");

    Query<Service> query =
        wingsPersistence.createQuery(Service.class).filter(Service.APP_ID_KEY, appId).filter(Service.ID_KEY, serviceId);

    wingsPersistence.update(query, updateOperations);
  }

  // TODO: ASR: remove these methods after refactor

  @Override
  public List<Service> listByArtifactStreamId(String appId, String artifactStreamId) {
    return wingsPersistence.createQuery(Service.class)
        .filter(Service.APP_ID_KEY, appId)
        .field(ServiceKeys.artifactStreamIds)
        .contains(artifactStreamId)
        .asList();
  }

  @Override
  public List<Service> listByArtifactStreamId(String artifactStreamId) {
    return wingsPersistence.createQuery(Service.class, excludeAuthority)
        .field(ServiceKeys.artifactStreamIds)
        .contains(artifactStreamId)
        .asList();
  }

  @Override
  public List<Service> listByDeploymentType(String appId, String deploymentType) {
    List<ArtifactType> supportedArtifactTypes =
        DeploymentType.supportedArtifactTypes.get(DeploymentType.valueOf(deploymentType));
    Query<Service> query = wingsPersistence.createQuery(Service.class).field(ServiceKeys.appId).equal(appId);
    query.or(query.criteria(ServiceKeys.deploymentType).equal(deploymentType),
        query.and(query.criteria(ServiceKeys.deploymentType).equal(null),
            query.criteria(ServiceKeys.artifactType).in(supportedArtifactTypes)));
    return query.asList();
  }

  private void verifyDuplicateServiceCommandName(String appId, String serviceId, ServiceCommand serviceCommand) {
    ServiceCommand existingServiceCommand =
        commandService.getServiceCommandByName(appId, serviceId, serviceCommand.getName());
    if (Objects.nonNull(existingServiceCommand)
        && !StringUtils.equals(serviceCommand.getUuid(), existingServiceCommand.getUuid())) {
      throw new InvalidRequestException(
          format("Service command with name \"%s\" already exists", serviceCommand.getName()), USER);
    }
  }
}
