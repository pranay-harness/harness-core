/**
 *
 */

package software.wings.resources;

import software.wings.beans.CatalogNames;
import software.wings.beans.RestResponse;
import software.wings.service.intfc.CatalogService;
import software.wings.service.intfc.WorkflowService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

/**
 * @author Rishi
 */
@Path("/catalogs")
@Produces("application/json")
public class CatalogResource {
  private WorkflowService workflowService;
  private CatalogService catalogService;

  @Inject
  public CatalogResource(CatalogService catalogService, WorkflowService workflowService) {
    this.catalogService = catalogService;
    this.workflowService = workflowService;
  }

  @GET
  public RestResponse<Map<String, Object>> list(@QueryParam("catalogType") List<String> catalogTypes) {
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
          default: { catalogs.put(catalogType, catalogService.getCatalogItems(catalogType)); }
        }
      }
    }
    return new RestResponse<>(catalogs);
  }
}
