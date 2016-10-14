/**
 *
 */

package software.wings.resources;

import static software.wings.beans.CatalogNames.BASTION_HOST_ATTRIBUTES;
import static software.wings.beans.CatalogNames.CONNECTION_ATTRIBUTES;

import io.swagger.annotations.Api;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.CatalogNames;
import software.wings.beans.Environment.EnvironmentType;
import software.wings.beans.ExecutionCredential.ExecutionType;
import software.wings.beans.RestResponse;
import software.wings.beans.SettingValue.SettingVariableTypes;
import software.wings.service.intfc.CatalogService;
import software.wings.service.intfc.SettingsService;

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
 * The Class CatalogResource.
 *
 * @author Rishi.
 */
@Api("catalogs")
@Path("/catalogs")
@Produces("application/json")
public class CatalogResource {
  /**
   * The constant APP_ID.
   */
  public static final String APP_ID = "appId";
  /**
   * The constant SERVICE_ID.
   */
  public static final String SERVICE_ID = "serviceId";
  /**
   * The constant JENKINS_SETTING_ID.
   */
  public static final String JENKINS_SETTING_ID = "jenkinsSettingId";

  private final Logger logger = LoggerFactory.getLogger(getClass());

  private CatalogService catalogService;
  private SettingsService settingsService;

  /**
   * Creates a new catalog resource.
   *
   * @param catalogService  catalogService object.
   * @param settingsService SettingService object
   */
  @Inject
  public CatalogResource(CatalogService catalogService, SettingsService settingsService) {
    this.catalogService = catalogService;
    this.settingsService = settingsService;
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
    Map<String, Object> catalogs = getCatalogs(catalogTypes, uriInfo);
    return new RestResponse<>(catalogs);
  }

  private Map<String, Object> getCatalogs(List<String> catalogTypes, UriInfo uriInfo) throws IOException {
    Map<String, Object> catalogs = new HashMap<>();

    if (catalogTypes == null || catalogTypes.size() == 0) {
      catalogs.put(CatalogNames.EXECUTION_TYPE, ExecutionType.values());
      catalogs.put(CatalogNames.ENVIRONMENT_TYPE, EnvironmentType.values());
      catalogs.putAll(catalogService.getCatalogs());
    } else {
      for (String catalogType : catalogTypes) {
        switch (catalogType) {
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
          case CatalogNames.EXECUTION_TYPE: {
            catalogs.put(catalogType, ExecutionType.values());
            ;
            break;
          }
          case CatalogNames.ENVIRONMENT_TYPE: {
            catalogs.put(catalogType, EnvironmentType.values());
          }
          default: { catalogs.put(catalogType, catalogService.getCatalogItems(catalogType)); }
        }
      }
    }
    return catalogs;
  }
}
