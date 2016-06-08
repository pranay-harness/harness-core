/**
 *
 */

package software.wings.resources;

import static java.util.Arrays.stream;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static software.wings.beans.CatalogNames.BASTION_HOST_ATTRIBUTES;
import static software.wings.beans.CatalogNames.CONNECTION_ATTRIBUTES;

import com.google.common.collect.Lists;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.annotations.Api;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.CatalogNames;
import software.wings.beans.CommandUnitType;
import software.wings.beans.JenkinsConfig;
import software.wings.beans.RestResponse;
import software.wings.beans.SettingValue.SettingVariableTypes;
import software.wings.service.intfc.CatalogService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.JenkinsBuildService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.WorkflowService;
import software.wings.sm.EnumData;
import software.wings.sm.OverridingStateTypeDescriptor;
import software.wings.sm.StateTypeDescriptor;
import software.wings.sm.StateTypeScope;
import software.wings.utils.JsonUtils;

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

// TODO: Auto-generated Javadoc

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

  private WorkflowService workflowService;
  private CatalogService catalogService;
  private JenkinsBuildService jenkinsBuildService;
  private SettingsService settingsService;
  private ServiceResourceService serviceResourceService;
  private EnvironmentService environmentService;

  /**
   * Creates a new catalog resource.
   *
   * @param catalogService         catalogService object.
   * @param workflowService        workflowService object.
   * @param jenkinsBuildService    JenkinsBuildService object.
   * @param settingsService        SettingService object
   * @param serviceResourceService the service resource service
   * @param environmentService     the environment service
   */
  @Inject
  public CatalogResource(CatalogService catalogService, WorkflowService workflowService,
      JenkinsBuildService jenkinsBuildService, SettingsService settingsService,
      ServiceResourceService serviceResourceService, EnvironmentService environmentService) {
    this.catalogService = catalogService;
    this.workflowService = workflowService;
    this.jenkinsBuildService = jenkinsBuildService;
    this.settingsService = settingsService;
    this.serviceResourceService = serviceResourceService;
    this.environmentService = environmentService;
    this.environmentService = environmentService;
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
      Map<StateTypeScope, List<StateTypeDescriptor>> stencils = workflowService.stencils();
      for (StateTypeScope stencil : stencils.keySet()) {
        catalogs.put(stencil.name(), postProcess(stencils.get(stencil), uriInfo));
      }
      catalogs.put(CatalogNames.COMMAND_STENCILS, getCommandStencils(uriInfo.getQueryParameters()));
      catalogs.putAll(catalogService.getCatalogs());
    } else {
      for (String catalogType : catalogTypes) {
        switch (catalogType) {
          case CatalogNames.PIPELINE_STENCILS: {
            StateTypeScope scope = StateTypeScope.valueOf(catalogType);
            catalogs.put(catalogType, postProcess(workflowService.stencils(scope).get(scope), uriInfo));
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
          case CatalogNames.ENVIRONMENTS: {
            catalogs.put(catalogType, environmentService.listForEnum(uriInfo.getQueryParameters().getFirst(APP_ID)));
            break;
          }
          default: { catalogs.put(catalogType, catalogService.getCatalogItems(catalogType)); }
        }
      }
    }
    return catalogs;
  }

  private Object postProcess(List<StateTypeDescriptor> stateTypeDescriptors, UriInfo uriInfo) {
    return stateTypeDescriptors.stream()
        .map(stateTypeDescriptor -> processStateTypeDescriptor(stateTypeDescriptor, uriInfo))
        .collect(toList());
  }

  private StateTypeDescriptor processStateTypeDescriptor(StateTypeDescriptor stateTypeDescriptor, UriInfo uriInfo) {
    OverridingStateTypeDescriptor result = new OverridingStateTypeDescriptor(stateTypeDescriptor);
    JsonNode jsonSchema = ((JsonNode) stateTypeDescriptor.getJsonSchema());

    stream(stateTypeDescriptor.getStateClass().getDeclaredFields())
        .filter(field -> field.getAnnotation(EnumData.class) != null)
        .forEach(field -> {
          String catalogName = field.getAnnotation(EnumData.class).catalog();
          try {
            Map<String, Object> catalogs = getCatalogs(singletonList(catalogName), uriInfo);
            Map<String, Object> catalogData = (Map<String, Object>) catalogs.get(catalogName);
            if (catalogData != null) {
              ObjectNode jsonSchemaField = ((ObjectNode) jsonSchema.get("properties").get(field.getName()));
              jsonSchemaField.set("enum", JsonUtils.asTree(catalogData.keySet()));
              jsonSchemaField.set("enumNames", JsonUtils.asTree(catalogData.values()));
            }
          } catch (Exception e) {
            logger.warn("Unable to fill in values for stateType {}:field {} with catalog {}", stateTypeDescriptor,
                field.getName(), catalogName);
          }
        });

    result.setOverridingJsonSchema(jsonSchema);
    return result;
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
