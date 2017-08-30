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
import software.wings.yaml.YamlVersionList;

import java.util.Optional;
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
@Api("/yamlHistory")
@Path("/yamlHistory")
@Produces("application/json")
@AuthRule(APPLICATION)
public class YamlHistoryResource {
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
  public YamlHistoryResource() {}

  /**
   * Gets the Yaml history by entityId
   *
   * @param accountId
   * @param entityId
   * @param yamlType
   * @param versionId - optional, if present than we return (a single) YamlVersion, otherwise YamlHistory
   * @return the rest response
   */
  @GET
  @Path("/{accountId}")
  @Timed
  @ExceptionMetered
  public RestResponse<YamlHistory> get(@PathParam("accountId") String accountId,
      @QueryParam("entityId") String entityId, @QueryParam("type") YamlType yamlType,
      @QueryParam("versionId") Optional<String> versionId) {
    if (versionId.isPresent()) {
      return getYamlVersion(accountId, entityId, yamlType, versionId.get());
    }

    return getYamlVersionList(accountId, entityId, yamlType);
  }

  private RestResponse<YamlHistory> getYamlVersion(
      String accountId, String entityId, YamlType yamlType, String versionId) {
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

  private RestResponse<YamlHistory> getYamlVersionList(String accountId, String entityId, YamlType yamlType) {
    RestResponse rr = new RestResponse<>();

    YamlVersionList yvList = new YamlVersionList();

    //------- ADD DUMMY DATA -------------

    YamlVersion yv1 = new YamlVersion();
    YamlVersion yv2 = new YamlVersion();
    YamlVersion yv3 = new YamlVersion();

    yv1.setVersion(1);
    yv1.setInEffectStart(String.valueOf(System.currentTimeMillis()));
    yv1.setInEffectEnd(String.valueOf(System.currentTimeMillis() + 1000000));
    yv1.setType(YamlType.SERVICE);
    yv1.setEntityId("serv6789");
    yv1.setYamlVersionId("yv12345");
    yvList.addVersion(yv1);

    yv2.setVersion(2);
    yv2.setInEffectStart(String.valueOf(System.currentTimeMillis() + 1000001));
    yv2.setInEffectEnd(String.valueOf(System.currentTimeMillis() + 2000000));
    yv2.setType(YamlType.SERVICE);
    yv2.setEntityId("serv6789");
    yv2.setYamlVersionId("yv23456");
    yvList.addVersion(yv2);

    yv3.setVersion(3);
    yv3.setInEffectStart(String.valueOf(System.currentTimeMillis() + 2000001));
    yv3.setInEffectEnd(String.valueOf(System.currentTimeMillis() + 3000000));
    yv3.setType(YamlType.SERVICE);
    yv3.setEntityId("serv6789");
    yv3.setYamlVersionId("yv34567");
    yvList.addVersion(yv3);

    //------------------------------------

    rr.setResource(yvList);

    return rr;
  }
}
