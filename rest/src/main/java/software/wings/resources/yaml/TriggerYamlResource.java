package software.wings.resources.yaml;

import static software.wings.security.PermissionAttribute.ResourceType.APPLICATION;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import software.wings.beans.RestResponse;
import software.wings.beans.artifact.ArtifactStream;
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
 * Trigger (ArtifactStream) Yaml Resource class.
 *
 * @author bsollish
 */
@Api("/triggerYaml")
@Path("/triggerYaml")
@Produces("application/json")
@AuthRule(APPLICATION)
public class TriggerYamlResource {
  private YamlResourceService yamlResourceService;

  /**
   * Instantiates new resources
   *
   * @param yamlResourceService     the yaml resource service
   */
  @Inject
  public TriggerYamlResource(YamlResourceService yamlResourceService) {
    this.yamlResourceService = yamlResourceService;
  }

  /**
   * Gets the yaml version of a trigger by artifactStreamId
   *
   * @param appId     the app id
   * @param artifactStreamId the artifact stream id
   * @return the rest response
   */
  @GET
  @Path("/{appId}/{artifactStreamId}")
  @Timed
  @ExceptionMetered
  public RestResponse<YamlPayload> get(
      @PathParam("appId") String appId, @PathParam("artifactStreamId") String artifactStreamId) {
    return yamlResourceService.getTrigger(appId, artifactStreamId);
  }

  /**
   * Update a trigger that is sent as Yaml (in a JSON "wrapper")
   *
   * @param appId     the app id
   * @param artifactStreamId the artifact stream id
   * @param yamlPayload the yaml version of the service command
   * @return the rest response
   */
  @PUT
  @Path("/{appId}/{serviceId}/{artifactStreamId}")
  @Timed
  @ExceptionMetered
  public RestResponse<ArtifactStream> update(@PathParam("appId") String appId,
      @PathParam("artifactStreamId") String artifactStreamId, YamlPayload yamlPayload,
      @QueryParam("deleteEnabled") @DefaultValue("false") boolean deleteEnabled) {
    return new RestResponse<>(yamlResourceService.updateTrigger(appId, artifactStreamId, yamlPayload, deleteEnabled));
  }
}