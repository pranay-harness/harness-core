/**
 *
 */

package software.wings.resources;

import static software.wings.beans.CatalogNames.BASTION_HOST_ATTRIBUTES;
import static software.wings.beans.CatalogNames.CONNECTION_ATTRIBUTES;

import com.google.common.collect.Lists;

import io.swagger.annotations.Api;
import software.wings.beans.CatalogNames;
import software.wings.beans.CommandUnitType;
import software.wings.beans.JenkinsConfig;
import software.wings.beans.RestResponse;
import software.wings.beans.SettingValue.SettingVariableTypes;
import software.wings.service.intfc.CatalogService;
import software.wings.service.intfc.JenkinsBuildService;
import software.wings.service.intfc.ServiceResourceService;
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
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;

/**
 * @author Rishi.
 */
@Api("catalogs")
@Path("/catalogs")
@Produces("application/json")
public class CatalogResource {
  public static final String APP_ID = "appId";
  public static final String SERVICE_ID = "serviceId";
  public static final String JENKINS_SETTING_ID = "jenkinsSettingId";
  private WorkflowService workflowService;
  private CatalogService catalogService;
  private JenkinsBuildService jenkinsBuildService;
  private SettingsService settingsService;
  private ServiceResourceService serviceResourceService;

  /**
   * Creates a new calalog resource.
   *
   * @param catalogService      catalogService object.
   * @param workflowService     workflowService object.
   * @param jenkinsBuildService JenkinsBuildService object.
   * @param settingsService     SettingService object
   */
  @Inject
  public CatalogResource(CatalogService catalogService, WorkflowService workflowService,
      JenkinsBuildService jenkinsBuildService, SettingsService settingsService,
      ServiceResourceService serviceResourceService) {
    this.catalogService = catalogService;
    this.workflowService = workflowService;
    this.jenkinsBuildService = jenkinsBuildService;
    this.settingsService = settingsService;
    this.serviceResourceService = serviceResourceService;
  }

  /**
   * returns catalog items.
   *
   * @param catalogTypes types of catalog items.
   * @param uriInfo      uriInfo from jersey.
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
      catalogs.put(CatalogNames.COMMAND_STENCILS, getCommandStencils(uriInfo.getQueryParameters()));
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
            catalogs.put(catalogType, getCommandStencils(uriInfo.getQueryParameters()));
            break;
          }
          case CatalogNames.JENKINS_CONFIG: {
            catalogs.put(catalogType,
                settingsService.getSettingAttributesByType(
                    uriInfo.getQueryParameters().getFirst(APP_ID), SettingVariableTypes.JENKINS));
            break;
          }
          case CatalogNames.JENKINS_BUILD: {
            catalogs.put(catalogType,
                jenkinsBuildService.getBuilds(uriInfo.getQueryParameters(),
                    (JenkinsConfig) settingsService.get(uriInfo.getQueryParameters().getFirst(JENKINS_SETTING_ID))
                        .getValue()));
            break;
          }
          case CONNECTION_ATTRIBUTES: {
            catalogs.put(CONNECTION_ATTRIBUTES,
                settingsService.getSettingAttributesByType(
                    uriInfo.getQueryParameters().getFirst(APP_ID), SettingVariableTypes.HOST_CONNECTION_ATTRIBUTES));
            break;
          }
          case BASTION_HOST_ATTRIBUTES: {
            catalogs.put(BASTION_HOST_ATTRIBUTES,
                settingsService.getSettingAttributesByType(uriInfo.getQueryParameters().getFirst(APP_ID),
                    SettingVariableTypes.BASTION_HOST_CONNECTION_ATTRIBUTES));
            break;
          }
          default: { catalogs.put(catalogType, catalogService.getCatalogItems(catalogType)); }
        }
      }
    }
    return new RestResponse<>(catalogs);
  }

  private List<Object> getCommandStencils(MultivaluedMap<String, String> queryParameters) {
    List<Object> stencils = Lists.newArrayList(CommandUnitType.getStencils());
    if (queryParameters.containsKey(SERVICE_ID) && queryParameters.containsKey(APP_ID)) {
      stencils.addAll(serviceResourceService.getCommandStencils(
          queryParameters.getFirst(APP_ID), queryParameters.getFirst(SERVICE_ID)));
    }
    return stencils;
  }
}
