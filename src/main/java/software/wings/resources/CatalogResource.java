/**
 *
 */

package software.wings.resources;

import static software.wings.beans.CatalogNames.BASTION_HOST_ATTRIBUTES;
import static software.wings.beans.CatalogNames.CONNECTION_ATTRIBUTES;

import software.wings.beans.CatalogNames;
import software.wings.beans.RestResponse;
import software.wings.beans.SettingValue.SettingVariableTypes;
import software.wings.service.intfc.CatalogService;
import software.wings.service.intfc.JenkinsBuildService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.WorkflowService;
import software.wings.sm.StateTypeDescriptor;
import software.wings.sm.StateTypeScope;

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
  private SettingsService settingsService;

  /**
   * Creates a new calalog resource.
   *
   * @param catalogService catalogService object.
   * @param workflowService workflowService object.
   * @param jenkinsBuildService JenkinsBuildService object.
   * @param settingsService SettingService object
   */
  @Inject
  public CatalogResource(CatalogService catalogService, WorkflowService workflowService,
      JenkinsBuildService jenkinsBuildService, SettingsService settingsService) {
    this.catalogService = catalogService;
    this.workflowService = workflowService;
    this.jenkinsBuildService = jenkinsBuildService;
    this.settingsService = settingsService;
  }

  /**
   * returns catalog items.
   *
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
      Map<StateTypeScope, List<StateTypeDescriptor>> stencils = workflowService.stencils();
      for (StateTypeScope stencil : stencils.keySet()) {
        catalogs.put(stencil.name(), stencils.get(stencil));
      }
      catalogs.putAll(catalogService.getCatalogs());
    } else {
      for (String catalogType : catalogTypes) {
        switch (catalogType) {
          case CatalogNames.PIPELINE_STENCILS: {
            StateTypeScope scope = StateTypeScope.valueOf(catalogType);
            catalogs.put(catalogType, workflowService.stencils(scope).get(scope));
            break;
          }
          case CatalogNames.ORCHESTRATION_STENCILS: {
            StateTypeScope scope = StateTypeScope.valueOf(catalogType);
            catalogs.put(catalogType, workflowService.stencils(scope).get(scope));
            break;
          }
          case CatalogNames.COMMAND_STENCILS: {
            StateTypeScope scope = StateTypeScope.valueOf(catalogType);
            catalogs.put(catalogType, workflowService.stencils(scope).get(scope));
            break;
          }
          case CatalogNames.JENKINS_BUILD: {
            catalogs.put(catalogType, jenkinsBuildService.getBuilds(uriInfo.getQueryParameters()));
            break;
          }
          case CONNECTION_ATTRIBUTES: {
            catalogs.put(CONNECTION_ATTRIBUTES,
                settingsService.getSettingAttributesByType(
                    uriInfo.getQueryParameters().getFirst("appId"), SettingVariableTypes.HOST_CONNECTION_ATTRIBUTES));
            break;
          }
          case BASTION_HOST_ATTRIBUTES: {
            catalogs.put(BASTION_HOST_ATTRIBUTES,
                settingsService.getSettingAttributesByType(uriInfo.getQueryParameters().getFirst("appId"),
                    SettingVariableTypes.BASTION_HOST_CONNECTION_ATTRIBUTES));
            break;
          }
          default: { catalogs.put(catalogType, catalogService.getCatalogItems(catalogType)); }
        }
      }
    }
    return new RestResponse<>(catalogs);
  }
}
