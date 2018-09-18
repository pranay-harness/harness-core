package software.wings.resources.yaml;

import static io.harness.beans.PageResponse.PageResponseBuilder.aPageResponse;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.MULTIPART_FORM_DATA;
import static software.wings.security.PermissionAttribute.ResourceType.SETTING;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.harness.beans.PageResponse;
import io.harness.exception.InvalidRequestException;
import io.swagger.annotations.Api;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.hibernate.validator.constraints.NotEmpty;
import org.jvnet.hk2.annotations.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.app.MainConfiguration;
import software.wings.beans.Application;
import software.wings.beans.Base;
import software.wings.beans.ConfigFile;
import software.wings.beans.Environment;
import software.wings.beans.InfrastructureProvisioner;
import software.wings.beans.Pipeline;
import software.wings.beans.RestResponse;
import software.wings.beans.Service;
import software.wings.beans.SettingAttribute;
import software.wings.beans.User;
import software.wings.beans.Workflow;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.command.ServiceCommand;
import software.wings.exception.YamlProcessingException;
import software.wings.security.PermissionAttribute.Action;
import software.wings.security.PermissionAttribute.PermissionType;
import software.wings.security.UserThreadLocal;
import software.wings.security.annotations.AuthRule;
import software.wings.security.annotations.PublicApi;
import software.wings.security.annotations.Scope;
import software.wings.service.impl.yaml.YamlWebHookPayload;
import software.wings.service.intfc.AuthService;
import software.wings.service.intfc.HarnessUserGroupService;
import software.wings.service.intfc.yaml.AppYamlResourceService;
import software.wings.service.intfc.yaml.YamlArtifactStreamService;
import software.wings.service.intfc.yaml.YamlDirectoryService;
import software.wings.service.intfc.yaml.YamlGitService;
import software.wings.service.intfc.yaml.YamlResourceService;
import software.wings.service.intfc.yaml.clone.YamlCloneService;
import software.wings.service.intfc.yaml.sync.YamlService;
import software.wings.utils.BoundedInputStream;
import software.wings.yaml.YamlPayload;
import software.wings.yaml.directory.DirectoryNode;
import software.wings.yaml.errorhandling.GitSyncError;
import software.wings.yaml.gitSync.GitSyncWebhook;
import software.wings.yaml.gitSync.YamlGitConfig;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.List;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

/**
 * Created by bsollish
 */
@Api("setup-as-code/yaml")
@Path("setup-as-code/yaml")
@Produces(APPLICATION_JSON)
@Scope(SETTING)
public class YamlResource {
  private static final Logger logger = LoggerFactory.getLogger(YamlResource.class);

  private YamlResourceService yamlResourceService;
  private AppYamlResourceService appYamlResourceService;
  private YamlArtifactStreamService yamlArtifactStreamService;
  private YamlService yamlService;
  private YamlDirectoryService yamlDirectoryService;
  private YamlGitService yamlGitService;
  private AuthService authService;
  private HarnessUserGroupService harnessUserGroupService;
  @Inject private MainConfiguration configuration;
  @Inject private YamlCloneService yamlCloneService;

  /**
   * Instantiates a new service resource.
   *
   * @param yamlResourceService        the yaml resource servicewe
   * @param appYamlResourceService     the app yaml resource service
   * @param yamlDirectoryService       the yaml directory service
   * @param yamlArtifactStreamService  the yaml artifact stream service
   * @param yamlService            the yaml service
   * @param yamlGitSyncService
   */
  @Inject
  public YamlResource(YamlResourceService yamlResourceService, AppYamlResourceService appYamlResourceService,
      YamlDirectoryService yamlDirectoryService, YamlArtifactStreamService yamlArtifactStreamService,
      YamlService yamlService, YamlGitService yamlGitSyncService, AuthService authService,
      HarnessUserGroupService harnessUserGroupService) {
    this.yamlResourceService = yamlResourceService;
    this.appYamlResourceService = appYamlResourceService;
    this.yamlDirectoryService = yamlDirectoryService;
    this.yamlArtifactStreamService = yamlArtifactStreamService;
    this.yamlService = yamlService;
    this.yamlGitService = yamlGitSyncService;
    this.authService = authService;
    this.harnessUserGroupService = harnessUserGroupService;
  }

  /**
   * Gets the yaml for a workflow
   *
   * @param appId      the app id
   * @param workflowId the workflow id
   * @return the rest response
   */
  @GET
  @Path("/workflows/{workflowId}")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = PermissionType.WORKFLOW, action = Action.READ)
  public RestResponse<YamlPayload> getWorkflow(
      @QueryParam("appId") String appId, @PathParam("workflowId") String workflowId) {
    return yamlResourceService.getWorkflow(appId, workflowId);
  }

  /**
   * Gets the yaml for a workflow
   *
   * @param appId      the app id
   * @param provisionerId the provisioner id
   * @return the rest response
   */
  @GET
  @Path("/infrastructureprovisioners/{infraProvisionerId}")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = PermissionType.PROVISIONER, action = Action.READ)
  public RestResponse<YamlPayload> getProvisioner(
      @QueryParam("appId") String appId, @PathParam("infraProvisionerId") String provisionerId) {
    return yamlResourceService.getProvisioner(appId, provisionerId);
  }

  @PUT
  @Path("/infrastructureprovisioners/{infraProvisionerId}")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = PermissionType.PROVISIONER, action = Action.UPDATE)
  public RestResponse<InfrastructureProvisioner> updateProvisioner(
      @QueryParam("accountId") String accountId, @QueryParam("appId") String appId, YamlPayload yamlPayload) {
    return yamlService.update(yamlPayload, accountId);
  }

  // TODO - need to add a PUT for updateWorkflow HERE

  /**
   * Gets the yaml version of a trigger by artifactStreamId
   *
   * @param appId            the app id
   * @param artifactStreamId the artifact stream id
   * @return the rest response
   */
  @GET
  @Path("/triggers/{artifactStreamId}")
  @Timed
  @ExceptionMetered
  public RestResponse<YamlPayload> getTrigger(
      @QueryParam("appId") String appId, @PathParam("artifactStreamId") String artifactStreamId) {
    return yamlResourceService.getTrigger(appId, artifactStreamId);
  }

  /**
   * Update a trigger that is sent as Yaml (in a JSON "wrapper")
   *
   * @param yamlPayload the yaml version of the service command
   * @param accountId   the account id
   * @return the rest response
   */
  @PUT
  @Path("/triggers/{artifactStreamId}")
  @Timed
  @ExceptionMetered
  public RestResponse<ArtifactStream> updateTrigger(
      @QueryParam("accountId") String accountId, @QueryParam("appId") String appId, YamlPayload yamlPayload) {
    return yamlService.update(yamlPayload, accountId);
  }

  /**
   * Gets the yaml version of a pipeline by pipelineId
   *
   * @param appId      the app id
   * @param pipelineId the pipeline id
   * @return the rest response
   */
  @GET
  @Path("/pipelines/{pipelineId}")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = PermissionType.PIPELINE, action = Action.READ)
  public RestResponse<YamlPayload> getPipeline(
      @QueryParam("appId") String appId, @PathParam("pipelineId") String pipelineId) {
    return yamlResourceService.getPipeline(appId, pipelineId);
  }

  /**
   * Update a pipeline that is sent as Yaml (in a JSON "wrapper")
   *
   * @param appId         the app id
   * @param yamlPayload   the yaml version of the service command
   * @return the rest response
   */
  @PUT
  @Path("/pipelines/{pipelineId}")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = PermissionType.PIPELINE, action = Action.UPDATE)
  public RestResponse<Pipeline> updatePipeline(
      @QueryParam("accountId") String accountId, @QueryParam("appId") String appId, YamlPayload yamlPayload) {
    return yamlService.update(yamlPayload, accountId);
  }

  @PUT
  @Path("/workflows/{workflowId}")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = PermissionType.WORKFLOW, action = Action.UPDATE)
  public RestResponse<Workflow> updateWorkflow(
      @QueryParam("accountId") String accountId, @QueryParam("appId") String appId, YamlPayload yamlPayload) {
    return yamlService.update(yamlPayload, accountId);
  }

  /**
   * Gets the yaml version of a notification group by id
   *
   * @param accountId            the account id
   * @param notificationGroupId  the notification group id
   * @return the rest response
   */
  @GET
  @Path("/notification-groups/{notificationGroupId}")
  @Timed
  @ExceptionMetered
  public RestResponse<YamlPayload> getNotificationGroup(
      @QueryParam("accountId") String accountId, @PathParam("notificationGroupId") String notificationGroupId) {
    return yamlResourceService.getNotificationGroup(accountId, notificationGroupId);
  }

  /**
   * Gets the yaml version of a service command by serviceCommandId
   *
   * @param appId            the app id
   * @param serviceCommandId the service command id
   * @return the rest response
   */
  @GET
  @Path("/service-commands/{serviceCommandId}")
  @Timed
  @ExceptionMetered
  public RestResponse<YamlPayload> getServiceCommand(
      @QueryParam("appId") String appId, @PathParam("serviceCommandId") String serviceCommandId) {
    return yamlResourceService.getServiceCommand(appId, serviceCommandId);
  }

  /**
   * Update a service command that is sent as Yaml (in a JSON "wrapper")
   *
   * @param accountId            the account id
   * @param yamlPayload      the yaml version of the service command
   * @return the rest response
   */
  @PUT
  @Path("/service-commands/{serviceCommandId}")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = PermissionType.SERVICE, action = Action.UPDATE, skipAuth = true)
  public RestResponse<ServiceCommand> updateServiceCommand(
      @QueryParam("accountId") String accountId, YamlPayload yamlPayload) {
    return yamlService.update(yamlPayload, accountId);
  }

  /**
   * Gets all the setting attributes of a given type by accountId
   *
   * @param accountId the account id
   * @param type      the SettingVariableTypes
   * @return the rest response
   */
  @GET
  @Path("/settings")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = PermissionType.ACCOUNT_MANAGEMENT)
  public RestResponse<YamlPayload> getSettingAttributesList(
      @QueryParam("accountId") String accountId, @QueryParam("type") String type) {
    return yamlResourceService.getGlobalSettingAttributesList(accountId, type);
  }

  /**
   * Gets the yaml for a setting attribute by accountId and uuid
   *
   * @param accountId the account id
   * @param uuid      the uid of the setting attribute
   * @return the rest response
   */
  @GET
  @Path("/settings/{uuid}")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = PermissionType.ACCOUNT_MANAGEMENT)
  public RestResponse<YamlPayload> getSettingAttribute(
      @QueryParam("accountId") String accountId, @PathParam("uuid") String uuid) {
    return yamlResourceService.getSettingAttribute(accountId, uuid);
  }

  /**
   * Gets the yaml for a setting attribute by accountId and uuid
   *
   * @param accountId the account id
   * @param uuid      the uid of the setting attribute
   * @return the rest response
   */
  @GET
  @Path("/defaults/{uuid}")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = PermissionType.ACCOUNT_MANAGEMENT)
  public RestResponse<YamlPayload> getDefaults(
      @QueryParam("accountId") String accountId, @PathParam("uuid") String uuid) {
    return yamlResourceService.getDefaultVariables(accountId, uuid);
  }

  /**
   * Update defaults that is sent as Yaml
   *
   * @param accountId            the account id
   * @param yamlPayload      the yaml version of the defaults
   * @return the rest response
   */
  @PUT
  @Path("/defaults/{uuid}")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = PermissionType.ACCOUNT_MANAGEMENT)
  public RestResponse<ServiceCommand> updateDefaults(
      @QueryParam("accountId") String accountId, YamlPayload yamlPayload) {
    return yamlService.update(yamlPayload, accountId);
  }

  /**
   * Update setting attribute sent as Yaml (in a JSON "wrapper")
   *
   * @param accountId     the account id
   * @param uuid          the uid of the setting attribute
   * @param type          the SettingVariableTypes
   * @param yamlPayload   the yaml version of setup
   * @param deleteEnabled the delete enabled
   * @return the rest response
   */
  @PUT
  @Path("/settings/{uuid}")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = PermissionType.ACCOUNT_MANAGEMENT)
  public RestResponse<SettingAttribute> updateSettingAttribute(@QueryParam("accountId") String accountId,
      @PathParam("uuid") String uuid, @QueryParam("type") String type, YamlPayload yamlPayload,
      @QueryParam("deleteEnabled") @DefaultValue("false") boolean deleteEnabled) {
    return yamlService.update(yamlPayload, accountId);
  }

  /**
   * Gets the yaml version of an environment by envId
   *
   * @param appId the app id
   * @param envId the environment id
   * @return the rest response
   */
  @GET
  @Path("/environments/{envId}")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = PermissionType.ENV, action = Action.READ)
  public RestResponse<YamlPayload> getEnvironment(@QueryParam("appId") String appId, @PathParam("envId") String envId) {
    return yamlResourceService.getEnvironment(appId, envId);
  }

  @GET
  @Path("/configs/{configId}")
  @Timed
  @ExceptionMetered
  public RestResponse<YamlPayload> getConfigFile(@QueryParam("accountId") String accountId,
      @QueryParam("appId") String appId, @PathParam("configId") String configId) {
    return yamlResourceService.getConfigFileYaml(accountId, appId, configId);
  }

  /**
   * Update a environment that is sent as Yaml (in a JSON "wrapper")
   *
   * @param accountId         the account id
   * @param appId         the app id
   * @param yamlPayload   the yaml version of environment
   * @return the rest response
   */
  @PUT
  @Path("/environments/{envId}")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = PermissionType.ENV, action = Action.UPDATE, skipAuth = true)
  public RestResponse<Environment> updateEnvironment(
      @QueryParam("accountId") String accountId, @QueryParam("appId") String appId, YamlPayload yamlPayload) {
    return yamlService.update(yamlPayload, accountId);
  }

  /**
   * Gets the yaml version of a service by serviceId
   *
   * @param appId     the app id
   * @param serviceId the service id
   * @return the rest response
   */
  @GET
  @Path("/services/{serviceId}")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = PermissionType.SERVICE, action = Action.READ)
  public RestResponse<YamlPayload> getService(
      @QueryParam("appId") String appId, @PathParam("serviceId") String serviceId) {
    return yamlResourceService.getService(appId, serviceId);
  }

  /**
   * Update a service that is sent as Yaml (in a JSON "wrapper")
   *
   * @param appId         the app id
   * @param accountId         the account id
   * @param yamlPayload   the yaml version of service
   * @return the rest response
   */
  @PUT
  @Path("/services/{serviceId}")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = PermissionType.SERVICE, action = Action.UPDATE)
  public RestResponse<Service> updateService(
      @QueryParam("accountId") String accountId, @QueryParam("appId") String appId, YamlPayload yamlPayload) {
    return yamlService.update(yamlPayload, accountId);
  }

  /**
   * Update a config file that is sent as Yaml (in a JSON "wrapper")
   * @param appId app id
   * @param configId the config id
   * @param yamlPayload the yaml version of configFile
   * @param deleteEnabled
   * @return
   */
  @PUT
  @Path("/configs/{configId}")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = PermissionType.SERVICE, action = Action.UPDATE, skipAuth = true)
  public RestResponse<ConfigFile> updateConfigFile(@QueryParam("accountId") String accountId,
      @QueryParam("appId") String appId, @PathParam("configId") String configId, YamlPayload yamlPayload,
      @QueryParam("deleteEnabled") @DefaultValue("false") boolean deleteEnabled) {
    return yamlService.update(yamlPayload, accountId);
  }

  /**
   * Gets the yaml version of an app by appId
   *
   * @param appId the app id
   * @return the rest response
   */
  @GET
  @Path("/applications/{appId}")
  @Timed
  @ExceptionMetered
  public RestResponse<YamlPayload> getApp(@PathParam("appId") String appId) {
    return appYamlResourceService.getApp(appId);
  }

  /**
   * Update an app that is sent as Yaml (in a JSON "wrapper")
   *
   * @param appId         the app id
   * @param yamlPayload   the yaml version of app
   * @param deleteEnabled the delete enabled
   * @return the rest response
   */
  @PUT
  @Path("/applications/{appId}")
  @Timed
  @ExceptionMetered
  public RestResponse<Application> updateApp(@PathParam("appId") String appId, YamlPayload yamlPayload,
      @QueryParam("deleteEnabled") @DefaultValue("false") boolean deleteEnabled) {
    return appYamlResourceService.updateApp(appId, yamlPayload, deleteEnabled);
  }

  /**
   * Gets the config as code directory by accountId
   *
   * @param accountId the account id
   * @return the rest response
   */
  @GET
  @Path("/directory")
  @Timed
  @ExceptionMetered
  public RestResponse<DirectoryNode> getDirectory(@QueryParam("accountId") String accountId) {
    return new RestResponse<>(yamlDirectoryService.getDirectory(accountId));
  }

  /**
   * Push directory rest response.
   *
   * @param accountId the account id
   * @return the rest response
   */
  @POST
  @Path("full-sync")
  @Timed
  @ExceptionMetered
  public RestResponse pushDirectory(@QueryParam("accountId") String accountId) {
    yamlGitService.fullSync(accountId, true);
    return new RestResponse<>();
  }

  /**
   * Gets artifact stream.
   *
   * @param appId            the app id
   * @param artifactStreamId the artifact stream id
   * @return the artifact stream
   */
  @GET
  @Path("/artifact-streams/{artifactStreamId}")
  @Timed
  @ExceptionMetered
  public RestResponse<YamlPayload> getArtifactStream(
      @QueryParam("appId") String appId, @PathParam("artifactStreamId") String artifactStreamId) {
    return yamlArtifactStreamService.getArtifactStreamYaml(appId, artifactStreamId);
  }

  /**
   * Update a config file that is sent as Yaml (in a JSON "wrapper")
   *
   * @param appId            app id
   * @param artifactStreamId the artifact stream id
   * @param yamlPayload      the yaml version of configFile
   * @param deleteEnabled    the delete enabled
   * @return rest response
   */
  @PUT
  @Path("/artifact-streams/{artifactStreamId}")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = PermissionType.SERVICE, action = Action.UPDATE, skipAuth = true)
  public RestResponse<Base> updateArtifactStream(@QueryParam("appId") String appId,
      @PathParam("artifactStreamId") String artifactStreamId, YamlPayload yamlPayload,
      @QueryParam("deleteEnabled") @DefaultValue("false") boolean deleteEnabled) {
    return yamlArtifactStreamService.updateArtifactStream(appId, artifactStreamId, yamlPayload, deleteEnabled);
  }

  @GET
  @Path("/infrastructuremappings/{infraMappingId}")
  @Timed
  @ExceptionMetered
  public RestResponse<YamlPayload> getInfraMapping(@QueryParam("appId") String appId,
      @QueryParam("accountId") String accountId, @PathParam("infraMappingId") String infraMappingId) {
    return yamlResourceService.getInfraMapping(accountId, appId, infraMappingId);
  }

  @PUT
  @Path("/infrastructuremappings/{infraMappingId}")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = PermissionType.ENV, action = Action.UPDATE, skipAuth = true)
  public RestResponse<Base> updateInfraMapping(@QueryParam("appId") String appId,
      @QueryParam("accountId") String accountId, YamlPayload yamlPayload,
      @QueryParam("deleteEnabled") @DefaultValue("false") boolean deleteEnabled) {
    return yamlService.update(yamlPayload, accountId);
  }

  @GET
  @Path("/container-tasks/{containerTaskId}")
  @Timed
  @ExceptionMetered
  public RestResponse<YamlPayload> getContainerTask(@QueryParam("appId") String appId,
      @QueryParam("accountId") String accountId, @PathParam("containerTaskId") String containerTaskId) {
    return yamlResourceService.getContainerTask(accountId, appId, containerTaskId);
  }

  @PUT
  @Path("/container-tasks/{containerTaskId}")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = PermissionType.SERVICE, action = Action.UPDATE, skipAuth = true)
  public RestResponse<Base> updateContainerTask(@QueryParam("appId") String appId,
      @QueryParam("accountId") String accountId, YamlPayload yamlPayload,
      @QueryParam("deleteEnabled") @DefaultValue("false") boolean deleteEnabled) {
    return yamlService.update(yamlPayload, accountId);
  }

  @GET
  @Path("/pcfservicespecifications/{pcfservicespecificationId}")
  @Timed
  @ExceptionMetered
  public RestResponse<YamlPayload> getPcfservicespecificationId(@QueryParam("appId") String appId,
      @QueryParam("accountId") String accountId,
      @PathParam("pcfservicespecificationId") String pcfservicespecificationId) {
    return yamlResourceService.getPcfServiceSpecification(accountId, appId, pcfservicespecificationId);
  }

  @PUT
  @Path("/pcfservicespecifications/{pcfservicespecificationId}")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = PermissionType.SERVICE, action = Action.UPDATE, skipAuth = true)
  public RestResponse<Base> updatePcfServiceSpecification(@QueryParam("appId") String appId,
      @QueryParam("accountId") String accountId, YamlPayload yamlPayload,
      @QueryParam("deleteEnabled") @DefaultValue("false") boolean deleteEnabled) {
    return yamlService.update(yamlPayload, accountId);
  }

  @GET
  @Path("/helm-charts/{helmChartSpecificationId}")
  @Timed
  @ExceptionMetered
  public RestResponse<YamlPayload> getHelmChartSpecification(@QueryParam("appId") String appId,
      @QueryParam("accountId") String accountId,
      @PathParam("helmChartSpecificationId") String helmChartSpecificationId) {
    return yamlResourceService.getHelmChartSpecification(accountId, appId, helmChartSpecificationId);
  }

  @PUT
  @Path("/helm-charts/{helmChartSpecificationId}")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = PermissionType.SERVICE, action = Action.UPDATE, skipAuth = true)
  public RestResponse<Base> updateHelmChartSpecification(@QueryParam("appId") String appId,
      @QueryParam("accountId") String accountId, YamlPayload yamlPayload,
      @QueryParam("deleteEnabled") @DefaultValue("false") boolean deleteEnabled) {
    return yamlService.update(yamlPayload, accountId);
  }

  @GET
  @Path("/lambda-specs/{lambdaSpecId}")
  @Timed
  @ExceptionMetered
  public RestResponse<YamlPayload> getLamdbaSpec(@QueryParam("appId") String appId,
      @QueryParam("accountId") String accountId, @PathParam("lambdaSpecId") String lambdaSpecId) {
    return yamlResourceService.getLambdaSpec(accountId, appId, lambdaSpecId);
  }

  @PUT
  @Path("/user-data-specs/{userDataSpecId}")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = PermissionType.SERVICE, action = Action.UPDATE)
  public RestResponse<Base> updateUserDataSpec(@QueryParam("appId") String appId,
      @QueryParam("accountId") String accountId, YamlPayload yamlPayload,
      @QueryParam("deleteEnabled") @DefaultValue("false") boolean deleteEnabled) {
    return yamlService.update(yamlPayload, accountId);
  }

  @GET
  @Path("/user-data-specs/{userDataSpecId}")
  @Timed
  @ExceptionMetered
  public RestResponse<YamlPayload> getUserDataSpec(@QueryParam("appId") String appId,
      @QueryParam("accountId") String accountId, @PathParam("userDataSpecId") String userDataSpecId) {
    return yamlResourceService.getUserDataSpec(accountId, appId, userDataSpecId);
  }

  @PUT
  @Path("/lambda-specs/{lambdaSpecId}")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = PermissionType.SERVICE, action = Action.UPDATE, skipAuth = true)
  public RestResponse<Base> updateLambdaSpec(@QueryParam("appId") String appId,
      @QueryParam("accountId") String accountId, YamlPayload yamlPayload,
      @QueryParam("deleteEnabled") @DefaultValue("false") boolean deleteEnabled) {
    return yamlService.update(yamlPayload, accountId);
  }

  /**
   * Update yaml file
   *
   * @param accountId   account id
   * @param yamlPayload yaml payload with payload as string
   * @return rest response
   */
  @PUT
  @Timed
  @ExceptionMetered
  public RestResponse<Base> updateYaml(@QueryParam("accountId") String accountId, YamlPayload yamlPayload) {
    return yamlService.update(yamlPayload, accountId);
  }

  /**
   * Save git config rest response.
   *
   * @param accountId   the account id
   * @param yamlGitSync the yaml git sync
   * @return the rest response
   */
  @POST
  @Path("git-config")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = PermissionType.ACCOUNT_MANAGEMENT)
  public RestResponse<YamlGitConfig> saveGitConfig(
      @QueryParam("accountId") String accountId, YamlGitConfig yamlGitSync) {
    yamlGitSync.setAccountId(accountId);
    yamlGitSync.setAppId(Base.GLOBAL_APP_ID);
    return new RestResponse<>(yamlGitService.save(yamlGitSync));
  }

  /**
   * Gets the yaml git sync info by uuid
   *
   * @param entityId  the entity id
   * @param accountId the account id
   * @return the rest response
   */
  @GET
  @Path("git-config/{entityId}")
  @Timed
  @ExceptionMetered
  public RestResponse<YamlGitConfig> get(
      @PathParam("entityId") String entityId, @QueryParam("accountId") String accountId) {
    return new RestResponse<>(yamlGitService.get(accountId, entityId));
  }

  /**
   * Update git config rest response.
   *
   * @param accountId   the account id
   * @param yamlGitSync the yaml git sync
   * @return the rest response
   */
  @PUT
  @Path("git-config/{entityId}")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = PermissionType.ACCOUNT_MANAGEMENT)
  public RestResponse<YamlGitConfig> updateGitConfig(
      @QueryParam("accountId") String accountId, YamlGitConfig yamlGitSync) {
    yamlGitSync.setAccountId(accountId);
    yamlGitSync.setAppId(Base.GLOBAL_APP_ID);
    return new RestResponse<>(yamlGitService.update(yamlGitSync));
  }

  /**
   * Webhook catcher rest response.
   *
   * @param accountId          the account id
   * @param entityToken        the entity token
   * @param yamlWebHookPayload the yaml web hook payload
   * @return the rest response
   */
  @POST
  @Path("webhook/{entityToken}")
  @Timed
  @ExceptionMetered
  @PublicApi
  public RestResponse webhookCatcher(@QueryParam("accountId") String accountId,
      @PathParam("entityToken") String entityToken, YamlWebHookPayload yamlWebHookPayload) {
    yamlGitService.processWebhookPost(accountId, entityToken, yamlWebHookPayload);
    return new RestResponse();
  }

  /**
   * Gets existing or new webhook
   *
   * @param entityId  the uuid of the entity
   * @param accountId the account id
   * @return the git sync webhook
   */
  @GET
  @Path("webhook/{entityId}")
  @Timed
  @ExceptionMetered
  public RestResponse<GitSyncWebhook> getWebhook(
      @PathParam("entityId") String entityId, @QueryParam("accountId") String accountId) {
    try {
      entityId = URLDecoder.decode(entityId, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      logger.error("", e);
    }
    return new RestResponse<>(yamlGitService.getWebhook(entityId, accountId));
  }

  /**
   * Run a full sync dry-run
   *
   * @param accountId the account id
   * @return
   */
  @GET
  @Path("full-sync-dry-run")
  @Timed
  @ExceptionMetered
  public RestResponse fullSyncDryRun(@QueryParam("accountId") String accountId, @QueryParam("token") String token,
      @QueryParam("queryAllAccounts") @DefaultValue("false") boolean queryAllAccounts) {
    validateUserAndToken(token);
    if (queryAllAccounts) {
      yamlGitService.performFullSyncDryRunOnAllAccounts();
    } else {
      yamlGitService.performFullSyncDryRun(accountId);
    }
    return new RestResponse();
  }

  private void validateUserAndToken(String token) {
    authService.validateToken(token);
    User existingUser = UserThreadLocal.get();
    if (existingUser == null) {
      throw new InvalidRequestException("Invalid User");
    }
    if (!harnessUserGroupService.isHarnessSupportUser(existingUser.getUuid())) {
      throw new InvalidRequestException("User needs to be a harness support user for this action");
    }
  }

  @GET
  @Path("get-all-yaml-errors")
  @Timed
  @ExceptionMetered
  public RestResponse<List<String>> getAllYamlErrors(@QueryParam("accountId") String accountId,
      @QueryParam("token") String token,
      @QueryParam("queryAllAccounts") @DefaultValue("false") boolean queryAllAccounts) {
    validateUserAndToken(token);
    List<String> errorLog = queryAllAccounts ? yamlGitService.getAllYamlErrorsForAllAccounts()
                                             : yamlGitService.getAllYamlErrorsForAccount(accountId);
    PageResponse pageResponse = aPageResponse().withResponse(errorLog).build();
    return RestResponse.Builder.aRestResponse().withResource(pageResponse.getResponse()).build();
  }

  @GET
  @Path("git-sync-errors")
  @Timed
  @ExceptionMetered
  public RestResponse<List<GitSyncError>> listGitSyncErrors(@QueryParam("accountId") String accountId) {
    return yamlGitService.listGitSyncErrors(accountId);
  }

  @POST
  @Path("git-sync-errors")
  @Timed
  @ExceptionMetered
  public RestResponse fixGitSyncError(@QueryParam("accountId") String accountId, YamlPayload yamlPayload) {
    return yamlGitService.fixGitSyncErrors(accountId, yamlPayload.getPath(), yamlPayload.getYaml());
  }

  @POST
  @Path("git-sync-errors-discard-all")
  @Timed
  @ExceptionMetered
  public RestResponse discardGitSyncError(@QueryParam("accountId") String accountId) {
    return yamlGitService.discardAllGitSyncError(accountId);
  }

  @POST
  @Path("git-sync-errors-discard-selected")
  @Timed
  @ExceptionMetered
  public RestResponse discardGitSyncError(@QueryParam("accountId") String accountId, List<String> errorIds) {
    return yamlGitService.discardGitSyncErrorsForGivenIds(accountId, errorIds);
  }

  @POST
  @Path("yaml-as-zip")
  @Consumes(MULTIPART_FORM_DATA)
  @Timed
  @ExceptionMetered
  public RestResponse<String> processYamlFilesAsZip(@QueryParam("accountId") @NotEmpty String accountId,
      @QueryParam("yamlPath") @Optional String yamlPath, @FormDataParam("file") InputStream uploadedInputStream,
      @FormDataParam("file") FormDataContentDisposition fileDetail) throws IOException, YamlProcessingException {
    logger.debug("accountId: {}, fileDetail: {}, yamlPath: {}", accountId, fileDetail, yamlPath);

    return yamlService.processYamlFilesAsZip(accountId,
        new BoundedInputStream(uploadedInputStream, configuration.getFileUploadLimits().getAppContainerLimit()),
        yamlPath);
  }

  @POST
  @Path("clone")
  @Timed
  @ExceptionMetered
  public RestResponse clone(@QueryParam("accountId") String accountId, @QueryParam("appId") String appId,
      @QueryParam("entityType") String entityType, @QueryParam("entityId") String entityId,
      @QueryParam("newEntityName") String newEntityName) {
    return yamlCloneService.cloneEntityUsingYaml(accountId, appId, false, entityType, entityId, newEntityName);
  }
}
