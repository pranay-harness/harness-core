package software.wings.service.intfc;

import org.hibernate.validator.constraints.NotEmpty;
import ru.vyarus.guice.validator.group.annotation.ValidationGroups;
import software.wings.beans.container.ContainerTask;
import software.wings.beans.Service;
import software.wings.beans.Setup.SetupStatus;
import software.wings.beans.command.ServiceCommand;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.stencils.Stencil;
import software.wings.utils.validation.Create;
import software.wings.utils.validation.Update;

import java.util.List;
import javax.validation.Valid;

/**
 * Created by anubhaw on 3/28/16.
 */
public interface ServiceResourceService {
  /**
   * List.
   *
   * @param pageRequest the page request
   * @param withBuildSource
   * @return the page response
   */
  PageResponse<Service> list(PageRequest<Service> pageRequest, boolean withBuildSource);

  /**
   * Save.
   *
   * @param service the service
   * @return the service
   */
  @ValidationGroups(Create.class) Service save(@Valid Service service);

  /**
   * Update.
   *
   * @param service the service
   * @return the service
   */
  @ValidationGroups(Update.class) Service update(@Valid Service service);

  /**
   * Gets the.
   *
   * @param appId     the app id
   * @param serviceId the service id
   * @return the service
   */
  Service get(@NotEmpty String appId, @NotEmpty String serviceId);

  /**
   * Get service.
   *
   * @param appId          the app id
   * @param serviceId      the service id
   * @param includeDetails the include details
   * @return the service
   */
  Service get(String appId, String serviceId, boolean includeDetails);

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
   * Adds the command.
   *
   * @param appId          the app id
   * @param serviceId      the service id
   * @param serviceCommand the command graph
   * @return the service
   */
  Service addCommand(@NotEmpty String appId, @NotEmpty String serviceId, ServiceCommand serviceCommand);

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
   * Delete command.
   *
   * @param appId       the app id
   * @param serviceId   the service id
   * @param commandName the command name
   * @return the service
   */
  Service deleteCommand(@NotEmpty String appId, @NotEmpty String serviceId, @NotEmpty String commandName);

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
   * Delete by app id boolean.
   *
   * @param appId the app id
   * @return the boolean
   */
  void deleteByApp(String appId);

  /**
   * Find services by app list.
   *
   * @param appId the app id
   * @return the list
   */
  List<Service> findServicesByApp(String appId);

  /**
   * Get service.
   *
   * @param appId     the app id
   * @param serviceId the service id
   * @param status    the status
   * @return the service
   */
  Service get(String appId, String serviceId, SetupStatus status);

  /**
   * Create container task container task.
   *
   * @param containerTask the container task  @return the container task
   * @return the container task
   */
  ContainerTask createContainerTask(ContainerTask containerTask);

  /**
   * Delete container task.
   *
   * @param appId           the app id
   * @param containerTaskId the container task id
   */
  void deleteContainerTask(String appId, String containerTaskId);

  /**
   * Update container task container task.
   *
   * @param containerTask the container task
   * @return the container task
   */
  ContainerTask updateContainerTask(ContainerTask containerTask);

  /**
   * List container tasks page response.
   *
   * @param pageRequest the page request
   * @return the page response
   */
  PageResponse<ContainerTask> listContainerTasks(PageRequest<ContainerTask> pageRequest);

  /**
   * Gets container task stencils.
   *
   * @param appId     the app id
   * @param serviceId the service id
   * @return the container task stencils
   */
  List<Stencil> getContainerTaskStencils(@NotEmpty String appId, @NotEmpty String serviceId);

  /**
   * Gets container task by deployment type.
   *
   * @param appId          the app id
   * @param serviceId      the service id
   * @param deploymentType the deployment type
   * @return the container task by deployment type
   */
  ContainerTask getContainerTaskByDeploymentType(String appId, String serviceId, String deploymentType);
}
