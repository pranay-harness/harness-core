package software.wings.resources.yaml;

import static software.wings.security.PermissionAttribute.ResourceType.APPLICATION;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.RestResponse;
import software.wings.security.annotations.Scope;
import software.wings.service.intfc.yaml.YamlHistoryService;
import software.wings.yaml.YamlHistory;
import software.wings.yaml.YamlVersion;
import software.wings.yaml.YamlVersion.Type;
import software.wings.yaml.YamlVersionList;

import java.util.Optional;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

/**
 * Configuration as Code Resource class.
 *
 * @author bsollish
 */
@Api("/yaml-history")
@Path("/yaml-history")
@Produces("application/json")
@Scope(APPLICATION)
public class YamlHistoryResource {
  private YamlHistoryService yamlHistoryService;

  private static final Logger logger = LoggerFactory.getLogger(YamlHistoryResource.class);

  /**
   * Instantiates a new app yaml resource.
   *
   * @param yamlHistoryService  the yaml history service
   */
  @Inject
  public YamlHistoryResource(YamlHistoryService yamlHistoryService) {
    this.yamlHistoryService = yamlHistoryService;
  }

  /**
   * Gets the Yaml history by entityId
   *
   * @param accountId
   * @param entityId
   * @param type
   * @param versionId - optional, if present than we return (a single) YamlVersion, otherwise YamlHistory
   * @return the rest response
   */
  @GET
  @Path("/{accountId}")
  @Timed
  @ExceptionMetered
  public RestResponse<YamlHistory> get(@PathParam("accountId") String accountId,
      @QueryParam("entityId") String entityId, @QueryParam("type") Type type,
      @QueryParam("versionId") Optional<String> versionId) {
    RestResponse rr = new RestResponse<>();

    if (versionId.isPresent()) {
      YamlVersion yv = yamlHistoryService.get(versionId.get());
      rr.setResource(yv);
    } else {
      YamlVersionList yvList = new YamlVersionList(yamlHistoryService.getList(entityId, type));
      rr.setResource(yvList);
    }

    return rr;
  }

  /**
   * Save.
   *
   * @param accountId
   * @param entityId
   * @param type
   * @return the rest response
   */
  @POST
  @Path("/{accountId}")
  @Timed
  @ExceptionMetered
  public RestResponse<YamlVersion> save(@PathParam("accountId") String accountId,
      @QueryParam("entityId") String entityId, @QueryParam("type") Type type, YamlVersion yv) {
    yv.setAccountId(accountId);
    yv.setEntityId(entityId);
    yv.setType(type);
    return new RestResponse<>(yamlHistoryService.save(yv));
  }
}
