/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.resources;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import static software.wings.beans.CatalogNames.BASTION_HOST_ATTRIBUTES;
import static software.wings.beans.CatalogNames.CONNECTION_ATTRIBUTES;

import io.harness.beans.EnvironmentType;
import io.harness.rest.RestResponse;
import io.harness.security.annotations.PublicApi;

import software.wings.beans.CatalogNames;
import software.wings.beans.ExecutionCredential.ExecutionType;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.Scope;
import software.wings.service.intfc.CatalogService;
import software.wings.service.intfc.SettingsService;
import software.wings.settings.SettingVariableTypes;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
   * @return RestReponse containing map of catalog objects.
   * @throws IOException exception.
   */
  @GET
  @Timed
  @ExceptionMetered
  @PublicApi
  public RestResponse<Map<String, Object>> list(@QueryParam("catalogType") List<String> catalogTypes)
      throws IOException {
    Map<String, Object> catalogs = getCatalogs(catalogTypes, null);
    return new RestResponse<>(catalogs);
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
  @Path("app-catalogs")
  @Timed
  @ExceptionMetered
  @Scope(ResourceType.APPLICATION)
  public RestResponse<Map<String, Object>> listForApp(
      @QueryParam("catalogType") List<String> catalogTypes, @Context UriInfo uriInfo) throws IOException {
    Map<String, Object> catalogs = getCatalogs(catalogTypes, uriInfo.getQueryParameters().getFirst(APP_ID));
    return new RestResponse<>(catalogs);
  }

  private Map<String, Object> getCatalogs(List<String> catalogTypes, String appId) throws IOException {
    Map<String, Object> catalogs = new HashMap<>();

    if (isEmpty(catalogTypes)) {
      catalogs.put(CatalogNames.EXECUTION_TYPE, ExecutionType.values());
      catalogs.put(CatalogNames.ENVIRONMENT_TYPE, EnvironmentType.values());
      catalogs.putAll(catalogService.getCatalogs());
    } else {
      for (String catalogType : catalogTypes) {
        switch (catalogType) {
          case CONNECTION_ATTRIBUTES: {
            if (appId != null) {
              catalogs.put(CONNECTION_ATTRIBUTES,
                  settingsService.getSettingAttributesByType(
                      appId, SettingVariableTypes.HOST_CONNECTION_ATTRIBUTES.name()));
            }
            break;
          }
          case BASTION_HOST_ATTRIBUTES: {
            if (appId != null) {
              catalogs.put(BASTION_HOST_ATTRIBUTES,
                  settingsService.getSettingAttributesByType(
                      appId, SettingVariableTypes.BASTION_HOST_CONNECTION_ATTRIBUTES.name()));
            }
            break;
          }
          case CatalogNames.EXECUTION_TYPE: {
            catalogs.put(catalogType, ExecutionType.values());
            break;
          }
          case CatalogNames.ENVIRONMENT_TYPE: {
            catalogs.put(catalogType, EnvironmentType.values());
            break;
          }
          default: {
            catalogs.put(catalogType, catalogService.getCatalogItems(catalogType));
          }
        }
      }
    }
    return catalogs;
  }
}
