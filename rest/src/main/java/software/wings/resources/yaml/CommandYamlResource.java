package software.wings.resources.yaml;

import static software.wings.security.PermissionAttribute.ResourceType.APPLICATION;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.RestResponse;
import software.wings.beans.command.ServiceCommand;
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
 * Command Yaml Resource class.
 *
 * @author bsollish
 */
@Api("/commandYaml")
@Path("/commandYaml")
@Produces("application/json")
@AuthRule(APPLICATION)
public class CommandYamlResource {
  private YamlResourceService yamlResourceService;

  private final Logger logger = LoggerFactory.getLogger(getClass());

  /**
   * Instantiates new resources
   *
   * @param yamlResourceService     the yaml resource service
   */
  @Inject
  public CommandYamlResource(YamlResourceService yamlResourceService) {
    this.yamlResourceService = yamlResourceService;
  }

  /**
   * Gets the yaml version of a service by serviceId
   *
   * @param appId     the app id
   * @param serviceId the service id
   * @param serviceCommandId the service command id
   * @return the rest response
   */
  @GET
  @Path("/{appId}/{serviceId}/{serviceCommandId}")
  @Timed
  @ExceptionMetered
  public RestResponse<YamlPayload> get(@PathParam("appId") String appId, @PathParam("serviceId") String serviceId,
      @PathParam("serviceCommandId") String serviceCommandId) {
    return yamlResourceService.getServiceCommand(appId, serviceId, serviceCommandId);
  }

  /**
   * Update a service that is sent as Yaml (in a JSON "wrapper")
   *
   * @param appId     the app id
   * @param serviceId the service id
   * @param serviceCommandId the service command id
   * @param yamlPayload the yaml version of the service command
   * @return the rest response
   */
  @PUT
  @Path("/{appId}/{serviceId}/{serviceCommandId}")
  @Timed
  @ExceptionMetered
  public RestResponse<ServiceCommand> update(@PathParam("appId") String appId, @PathParam("serviceId") String serviceId,
      @PathParam("serviceCommandId") String serviceCommandId, YamlPayload yamlPayload,
      @QueryParam("deleteEnabled") @DefaultValue("false") boolean deleteEnabled) {
    return new RestResponse<>(
        yamlResourceService.updateServiceCommand(appId, serviceId, serviceCommandId, yamlPayload, deleteEnabled));
  }
}