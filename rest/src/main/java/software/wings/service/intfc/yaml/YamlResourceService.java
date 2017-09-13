package software.wings.service.intfc.yaml;

import org.hibernate.validator.constraints.NotEmpty;
import software.wings.beans.Pipeline;
import software.wings.beans.RestResponse;
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
   * @param serviceId the service id
   * @param serviceCommandId the service command id
   * @return the service command
   */
  RestResponse<YamlPayload> getServiceCommand(
      @NotEmpty String appId, @NotEmpty String serviceId, @NotEmpty String serviceCommandId);

  /**
   * Update by app, service and service command ids and yaml payload
   *
   * @param appId     the app id
   * @param serviceId the service id
   * @param serviceCommandId the service command id
   * @param yamlPayload the yaml version of the service command
   * @return the service command
   */
  ServiceCommand updateServiceCommand(@NotEmpty String appId, @NotEmpty String serviceId,
      @NotEmpty String serviceCommandId, YamlPayload yamlPayload, boolean deleteEnabled);

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
  public Pipeline updatePipeline(String appId, String pipelineId, YamlPayload yamlPayload, boolean deleteEnabled);

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
  public ArtifactStream updateTrigger(
      String appId, String artifactStreamId, YamlPayload yamlPayload, boolean deleteEnabled);
}
