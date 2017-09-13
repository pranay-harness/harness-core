package software.wings.resources.yaml;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import software.wings.beans.RestResponse;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.AuthRule;
import software.wings.service.intfc.yaml.YamlResourceService;
import software.wings.yaml.YamlPayload;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

/**
 * Created by bsollish
 */
@Api("yaml")
@Path("yaml")
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
   * @param appId     the app id
   * @param workflowId the workflow id
   * @return the rest response
   */
  @GET
  @Path("/workflows/{workflowId}")
  @Timed
  @ExceptionMetered
  public RestResponse<YamlPayload> get(@QueryParam("appId") String appId, @PathParam("workflowId") String workflowId) {
    return yamlResourceService.getWorkflow(appId, workflowId);
  }
}
