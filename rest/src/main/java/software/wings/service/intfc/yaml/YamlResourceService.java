package software.wings.service.intfc.yaml;

import org.hibernate.validator.constraints.NotEmpty;
import software.wings.beans.Environment;
import software.wings.beans.Pipeline;
import software.wings.beans.RestResponse;
import software.wings.beans.SettingAttribute;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.command.ServiceCommand;
import software.wings.yaml.YamlPayload;

/**
 * Yaml Directory Service.
 *
 * @author bsollish
 */
public interface YamlResourceService {
  /**
   * Find by app, service and service command ids.
   *
   * @param appId     the app id
   * @param serviceCommandId the service command id
   * @return the service command
   */
  RestResponse<YamlPayload> getServiceCommand(@NotEmpty String appId, @NotEmpty String serviceCommandId);

  /**
   * Update by app, service and service command ids and yaml payload
   *
   * @param appId     the app id
   * @param serviceCommandId the service command id
   * @param yamlPayload the yaml version of the service command
   * @return the service command
   */
  RestResponse<ServiceCommand> updateServiceCommand(
      @NotEmpty String appId, @NotEmpty String serviceCommandId, YamlPayload yamlPayload, boolean deleteEnabled);

  /**
   * Gets the yaml version of a pipeline by pipelineId
   *
   * @param appId     the app id
   * @param pipelineId the pipeline id
   * @return the rest response
   */
  public RestResponse<YamlPayload> getPipeline(String appId, String pipelineId);

  /**
   * Update a pipeline that is sent as Yaml (in a JSON "wrapper")
   *
   * @param appId     the app id
   * @param pipelineId the pipeline id
   * @param yamlPayload the yaml version of the service command
   * @return the rest response
   */
  public RestResponse<Pipeline> updatePipeline(
      String appId, String pipelineId, YamlPayload yamlPayload, boolean deleteEnabled);

  /**
   * Gets the yaml version of a trigger by artifactStreamId
   *
   * @param appId     the app id
   * @param artifactStreamId the artifact stream id
   * @return the rest response
   */
  public RestResponse<YamlPayload> getTrigger(String appId, String artifactStreamId);

  /**
   * Update a trigger that is sent as Yaml (in a JSON "wrapper")
   *
   * @param appId     the app id
   * @param artifactStreamId the artifact stream id
   * @param yamlPayload the yaml version of the service command
   * @return the rest response
   */
  public RestResponse<ArtifactStream> updateTrigger(
      String appId, String artifactStreamId, YamlPayload yamlPayload, boolean deleteEnabled);

  /**
   * Gets the yaml for a workflow
   *
   * @param appId     the app id
   * @param workflowId the workflow id
   * @return the rest response
   */
  public RestResponse<YamlPayload> getWorkflow(String appId, String workflowId);

  /**
   * Gets all the setting attributes of a given type by accountId
   *
   * @param accountId   the account id
   * @param type        the SettingVariableTypes
   * @return the rest response
   */
  public RestResponse<YamlPayload> getSettingAttributesList(String accountId, String type);

  /**
   * Gets the yaml for a setting attribute by accountId and uuid
   *
   * @param accountId the account id
   * @param uuid      the uid of the setting attribute
   * @return the rest response
   */
  public RestResponse<YamlPayload> getSettingAttribute(String accountId, String uuid);

  /**
   * Update setting attribute sent as Yaml (in a JSON "wrapper")
   *
   * @param accountId   the account id
   * @param uuid        the uid of the setting attribute
   * @param type        the SettingVariableTypes
   * @param yamlPayload the yaml version of setup
   * @return the rest response
   */
  public RestResponse<SettingAttribute> updateSettingAttribute(
      String accountId, String uuid, String type, YamlPayload yamlPayload, boolean deleteEnabled);

  /**
   * Gets the yaml version of an environment by envId
   *
   * @param appId   the app id
   * @param envId   the environment id
   * @return the rest response
   */
  public RestResponse<YamlPayload> getEnvironment(String appId, String envId);

  /**
   * Update a environment that is sent as Yaml (in a JSON "wrapper")
   *
   * @param envId  the environment id
   * @param yamlPayload the yaml version of environment
   * @return the rest response
   */
  public RestResponse<Environment> updateEnvironment(
      String appId, String envId, YamlPayload yamlPayload, boolean deleteEnabled);
}
