package software.wings.resources;

import static software.wings.security.PermissionAttribute.ResourceType.APPLICATION;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.RestResponse;
import software.wings.security.annotations.AuthRule;
import software.wings.yaml.YamlHistory;
import software.wings.yaml.YamlType;
import software.wings.yaml.YamlVersion;
import software.wings.yaml.YamlVersionDetails;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

/**
 * Configuration as Code Resource class.
 *
 * @author bsollish
 */
@Api("/yamlVersion")
@Path("/yamlVersion")
@Produces("application/json")
@AuthRule(APPLICATION)
public class YamlVersionResource {
  // private AppService appService;
  // private ServiceResourceService serviceResourceService;

  private final Logger logger = LoggerFactory.getLogger(getClass());

  /**
   * Instantiates a new app yaml resource.
   *
   * @param appService             the app service
   * @param serviceResourceService the service (resource) service
   */
  @Inject
  public YamlVersionResource() {}

  /**
   * Gets the Yaml version by entityId and versionId
   *
   * @param accountId
   * @param versionId
   * @return the rest response
   */
  @GET
  @Path("/{accountId}")
  @Timed
  @ExceptionMetered
  public RestResponse<YamlVersionDetails> get(
      @PathParam("accountId") String accountId, @QueryParam("versionId") String versionId) {
    RestResponse rr = new RestResponse<>();

    YamlVersionDetails yvd = new YamlVersionDetails();

    //------- ADD DUMMY DATA -------------
    yvd.setVersion(1);
    yvd.setInEffectStart(String.valueOf(System.currentTimeMillis()));
    yvd.setInEffectEnd(String.valueOf(System.currentTimeMillis() + 1000000));
    yvd.setType(YamlType.SERVICE);
    yvd.setEntityId("serv6789");
    yvd.setYamlVersionId("yv12345");
    yvd.setYaml("name: Login\n"
        + "artifactType: WAR\n"
        + "description: \"The Login service\"\n"
        + "service-commands: \n"
        + "  - start\n"
        + "  - install\n"
        + "  - stop");
    //------------------------------------

    rr.setResource(yvd);

    return rr;
  }
}

// yv1.setYaml("name: Login\n" + "artifactType: WAR\n" + "description: \"The Login service\"\n" + "service-commands: \n"
// + "  - start\n" + "  - install\n" + "  - stop"); yv2.setYaml("name: Login\n" + "artifactType: WAR\n" + "description:
// \"The NEW description\"\n" + "service-commands: \n" + "  - start\n" + "  - install\n" + "  - stop");
// yv3.setYaml("name: LoginXXX\n" + "artifactType: WAR\n" + "description: \"The NEW description\"\n" +
// "service-commands: \n" + "  - start\n" + "  - install\n" + "  - stop\n" + "  - newCommand");