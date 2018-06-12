package software.wings.resources;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static software.wings.beans.SearchFilter.Operator.EQ;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiParam;
import software.wings.beans.CommandCategory;
import software.wings.beans.LambdaSpecification;
import software.wings.beans.RestResponse;
import software.wings.beans.Service;
import software.wings.beans.Setup.SetupStatus;
import software.wings.beans.command.ServiceCommand;
import software.wings.beans.container.ContainerTask;
import software.wings.beans.container.HelmChartSpecification;
import software.wings.beans.container.KubernetesPayload;
import software.wings.beans.container.PcfServiceSpecification;
import software.wings.beans.container.UserDataSpecification;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.security.PermissionAttribute.Action;
import software.wings.security.PermissionAttribute.PermissionType;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.AuthRule;
import software.wings.security.annotations.ListAPI;
import software.wings.security.annotations.Scope;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.stencils.Stencil;

import java.util.List;
import javax.validation.constraints.NotNull;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

/**
 * Created by anubhaw on 3/25/16.
 */
@Api("services")
@Path("services")
@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Scope(ResourceType.APPLICATION)
@AuthRule(permissionType = PermissionType.SERVICE)
public class ServiceResource {
  private ServiceResourceService serviceResourceService;

  /**
   * Instantiates a new service resource.
   *
   * @param serviceResourceService the service resource service
   */
  @Inject
  public ServiceResource(ServiceResourceService serviceResourceService) {
    this.serviceResourceService = serviceResourceService;
  }

  /**
   * List.
   *
   * @param appId       the app id
   * @param pageRequest the page request
   * @return the rest response
   */
  @GET
  @Timed
  @ExceptionMetered
  @ListAPI(ResourceType.SERVICE)
  public RestResponse<PageResponse<Service>> list(@QueryParam("appId") String appId,
      @BeanParam PageRequest<Service> pageRequest, @QueryParam("details") @DefaultValue("true") boolean details) {
    if (appId != null) {
      pageRequest.addFilter("appId", EQ, appId);
    }
    if (!details) {
      new RestResponse<>(serviceResourceService.list(pageRequest, false, false));
    }
    return new RestResponse<>(serviceResourceService.list(pageRequest, true, true));
  }

  /**
   * Gets the.
   *
   * @param appId     the app id
   * @param serviceId the service id
   * @param status    the status
   * @return the rest response
   */
  @GET
  @Path("{serviceId}")
  @Timed
  @ExceptionMetered
  public RestResponse<Service> get(@QueryParam("appId") String appId, @PathParam("serviceId") String serviceId,
      @QueryParam("status") SetupStatus status) {
    if (status == null) {
      status = SetupStatus.COMPLETE;
    }
    return new RestResponse<>(serviceResourceService.get(appId, serviceId, status));
  }

  /**
   * Save.
   *
   * @param appId   the app id
   * @param service the service
   * @return the rest response
   */
  @POST
  @Timed
  @ExceptionMetered
  public RestResponse<Service> save(@QueryParam("appId") String appId, Service service) {
    service.setAppId(appId);
    return new RestResponse<>(serviceResourceService.save(service));
  }

  /**
   * Update.
   *
   * @param appId     the app id
   * @param serviceId the service id
   * @param service   the service
   * @return the rest response
   */
  @PUT
  @Path("{serviceId}")
  @Timed
  @ExceptionMetered
  public RestResponse<Service> update(
      @QueryParam("appId") String appId, @PathParam("serviceId") String serviceId, Service service) {
    service.setUuid(serviceId);
    service.setAppId(appId);
    return new RestResponse<>(serviceResourceService.update(service));
  }

  @POST
  @Path("{serviceId}/clone")
  @Timed
  @ExceptionMetered
  public RestResponse<Service> clone(
      @QueryParam("appId") String appId, @PathParam("serviceId") String serviceId, Service service) {
    service.setAppId(appId);
    return new RestResponse<>(serviceResourceService.clone(appId, serviceId, service));
  }

  /**
   * Delete.
   *
   * @param appId     the app id
   * @param serviceId the service id
   * @return the rest response
   */
  @DELETE
  @Path("{serviceId}")
  @Timed
  @ExceptionMetered
  public RestResponse delete(@QueryParam("appId") String appId, @PathParam("serviceId") String serviceId) {
    serviceResourceService.delete(appId, serviceId);
    return new RestResponse();
  }

  /**
   * Save command.
   *
   * @param appId     the app id
   * @param serviceId the service id
   * @param command   the command
   * @return the rest response
   */
  @POST
  @Path("{serviceId}/commands")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = PermissionType.SERVICE, action = Action.UPDATE)
  public RestResponse<Service> saveCommand(@ApiParam(name = "appId", required = true) @QueryParam("appId") String appId,
      @ApiParam(name = "serviceId", required = true) @PathParam("serviceId") String serviceId,
      @ApiParam(name = "command", required = true) ServiceCommand command) {
    return new RestResponse<>(serviceResourceService.addCommand(appId, serviceId, command, true));
  }

  /**
   * Save command.
   *
   * @param appId     the app id
   * @param serviceId the service id
   * @return the rest response
   */
  @PUT
  @Path("{serviceId}/commands")
  @Timed
  @ExceptionMetered
  public RestResponse<Service> updateCommands(
      @ApiParam(name = "appId", required = true) @QueryParam("appId") String appId,
      @ApiParam(name = "serviceId", required = true) @PathParam("serviceId") String serviceId,
      @ApiParam(name = "command", required = true) List<ServiceCommand> commands) {
    return new RestResponse<>(serviceResourceService.updateCommandsOrder(appId, serviceId, commands));
  }

  @GET
  @Path("{serviceId}/commands/{commandName}")
  @Timed
  @ExceptionMetered
  public RestResponse<ServiceCommand> getCommand(@QueryParam("appId") String appId,
      @PathParam("serviceId") String serviceId, @PathParam("commandName") String commandName,
      @QueryParam("version") int version) {
    return new RestResponse<>(
        serviceResourceService.getCommandByNameAndVersion(appId, serviceId, commandName, version));
  }

  /**
   * Update command.
   *
   * @param appId     the app id
   * @param serviceId the service id
   * @param command   serviceCommand
   * @return the rest response
   */
  @PUT
  @Path("{serviceId}/commands/{commandName}")
  @Timed
  @ExceptionMetered
  public RestResponse<Service> updateCommand(
      @QueryParam("appId") String appId, @PathParam("serviceId") String serviceId, ServiceCommand command) {
    return new RestResponse<>(serviceResourceService.updateCommand(appId, serviceId, command));
  }

  @POST
  @Path("{serviceId}/commands/{commandName}/clone")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = PermissionType.SERVICE, action = Action.UPDATE)
  public RestResponse<Service> cloneCommand(@QueryParam("appId") String appId, @PathParam("serviceId") String serviceId,
      @PathParam("commandName") String commandName, ServiceCommand command) {
    return new RestResponse<>(serviceResourceService.cloneCommand(appId, serviceId, commandName, command));
  }

  /**
   * Delete command.
   *
   * @param appId            the app id
   * @param serviceId        the service id
   * @param serviceCommandId the command name
   * @return the rest response
   */
  @DELETE
  @Path("{serviceId}/commands/{serviceCommandId}")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = PermissionType.SERVICE, action = Action.UPDATE)
  public RestResponse<Service> deleteCommand(@QueryParam("appId") String appId,
      @PathParam("serviceId") String serviceId, @PathParam("serviceCommandId") String serviceCommandId) {
    return new RestResponse<>(serviceResourceService.deleteCommand(appId, serviceId, serviceCommandId));
  }

  /**
   * Stencils rest response.
   *
   * @param appId       the app id
   * @param serviceId   the service id
   * @param commandName the command name
   * @return the rest response
   */
  @GET
  @Path("{serviceId}/commands/stencils")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = PermissionType.LOGGED_IN)
  public RestResponse<List<Stencil>> stencils(@QueryParam("appId") String appId,
      @PathParam("serviceId") String serviceId, @QueryParam("filterCommand") String commandName) {
    return new RestResponse<>(serviceResourceService.getCommandStencils(appId, serviceId, commandName, true));
  }

  @POST
  @Path("{serviceId}/containers/tasks")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = PermissionType.SERVICE, action = Action.UPDATE)
  public RestResponse<ContainerTask> createContainerTask(@QueryParam("appId") String appId,
      @QueryParam("advanced") boolean advanced, @PathParam("serviceId") String serviceId, ContainerTask containerTask) {
    containerTask.setAppId(appId);
    containerTask.setServiceId(serviceId);
    return new RestResponse<>(serviceResourceService.createContainerTask(containerTask, advanced));
  }

  @GET
  @Path("{serviceId}/containers/tasks")
  @Timed
  @ExceptionMetered
  public RestResponse<PageResponse<ContainerTask>> listContainerTask(@QueryParam("appId") String appId,
      @PathParam("serviceId") String serviceId, @BeanParam PageRequest<ContainerTask> pageRequest) {
    pageRequest.addFilter("appId", EQ, appId);
    pageRequest.addFilter("serviceId", EQ, serviceId);
    return new RestResponse<>(serviceResourceService.listContainerTasks(pageRequest));
  }

  @PUT
  @Path("{serviceId}/containers/tasks/{taskId}")
  @Timed
  @ExceptionMetered
  public RestResponse<ContainerTask> updateContainerTask(@QueryParam("appId") String appId,
      @QueryParam("advanced") boolean advanced, @PathParam("serviceId") String serviceId,
      @PathParam("taskId") String taskId, ContainerTask containerTask) {
    containerTask.setAppId(appId);
    containerTask.setServiceId(serviceId);
    containerTask.setUuid(taskId);
    return new RestResponse<>(serviceResourceService.updateContainerTask(containerTask, advanced));
  }

  @PUT
  @Path("{serviceId}/containers/tasks/{taskId}/advanced")
  @Timed
  @ExceptionMetered
  public RestResponse<ContainerTask> updateContainerTaskAdvanced(@QueryParam("appId") String appId,
      @QueryParam("reset") boolean reset, @PathParam("serviceId") String serviceId, @PathParam("taskId") String taskId,
      KubernetesPayload kubernetesPayload) {
    return new RestResponse<>(
        serviceResourceService.updateContainerTaskAdvanced(appId, serviceId, taskId, kubernetesPayload, reset));
  }

  @GET
  @Path("{serviceId}/containers/tasks/stencils")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = PermissionType.LOGGED_IN)
  public RestResponse<List<Stencil>> listTaskStencils(
      @QueryParam("appId") String appId, @PathParam("serviceId") String serviceId) {
    return new RestResponse<>(serviceResourceService.getContainerTaskStencils(appId, serviceId));
  }

  @POST
  @Path("{serviceId}/containers/charts")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = PermissionType.SERVICE, action = Action.UPDATE)
  public RestResponse<HelmChartSpecification> createHelmChartSpecification(@QueryParam("appId") String appId,
      @PathParam("serviceId") String serviceId, HelmChartSpecification helmChartSpecification) {
    helmChartSpecification.setAppId(appId);
    helmChartSpecification.setServiceId(serviceId);
    return new RestResponse<>(serviceResourceService.createHelmChartSpecification(helmChartSpecification));
  }

  @GET
  @Path("{serviceId}/containers/charts")
  @Timed
  @ExceptionMetered
  public RestResponse<PageResponse<HelmChartSpecification>> listHelmChartSpecification(
      @QueryParam("appId") String appId, @PathParam("serviceId") String serviceId,
      @BeanParam PageRequest<HelmChartSpecification> pageRequest) {
    pageRequest.addFilter("appId", EQ, appId);
    pageRequest.addFilter("serviceId", EQ, serviceId);
    return new RestResponse<>(serviceResourceService.listHelmChartSpecifications(pageRequest));
  }

  @PUT
  @Path("{serviceId}/containers/charts/{taskId}")
  @Timed
  @ExceptionMetered
  public RestResponse<HelmChartSpecification> updateHelmChartSpecification(@QueryParam("appId") String appId,
      @PathParam("serviceId") String serviceId, @PathParam("taskId") String taskId,
      HelmChartSpecification helmChartSpecification) {
    helmChartSpecification.setAppId(appId);
    helmChartSpecification.setServiceId(serviceId);
    helmChartSpecification.setUuid(taskId);
    return new RestResponse<>(serviceResourceService.updateHelmChartSpecification(helmChartSpecification));
  }

  @POST
  @Path("{serviceId}/pcfspecification")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = PermissionType.SERVICE, action = Action.CREATE)
  public RestResponse<PcfServiceSpecification> createPcfServiceSpecification(@QueryParam("appId") String appId,
      @PathParam("serviceId") String serviceId, PcfServiceSpecification pcfServiceSpecification) {
    pcfServiceSpecification.setAppId(appId);
    pcfServiceSpecification.setServiceId(serviceId);
    return new RestResponse<>(serviceResourceService.createPcfServiceSpecification(pcfServiceSpecification));
  }

  @GET
  @Path("{serviceId}/pcfspecification")
  @Timed
  @ExceptionMetered
  public RestResponse<PcfServiceSpecification> getPcfServiceSpecification(@QueryParam("appId") String appId,
      @PathParam("serviceId") String serviceId, @BeanParam PageRequest<PcfServiceSpecification> pageRequest) {
    return new RestResponse<>(serviceResourceService.getExistingOrDefaultPcfServiceSpecification(appId, serviceId));
  }

  @PUT
  @Path("{serviceId}/pcfspecification/{pcfSpecificationId}")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = PermissionType.SERVICE, action = Action.UPDATE)
  public RestResponse<PcfServiceSpecification> updatePcfServiceSpecification(@QueryParam("appId") String appId,
      @PathParam("serviceId") String serviceId, @PathParam("pcfSpecificationId") String pcfSpecificationId,
      PcfServiceSpecification pcfServiceSpecification) {
    pcfServiceSpecification.setAppId(appId);
    pcfServiceSpecification.setServiceId(serviceId);
    pcfServiceSpecification.setUuid(pcfSpecificationId);
    return new RestResponse<>(serviceResourceService.updatePcfServiceSpecification(pcfServiceSpecification));
  }

  @PUT
  @Path("{serviceId}/pcfspecification/reset")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = PermissionType.SERVICE, action = Action.UPDATE)
  public RestResponse<PcfServiceSpecification> resetToDefaultPcfServiceSpecification(@QueryParam("appId") String appId,
      @PathParam("serviceId") String serviceId, PcfServiceSpecification pcfServiceSpecification) {
    pcfServiceSpecification.setAppId(appId);
    pcfServiceSpecification.setServiceId(serviceId);
    return new RestResponse<>(serviceResourceService.resetToDefaultPcfServiceSpecification(pcfServiceSpecification));
  }

  @POST
  @Path("{serviceId}/lambda-specifications")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = PermissionType.SERVICE, action = Action.UPDATE)
  public RestResponse<LambdaSpecification> createLambdaSpecification(@QueryParam("appId") String appId,
      @PathParam("serviceId") String serviceId, LambdaSpecification lambdaSpecification) {
    lambdaSpecification.setAppId(appId);
    lambdaSpecification.setServiceId(serviceId);
    return new RestResponse<>(serviceResourceService.createLambdaSpecification(lambdaSpecification));
  }

  @PUT
  @Path("{serviceId}/lambda-specifications/{lambdaSpecificationId}")
  @Timed
  @ExceptionMetered
  public RestResponse<LambdaSpecification> updateLambdaSpecification(@QueryParam("appId") String appId,
      @PathParam("serviceId") String serviceId, @PathParam("lambdaSpecificationId") String lambdaSpecificationId,
      LambdaSpecification lambdaSpecification) {
    lambdaSpecification.setAppId(appId);
    lambdaSpecification.setServiceId(serviceId);
    lambdaSpecification.setUuid(lambdaSpecificationId);
    return new RestResponse<>(serviceResourceService.updateLambdaSpecification(lambdaSpecification));
  }

  @GET
  @Path("{serviceId}/lambda-specifications")
  @Timed
  @ExceptionMetered
  public RestResponse<PageResponse<LambdaSpecification>> listLambdaSpecification(@QueryParam("appId") String appId,
      @PathParam("serviceId") String serviceId, @BeanParam PageRequest<LambdaSpecification> pageRequest) {
    pageRequest.addFilter("appId", EQ, appId);
    pageRequest.addFilter("serviceId", EQ, serviceId);
    return new RestResponse<>(serviceResourceService.listLambdaSpecification(pageRequest));
  }

  @POST
  @Path("{serviceId}/user-data-specifications")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = PermissionType.SERVICE, action = Action.UPDATE)
  public RestResponse<UserDataSpecification> createUserDataSpecification(@QueryParam("appId") String appId,
      @PathParam("serviceId") String serviceId, @NotNull UserDataSpecification userDataSpecification) {
    userDataSpecification.setAppId(appId);
    userDataSpecification.setServiceId(serviceId);
    return new RestResponse<>(serviceResourceService.createUserDataSpecification(userDataSpecification));
  }

  @PUT
  @Path("{serviceId}/user-data-specifications/{userDataSpecificationId}")
  @Timed
  @ExceptionMetered
  public RestResponse<UserDataSpecification> updateUserDataSpecification(@QueryParam("appId") String appId,
      @PathParam("serviceId") String serviceId, @PathParam("userDataSpecificationId") String userDataSpecificationId,
      @NotNull UserDataSpecification userDataSpecification) {
    userDataSpecification.setAppId(appId);
    userDataSpecification.setServiceId(serviceId);
    userDataSpecification.setUuid(userDataSpecificationId);
    return new RestResponse<>(serviceResourceService.updateUserDataSpecification(userDataSpecification));
  }

  @GET
  @Path("{serviceId}/user-data-specifications")
  @Timed
  @ExceptionMetered
  public RestResponse<PageResponse<UserDataSpecification>> listUserDataSpecification(@QueryParam("appId") String appId,
      @PathParam("serviceId") String serviceId, @BeanParam PageRequest<UserDataSpecification> pageRequest) {
    pageRequest.addFilter("appId", EQ, appId);
    pageRequest.addFilter("serviceId", EQ, serviceId);
    return new RestResponse<>(serviceResourceService.listUserDataSpecification(pageRequest));
  }

  @POST
  @Path("{serviceId}/config-map-yaml")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = PermissionType.SERVICE, action = Action.UPDATE)
  public RestResponse<Service> setConfigMapYaml(
      @ApiParam(name = "appId", required = true) @QueryParam("appId") String appId,
      @ApiParam(name = "serviceId", required = true) @PathParam("serviceId") String serviceId,
      KubernetesPayload kubernetesPayload) {
    return new RestResponse<>(serviceResourceService.setConfigMapYaml(appId, serviceId, kubernetesPayload));
  }

  @PUT
  @Path("{serviceId}/config-map-yaml")
  @Timed
  @ExceptionMetered
  public RestResponse<Service> updateConfigMapYaml(
      @ApiParam(name = "appId", required = true) @QueryParam("appId") String appId,
      @ApiParam(name = "serviceId", required = true) @PathParam("serviceId") String serviceId,
      KubernetesPayload kubernetesPayload) {
    return new RestResponse<>(serviceResourceService.setConfigMapYaml(appId, serviceId, kubernetesPayload));
  }

  @DELETE
  @Path("{serviceId}/config-map-yaml")
  @Timed
  @ExceptionMetered
  public RestResponse<Service> deleteConfigMapYaml(
      @ApiParam(name = "appId", required = true) @QueryParam("appId") String appId,
      @ApiParam(name = "serviceId", required = true) @PathParam("serviceId") String serviceId) {
    return new RestResponse<>(serviceResourceService.setConfigMapYaml(appId, serviceId, new KubernetesPayload()));
  }

  @POST
  @Path("{serviceId}/helm-value-yaml")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = PermissionType.SERVICE, action = Action.UPDATE)
  public RestResponse<Service> setHelmValueYaml(
      @ApiParam(name = "appId", required = true) @QueryParam("appId") String appId,
      @ApiParam(name = "serviceId", required = true) @PathParam("serviceId") String serviceId,
      KubernetesPayload kubernetesPayload) {
    return new RestResponse<>(serviceResourceService.setHelmValueYaml(appId, serviceId, kubernetesPayload));
  }

  @PUT
  @Path("{serviceId}/helm-value-yaml")
  @Timed
  @ExceptionMetered
  public RestResponse<Service> updateHelmValueYaml(
      @ApiParam(name = "appId", required = true) @QueryParam("appId") String appId,
      @ApiParam(name = "serviceId", required = true) @PathParam("serviceId") String serviceId,
      KubernetesPayload kubernetesPayload) {
    return new RestResponse<>(serviceResourceService.setHelmValueYaml(appId, serviceId, kubernetesPayload));
  }

  @DELETE
  @Path("{serviceId}/helm-value-yaml")
  @Timed
  @ExceptionMetered
  public RestResponse<Service> deleteHelmValueYaml(
      @ApiParam(name = "appId", required = true) @QueryParam("appId") String appId,
      @ApiParam(name = "serviceId", required = true) @PathParam("serviceId") String serviceId) {
    return new RestResponse<>(serviceResourceService.setHelmValueYaml(appId, serviceId, new KubernetesPayload()));
  }

  /**
   * Stencils rest response.
   *
   * @param appId       the app id
   * @param serviceId   the service id
   * @param commandName the command name
   * @return the rest response
   */
  @GET
  @Path("{serviceId}/commands/categories")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = PermissionType.LOGGED_IN)
  public RestResponse<List<CommandCategory>> getCommandUnitItems(@QueryParam("appId") String appId,
      @PathParam("serviceId") String serviceId, @QueryParam("filterCommand") String commandName) {
    return new RestResponse<>(serviceResourceService.getCommandCategories(appId, serviceId, commandName));
  }
}
