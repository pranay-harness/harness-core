package software.wings.resources.yaml;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import software.wings.beans.Environment;
import software.wings.beans.Pipeline;
import software.wings.beans.RestResponse;
import software.wings.beans.SettingAttribute;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.command.ServiceCommand;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.AuthRule;
import software.wings.service.intfc.yaml.YamlResourceService;
import software.wings.yaml.YamlPayload;

import javax.inject.Inject;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

/**
 * Created by bsollish
 */
@Api("setupAsCode/yaml")
@Path("setupAsCode/yaml")
@Produces(APPLICATION_JSON)
@AuthRule(ResourceType.SETTING)
public class YamlResource {
  private YamlResourceService yamlResourceService;

  /**
   * Instantiates a new service resource.
   *
   * @param yamlResourceService the yaml resource service
   */
  @Inject
  public YamlResource(YamlResourceService yamlResourceService) {
    this.yamlResourceService = yamlResourceService;
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
   * @param appId       the app id
   * @param pipelineId  the pipeline id
   * @param yamlPayload the yaml version of the service command
   * @return the rest response
   */
  @PUT
  @Path("/pipelines/{pipelines}")
  @Timed
  @ExceptionMetered
  public RestResponse<Pipeline> updatePipeline(@QueryParam("appId") String appId,
      @PathParam("pipelineId") String pipelineId, YamlPayload yamlPayload,
      @QueryParam("deleteEnabled") @DefaultValue("false") boolean deleteEnabled) {
    return yamlResourceService.updatePipeline(appId, pipelineId, yamlPayload, deleteEnabled);
  }

  /**
   * Gets the yaml version of a service command by serviceCommandId
   *
   * @param appId            the app id
   * @param serviceCommandId the service command id
   * @return the rest response
   */
  @GET
  @Path("/serviceCommands/{serviceCommandId}")
  @Timed
  @ExceptionMetered
  public RestResponse<YamlPayload> getServiceCommand(
      @QueryParam("appId") String appId, @PathParam("serviceCommandId") String serviceCommandId) {
    return yamlResourceService.getServiceCommand(appId, serviceCommandId);
  }

  /**
   * Update a service command that is sent as Yaml (in a JSON "wrapper")
   *
   * @param appId            the app id
   * @param serviceCommandId the service command id
   * @param yamlPayload      the yaml version of the service command
   * @return the rest response
   */
  @PUT
  @Path("/serviceCommands/{serviceCommandId}")
  @Timed
  @ExceptionMetered
  public RestResponse<ServiceCommand> updateServiceCommand(@QueryParam("appId") String appId,
      @PathParam("serviceCommandId") String serviceCommandId, YamlPayload yamlPayload,
      @QueryParam("deleteEnabled") @DefaultValue("false") boolean deleteEnabled) {
    return yamlResourceService.updateServiceCommand(appId, serviceCommandId, yamlPayload, deleteEnabled);
  }

  /**
   * Gets all the setting attributes of a given type by accountId
   *
   * @param accountId   the account id
   * @param type        the SettingVariableTypes
   * @return the rest response
   */
  @GET
  @Path("/settingAttributes")
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
  @Path("/settingAttributes/{uuid}")
  @Timed
  @ExceptionMetered
  public RestResponse<YamlPayload> getSettingAttribute(
      @QueryParam("accountId") String accountId, @PathParam("uuid") String uuid) {
    return yamlResourceService.getSettingAttribute(accountId, uuid);
  }

  /**
   * Update setting attribute sent as Yaml (in a JSON "wrapper")
   *
   * @param accountId   the account id
   * @param uuid        the uid of the setting attribute
   * @param type        the SettingVariableTypes
   * @param yamlPayload the yaml version of setup
   * @return the rest response
   */
  @PUT
  @Path("/settingAttributes/{uuid}")
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
   * @param appId   the app id
   * @param envId   the environment id
   * @return the rest response
   */
  @GET
  @Path("/environments/{envId}")
  @Timed
  @ExceptionMetered
  public RestResponse<YamlPayload> get(@QueryParam("appId") String appId, @PathParam("envId") String envId) {
    return yamlResourceService.getEnvironment(appId, envId);
  }

  /**
   * Update a environment that is sent as Yaml (in a JSON "wrapper")
   *
   * @param envId  the environment id
   * @param yamlPayload the yaml version of environment
   * @return the rest response
   */
  @PUT
  @Path("/environments/{envId}")
  @Timed
  @ExceptionMetered
  public RestResponse<Environment> update(@QueryParam("appId") String appId, @PathParam("envId") String envId,
      YamlPayload yamlPayload, @QueryParam("deleteEnabled") @DefaultValue("false") boolean deleteEnabled) {
    return yamlResourceService.updateEnvironment(appId, envId, yamlPayload, deleteEnabled);
  }
}
