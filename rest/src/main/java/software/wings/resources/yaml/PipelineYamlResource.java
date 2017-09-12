package software.wings.resources.yaml;

import static software.wings.security.PermissionAttribute.ResourceType.APPLICATION;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Pipeline;
import software.wings.beans.RestResponse;
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
 * Pipeline Yaml Resource class.
 *
 * @author bsollish
 */
@Api("/pipelineYaml")
@Path("/pipelineYaml")
@Produces("application/json")
@AuthRule(APPLICATION)
public class PipelineYamlResource {
  private YamlResourceService yamlResourceService;

  private final Logger logger = LoggerFactory.getLogger(getClass());

  /**
   * Instantiates new resources
   *
   * @param yamlResourceService     the yaml resource service
   */
  @Inject
  public PipelineYamlResource(YamlResourceService yamlResourceService) {
    this.yamlResourceService = yamlResourceService;
  }

  /**
   * Gets the yaml version of a pipeline by pipelineId
   *
   * @param appId     the app id
   * @param pipelineId the pipeline id
   * @return the rest response
   */
  @GET
  @Path("/{appId}/{pipelineId}")
  @Timed
  @ExceptionMetered
  public RestResponse<YamlPayload> get(@PathParam("appId") String appId, @PathParam("pipelineId") String pipelineId) {
    return yamlResourceService.getPipeline(appId, pipelineId);
  }

  /**
   * Update a pipeline that is sent as Yaml (in a JSON "wrapper")
   *
   * @param appId     the app id
   * @param pipelineId the pipeline id
   * @param yamlPayload the yaml version of the service command
   * @return the rest response
   */
  @PUT
  @Path("/{appId}/{serviceId}/{serviceCommandId}")
  @Timed
  @ExceptionMetered
  public RestResponse<Pipeline> update(@PathParam("appId") String appId, @PathParam("pipelineId") String pipelineId,
      YamlPayload yamlPayload, @QueryParam("deleteEnabled") @DefaultValue("false") boolean deleteEnabled) {
    return new RestResponse<>(yamlResourceService.updatePipeline(appId, pipelineId, yamlPayload, deleteEnabled));
  }
}