package software.wings.service.intfc;

import io.harness.validation.Create;
import io.harness.validation.Update;
import org.hibernate.validator.constraints.NotEmpty;
import ru.vyarus.guice.validator.group.annotation.ValidationGroups;
import software.wings.beans.CommandCategory;
import software.wings.beans.LambdaSpecification;
import software.wings.beans.Service;
import software.wings.beans.Setup.SetupStatus;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.command.CommandUnit;
import software.wings.beans.command.ServiceCommand;
import software.wings.beans.container.ContainerTask;
import software.wings.beans.container.HelmChartSpecification;
import software.wings.beans.container.KubernetesPayload;
import software.wings.beans.container.PcfServiceSpecification;
import software.wings.beans.container.UserDataSpecification;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.service.intfc.ownership.OwnedByApplication;
import software.wings.sm.ContextElement;
import software.wings.stencils.Stencil;

import java.util.List;
import javax.validation.Valid;

/**
 * Created by anubhaw on 3/28/16.
 */
public interface ServiceResourceService extends OwnedByApplication {
  /**
   * List.
   *
   * @param pageRequest         the page request
   * @param withBuildSource     the with build source
   * @param withServiceCommands the with service commands
   * @return the page response
   */
  PageResponse<Service> list(PageRequest<Service> pageRequest, boolean withBuildSource, boolean withServiceCommands);

  /**
   * Save.
   *
   * @param service the service
   * @return the service
   */
  @ValidationGroups(Create.class) Service save(@Valid Service service);

  /**
   * Save service.
   *
   * @param service  the service
   * @param createdFromYaml flag indicating if the entity is created from yaml
   * @param createDefaultCommands flag indicating if the default commands needs to be created
   * @return the service
   */
  @ValidationGroups(Create.class)
  Service save(@Valid Service service, boolean createdFromYaml, boolean createDefaultCommands);

  /**
   * Clone service.
   *
   * @param appId             the app id
   * @param originalServiceId the old service id
   * @param clonedService     the service
   * @return the service
   */
  Service clone(String appId, String originalServiceId, Service clonedService);

  boolean hasInternalCommands(Service service);

  /**
   * Update.
   *
   * @param service the service
   * @return the service
   */
  @ValidationGroups(Update.class) Service update(@Valid Service service);

  /**
   * Update.
   *
   * @param service the service
   * @param fromYaml if the update is from yaml
   * @return the service
   */
  @ValidationGroups(Update.class) Service update(@Valid Service service, boolean fromYaml);

  /**
   * Gets the.
   *
   * @param appId     the app id
   * @param serviceId the service id
   * @return the service
   */
  Service get(@NotEmpty String appId, @NotEmpty String serviceId);

  /**
   * Gets the.
   *
   * @param appId     the app id
   * @param serviceId the service id
   * @return the service
   */
  Service get(@NotEmpty String appId, @NotEmpty String serviceId, boolean includeCommands);

  /**
   * Gets service by name.
   *
   * @param appId       the app id
   * @param serviceName the service name
   * @return the service by name
   */
  Service getServiceByName(String appId, String serviceName);

  Service getServiceByName(String appId, String serviceName, boolean withDetails);

  /**
   * Exist boolean.
   *
   * @param appId     the app id
   * @param serviceId the service id
   * @return the boolean
   */
  boolean exist(@NotEmpty String appId, @NotEmpty String serviceId);

  /**
   * Delete.
   *
   * @param appId     the app id
   * @param serviceId the service id
   */
  void delete(@NotEmpty String appId, @NotEmpty String serviceId);

  /**
   * Prune descending objects.
   *
   * @param appId     the app id
   * @param serviceId the service id
   */
  void pruneDescendingEntities(@NotEmpty String appId, @NotEmpty String serviceId);

  /**
   * Adds the command.
   *
   * @param appId            the app id
   * @param serviceId        the service id
   * @param serviceCommand   the command graph
   * @param pushToYaml       flag indicating if command needs to be pushed to yaml
   * @return the service
   */
  Service addCommand(
      @NotEmpty String appId, @NotEmpty String serviceId, ServiceCommand serviceCommand, boolean pushToYaml);

  /**
   * Adds the command.
   *
   * @param appId            the app id
   * @param serviceId        the service id
   * @param serviceCommands   the service commands
   * @return the service
   */
  Service updateCommandsOrder(@NotEmpty String appId, @NotEmpty String serviceId, List<ServiceCommand> serviceCommands);

  /**
   * Update command service.
   *
   * @param appId          the app id
   * @param serviceId      the service id
   * @param serviceCommand the command graph
   * @return the service
   */
  Service updateCommand(String appId, String serviceId, ServiceCommand serviceCommand);

  /**
   * Update command to update linked command from template
   * @param appId
   * @param serviceId
   * @param serviceCommand
   * @param fromTemplate
   * @return
   */
  Service updateCommand(String appId, String serviceId, ServiceCommand serviceCommand, boolean fromTemplate);

  /**
   * Delete command.
   *
   * @param appId     the app id
   * @param serviceId the service id
   * @param commandId the command id
   * @return the service
   */
  Service deleteCommand(@NotEmpty String appId, @NotEmpty String serviceId, @NotEmpty String commandId);

  /**
   * Gets command by name.
   *
   * @param appId       the app id
   * @param serviceId   the service id
   * @param commandName the command name
   * @return the command by name
   */
  ServiceCommand getCommandByName(@NotEmpty String appId, @NotEmpty String serviceId, @NotEmpty String commandName);

  /**
   * Gets command by name.
   *
   * @param appId       the app id
   * @param serviceId   the service id
   * @param envId       the env id
   * @param commandName the command name
   * @return the command by name
   */
  ServiceCommand getCommandByName(
      @NotEmpty String appId, @NotEmpty String serviceId, @NotEmpty String envId, @NotEmpty String commandName);

  /**
   * Gets command by name and version.
   *
   * @param appId       the app id
   * @param serviceId   the service id
   * @param commandName the command name
   * @param version     the version
   * @return the command by name and version
   */
  ServiceCommand getCommandByNameAndVersion(
      @NotEmpty String appId, @NotEmpty String serviceId, @NotEmpty String commandName, int version);

  /**
   * Gets command stencils.
   *
   * @param appId       the app id
   * @param serviceId   the service id
   * @param commandName the command name
   * @return the command stencils
   */
  List<Stencil> getCommandStencils(@NotEmpty String appId, @NotEmpty String serviceId, String commandName);

  /**
   * Gets command stencils. It suppresses the container and AWS Lamda and AMI Stencils
   *
   * @param appId       the app id
   * @param serviceId   the service id
   * @param commandName the command name
   * @return the command stencils
   */
  List<Stencil> getCommandStencils(
      @NotEmpty String appId, @NotEmpty String serviceId, String commandName, boolean onlyScriptCommands);

  /**
   * Find services by app list.
   *
   * @param appId the app id
   * @return the list
   */
  List<Service> findServicesByApp(String appId);

  Artifact findPreviousArtifact(String appId, String workflowExecutionId, ContextElement instanceElement);

  /**
   * Get service.
   *
   * @param appId     the app id
   * @param serviceId the service id
   * @param status    the status
   * @return the service
   */
  Service get(String appId, String serviceId, SetupStatus status);

  ContainerTask createContainerTask(ContainerTask containerTask, boolean advanced);

  void deleteContainerTask(String appId, String containerTaskId);

  ContainerTask updateContainerTask(ContainerTask containerTask, boolean advanced);

  ContainerTask updateContainerTaskAdvanced(
      String appId, String serviceId, String taskId, KubernetesPayload kubernetesPayload, boolean reset);

  PageResponse<ContainerTask> listContainerTasks(PageRequest<ContainerTask> pageRequest);

  List<Stencil> getContainerTaskStencils(@NotEmpty String appId, @NotEmpty String serviceId);

  ContainerTask getContainerTaskByDeploymentType(String appId, String serviceId, String deploymentType);

  ContainerTask getContainerTaskById(String appId, String containerTaskId);

  HelmChartSpecification createHelmChartSpecification(HelmChartSpecification helmChartSpecification);

  void deleteHelmChartSpecification(String appId, String helmChartSpecificationId);

  HelmChartSpecification updateHelmChartSpecification(HelmChartSpecification helmChartSpecification);

  PageResponse<HelmChartSpecification> listHelmChartSpecifications(PageRequest<HelmChartSpecification> pageRequest);

  HelmChartSpecification getHelmChartSpecification(String appId, String serviceId);

  HelmChartSpecification getHelmChartSpecificationById(String appId, String helmChartSpecificationId);

  /**
   * Clone command service.
   *
   * @param appId       the app id
   * @param serviceId   the service id
   * @param commandName the command id
   * @param command     the command
   * @return the service
   */
  Service cloneCommand(String appId, String serviceId, String commandName, ServiceCommand command);

  /**
   * Gets flatten command unit list.
   *
   * @param appId       the app id
   * @param serviceId   the service id
   * @param envId       the env id
   * @param commandName the command name
   * @return the flatten command unit list
   */
  List<CommandUnit> getFlattenCommandUnitList(String appId, String serviceId, String envId, String commandName);

  LambdaSpecification getLambdaSpecificationById(String appId, String lambdaSpecificationId);

  UserDataSpecification getUserDataSpecificationById(String appId, String userDataSpecificationId);

  /**
   * Create lambda specification lambda specification.
   *
   * @param lambdaSpecification the lambda specification
   * @return the lambda specification
   */
  @ValidationGroups(Create.class)
  LambdaSpecification createLambdaSpecification(@Valid LambdaSpecification lambdaSpecification);

  /**
   * Update lambda specification lambda specification.
   *
   * @param lambdaSpecification the lambda specification
   * @return the lambda specification
   */
  @ValidationGroups(Update.class)
  LambdaSpecification updateLambdaSpecification(@Valid LambdaSpecification lambdaSpecification);

  /**
   * Gets lambda specification.
   *
   * @param pageRequest the page request
   * @return the lambda specification
   */
  PageResponse<LambdaSpecification> listLambdaSpecification(PageRequest<LambdaSpecification> pageRequest);

  /**
   * Gets lambda specification.
   *
   * @param appId     the app id
   * @param serviceId the service id
   * @return the lambda specification
   */
  LambdaSpecification getLambdaSpecification(String appId, String serviceId);

  /**
   * Verifies whether services needs artifact or not
   *
   * @param service the service
   * @return the artifact needed or not
   */
  boolean isArtifactNeeded(Service service);

  /**
   * Gets the list of service commands with the commands
   *
   * @param appId
   * @param serviceId
   * @return
   */
  List<ServiceCommand> getServiceCommands(String appId, String serviceId);

  /**
   * Returns the service commands with the Command details
   *
   * @param appId
   * @param serviceId
   * @param withCommandDetails
   * @return
   */
  List<ServiceCommand> getServiceCommands(String appId, String serviceId, boolean withCommandDetails);

  // Gets service with service commands with command details
  Service getServiceWithServiceCommands(String appId, String serviceId);

  @ValidationGroups(Create.class)
  UserDataSpecification createUserDataSpecification(@Valid UserDataSpecification userDataSpecification);

  @ValidationGroups(Update.class)
  UserDataSpecification updateUserDataSpecification(@Valid UserDataSpecification userDataSpecification);

  PageResponse<UserDataSpecification> listUserDataSpecification(PageRequest<UserDataSpecification> pageRequest);

  UserDataSpecification getUserDataSpecification(String appId, String serviceId);

  Service setConfigMapYaml(String appId, String serviceId, KubernetesPayload kubernetesPayload);

  Service setHelmValueYaml(String appId, String serviceId, KubernetesPayload kubernetesPayload);

  // Get services excluding appContainer details
  List<Service> fetchServicesByUuids(String appId, List<String> serviceUuids);

  PcfServiceSpecification getPcfServiceSpecification(String appId, String serviceId);

  PcfServiceSpecification createPcfServiceSpecification(PcfServiceSpecification pcfServiceSpecification);

  void deletePCFServiceSpecification(String appId, String pCFServiceSpecificationId);

  PcfServiceSpecification updatePcfServiceSpecification(PcfServiceSpecification pcfServiceSpecification);

  PcfServiceSpecification getPcfServiceSpecificationById(String appId, String pcfServiceSpecificationId);

  PcfServiceSpecification resetToDefaultPcfServiceSpecification(PcfServiceSpecification pcfServiceSpecification);

  PcfServiceSpecification getExistingOrDefaultPcfServiceSpecification(String appId, String serviceId);

  /***
   * Get command categories equivalent to stencils call
   * @param appId
   * @param serviceId
   * @param commandName
   * @return
   */
  List<CommandCategory> getCommandCategories(
      @NotEmpty String appId, @NotEmpty String serviceId, @NotEmpty String commandName);
}
