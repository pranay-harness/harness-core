/**
 *
 */

package software.wings.resources;

import software.wings.beans.RestResponse;
import software.wings.common.Constants;
import software.wings.service.intfc.WorkflowService;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

/**
 * @author Rishi
 */
@Path("/catalogs")
@Produces("application/json")
public class CatalogResource {
  private WorkflowService workflowService;

  @Inject
  public CatalogResource(WorkflowService workflowService) {
    this.workflowService = workflowService;
  }

  @GET
  public RestResponse<Map<String, Object>> list() {
    Map<String, Object> catalogs = new HashMap<>();
    catalogs.put(Constants.CATALOG_STENCILS, workflowService.stencils());
    return new RestResponse<>(catalogs);
  }
}
