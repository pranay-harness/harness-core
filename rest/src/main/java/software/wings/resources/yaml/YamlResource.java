package software.wings.resources.yaml;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import software.wings.beans.Application;
import software.wings.beans.Base;
import software.wings.beans.Environment;
import software.wings.beans.Pipeline;
import software.wings.beans.RestResponse;
import software.wings.beans.Service;
import software.wings.beans.SettingAttribute;
import software.wings.beans.Workflow;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.command.ServiceCommand;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.AuthRule;
import software.wings.security.annotations.PublicApi;
import software.wings.service.impl.yaml.YamlWebHookPayload;
import software.wings.service.intfc.yaml.AppYamlResourceService;
import software.wings.service.intfc.yaml.YamlArtifactStreamService;
import software.wings.service.intfc.yaml.YamlDirectoryService;
import software.wings.service.intfc.yaml.YamlGitService;
import software.wings.service.intfc.yaml.YamlResourceService;
import software.wings.service.intfc.yaml.sync.YamlSyncService;
import software.wings.yaml.BaseYaml;
import software.wings.yaml.YamlPayload;
import software.wings.yaml.directory.DirectoryNode;
import software.wings.yaml.gitSync.GitSyncWebhook;
import software.wings.yaml.gitSync.YamlGitConfig;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import javax.inject.Inject;
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
@AuthRule(ResourceType.SETTING)
public class YamlResource {
  private YamlResourceService yamlResourceService;
  private AppYamlResourceService appYamlResourceService;
  private YamlArtifactStreamService yamlArtifactStreamService;
  private YamlSyncService yamlSyncService;
  private YamlDirectoryService yamlDirectoryService;
  private YamlGitService yamlGitSyncService;

  /**
   * Instantiates a new service resource.
   *
   * @param yamlResourceService        the yaml resource servicewe
   * @param appYamlResourceService     the app yaml resource service
   * @param yamlDirectoryService       the yaml directory service
   * @param yamlArtifactStreamService  the yaml artifact stream service
   * @param yamlSyncService            the yaml sync service
   * @param yamlGitSyncService
   */
  @Inject
  public YamlResource(YamlResourceService yamlResourceService, AppYamlResourceService appYamlResourceService,
      YamlDirectoryService yamlDirectoryService, YamlArtifactStreamService yamlArtifactStreamService,
      YamlSyncService yamlSyncService, YamlGitService yamlGitSyncService) {
    this.yamlResourceService = yamlResourceService;
    this.appYamlResourceService = appYamlResourceService;
    this.yamlDirectoryService = yamlDirectoryService;
    this.yamlArtifactStreamService = yamlArtifactStreamService;
    this.yamlSyncService = yamlSyncService;
    this.yamlGitSyncService = yamlGitSyncService;
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
  public RestResponse<YamlPayload> getWorkflow(
      @QueryParam("appId") String appId, @PathParam("workflowId") String workflowId) {
    return yamlResourceService.getWorkflow(appId, workflowId);
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
   * @param appId            the app id
   * @param artifactStreamId the artifact stream id
   * @param yamlPayload      the yaml version of the service command
   * @param deleteEnabled    the delete enabled
   * @return the rest response
   */
  @PUT
  @Path("/triggers/{artifactStreamId}")
  @Timed
  @ExceptionMetered
  public RestResponse<ArtifactStream> updateTrigger(@QueryParam("appId") String appId,
      @PathParam("artifactStreamId") String artifactStreamId, YamlPayload yamlPayload,
      @QueryParam("deleteEnabled") @DefaultValue("false") boolean deleteEnabled) {
    return yamlResourceService.updateTrigger(appId, artifactStreamId, yamlPayload, deleteEnabled);
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
  public RestResponse<Pipeline> updatePipeline(
      @QueryParam("accountId") String accountId, @QueryParam("appId") String appId, YamlPayload yamlPayload) {
    return yamlResourceService.updatePipeline(accountId, yamlPayload);
  }

  @PUT
  @Path("/workflows/{workflowId}")
  @Timed
  @ExceptionMetered
  public RestResponse<Workflow> updateWorkflow(
      @QueryParam("accountId") String accountId, @QueryParam("appId") String appId, YamlPayload yamlPayload) {
    return yamlResourceService.updateWorkflow(accountId, yamlPayload);
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
  public RestResponse<ServiceCommand> updateServiceCommand(
      @QueryParam("accountId") String accountId, YamlPayload yamlPayload) {
    return yamlResourceService.updateServiceCommand(accountId, yamlPayload);
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
  public RestResponse<YamlPayload> getSettingAttributesList(
      @QueryParam("accountId") String accountId, @QueryParam("type") String type) {
    return yamlResourceService.getSettingAttributesList(accountId, type);
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
  public RestResponse<YamlPayload> getSettingAttribute(
      @QueryParam("accountId") String accountId, @PathParam("uuid") String uuid) {
    return yamlResourceService.getSettingAttribute(accountId, uuid);
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
  public RestResponse<SettingAttribute> updateSettingAttribute(@QueryParam("accountId") String accountId,
      @PathParam("uuid") String uuid, @QueryParam("type") String type, YamlPayload yamlPayload,
      @QueryParam("deleteEnabled") @DefaultValue("false") boolean deleteEnabled) {
    return yamlResourceService.updateSettingAttribute(accountId, uuid, type, yamlPayload, deleteEnabled);
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
  public RestResponse<YamlPayload> getEnvironment(@QueryParam("appId") String appId, @PathParam("envId") String envId) {
    return yamlResourceService.getEnvironment(appId, envId);
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
  public RestResponse<Environment> updateEnvironment(
      @QueryParam("accountId") String accountId, @QueryParam("appId") String appId, YamlPayload yamlPayload) {
    return yamlResourceService.updateEnvironment(accountId, yamlPayload);
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
  public RestResponse<Service> updateService(
      @QueryParam("accountId") String accountId, @QueryParam("appId") String appId, YamlPayload yamlPayload) {
    return yamlResourceService.updateService(accountId, yamlPayload);
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
  //-------------------------------------
  // TODO - I need an endpoint, at least temporarily, that will allow me to kick off pushing the full setup directory
  // "tree" to a synced git repo
  @GET
  @Path("push-directory")
  @Timed
  @ExceptionMetered
  public RestResponse pushDirectory(@QueryParam("accountId") String accountId) {
    yamlGitSyncService.pushDirectory(accountId);
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
  public RestResponse<Base> updateInfraMapping(@QueryParam("appId") String appId,
      @QueryParam("accountId") String accountId, YamlPayload yamlPayload,
      @QueryParam("deleteEnabled") @DefaultValue("false") boolean deleteEnabled) {
    return yamlSyncService.update(yamlPayload, accountId);
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
    return yamlSyncService.update(yamlPayload, accountId);
  }

  /**
   * Generic api to get any yaml file
   *
   * @param accountId the account id
   * @param yamlPath  the yaml path
   * @return rest response
   */
  @GET
  @Timed
  @ExceptionMetered
  public RestResponse<BaseYaml> getYaml(
      @QueryParam("accountId") String accountId, @QueryParam("yamlPath") String yamlPath) {
    return yamlSyncService.getYaml(accountId, yamlPath);
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
  public RestResponse<YamlGitConfig> saveGitConfig(
      @QueryParam("accountId") String accountId, YamlGitConfig yamlGitSync) {
    yamlGitSync.setAccountId(accountId);
    yamlGitSync.setAppId(Base.GLOBAL_APP_ID);
    return new RestResponse<>(yamlGitSyncService.save(yamlGitSync));
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
    return new RestResponse<>(yamlGitSyncService.get(accountId, entityId));
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
  public RestResponse<YamlGitConfig> updateGitConfig(
      @QueryParam("accountId") String accountId, YamlGitConfig yamlGitSync) {
    yamlGitSync.setAccountId(accountId);
    yamlGitSync.setAppId(Base.GLOBAL_APP_ID);
    return new RestResponse<>(yamlGitSyncService.update(yamlGitSync));
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
    yamlGitSyncService.processWebhookPost(accountId, entityToken, yamlWebHookPayload);
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
      e.printStackTrace();
    }
    return new RestResponse<>(yamlGitSyncService.getWebhook(entityId, accountId));
  }
}
