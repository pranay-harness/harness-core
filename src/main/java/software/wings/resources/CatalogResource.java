/**
 *
 */

package software.wings.resources;

import software.wings.beans.CatalogNames;
import software.wings.beans.RestResponse;
import software.wings.service.intfc.CatalogService;
import software.wings.service.intfc.JenkinsBuildService;
import software.wings.service.intfc.WorkflowService;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

/**
 * @author Rishi.
 */
@Path("/catalogs")
@Produces("application/json")
public class CatalogResource {
  private WorkflowService workflowService;
  private CatalogService catalogService;
  private JenkinsBuildService jenkinsBuildService;

  /**
   * Creates a new calalog resource.
   * @param catalogService catalogService object.
   * @param workflowService workflowService object.
   * @param jenkinsBuildService JenkinsBuildService object.
   */
  @Inject
  public CatalogResource(
      CatalogService catalogService, WorkflowService workflowService, JenkinsBuildService jenkinsBuildService) {
    this.catalogService = catalogService;
    this.workflowService = workflowService;
    this.jenkinsBuildService = jenkinsBuildService;
  }

  /**
   * returns catalog items.
   * @param catalogTypes types of catalog items.
   * @param uriInfo uriInfo from jersey.
   * @return RestReponse containing map of catalog objects.
   * @throws IOException exception.
   */
  @GET
  public RestResponse<Map<String, Object>> list(
      @QueryParam("catalogType") List<String> catalogTypes, @Context UriInfo uriInfo) throws IOException {
    Map<String, Object> catalogs = new HashMap<>();

    if (catalogTypes == null || catalogTypes.size() == 0) {
      catalogs.put(CatalogNames.ORCHESTRATION_STENCILS, workflowService.stencils());
      catalogs.putAll(catalogService.getCatalogs());
    } else {
      for (String catalogType : catalogTypes) {
        switch (catalogType) {
          case CatalogNames.ORCHESTRATION_STENCILS: {
            catalogs.put(catalogType, workflowService.stencils());
            break;
          }
          case CatalogNames.JENKINS_BUILD: {
            catalogs.put(catalogType, jenkinsBuildService.getBuilds(uriInfo.getQueryParameters()));
            break;
          }
          default: { catalogs.put(catalogType, catalogService.getCatalogItems(catalogType)); }
        }
      }
    }
    return new RestResponse<>(catalogs);
  }
}
