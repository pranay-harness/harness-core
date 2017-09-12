package software.wings.resources.yaml;

import static software.wings.security.PermissionAttribute.ResourceType.APPLICATION;
import static software.wings.yaml.YamlVersion.Builder.aYamlVersion;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.swagger.annotations.Api;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.Environment.EnvironmentType;
import software.wings.beans.ErrorCode;
import software.wings.beans.ResponseMessage.ResponseTypeEnum;
import software.wings.beans.RestResponse;
import software.wings.exception.WingsException;
import software.wings.security.annotations.AuthRule;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.yaml.YamlHistoryService;
import software.wings.yaml.EnvironmentYaml;
import software.wings.yaml.YamlHelper;
import software.wings.yaml.YamlPayload;
import software.wings.yaml.YamlVersion;
import software.wings.yaml.YamlVersion.Type;

import javax.inject.Inject;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

/**
 * Service Resource class.
 *
 * @author bsollish
 */
@Api("/envYaml")
@Path("/envYaml")
@Produces("application/json")
@AuthRule(APPLICATION)
public class EnvironmentYamlResource {
  private AppService appService;
  private EnvironmentService environmentService;
  private YamlHistoryService yamlHistoryService;

  private final Logger logger = LoggerFactory.getLogger(getClass());

  /**
   * Instantiates new resources
   *
   * @param appService             the app service
   * @param yamlHistoryService     the yaml history service
   */
  @Inject
  public EnvironmentYamlResource(
      AppService appService, EnvironmentService environmentService, YamlHistoryService yamlHistoryService) {
    this.appService = appService;
    this.environmentService = environmentService;
    this.yamlHistoryService = yamlHistoryService;
  }

  /**
   * Gets the yaml version of an environment by envId
   *
   * @param appId   the app id
   * @param envId   the environment id
   * @return the rest response
   */
  @GET
  @Path("/{accountId}/{appId}/{envId}")
  @Timed
  @ExceptionMetered
  public RestResponse<YamlPayload> get(@PathParam("appId") String appId, @PathParam("envId") String envId) {
    Environment environment = environmentService.get(appId, envId, true);
    EnvironmentYaml environmentYaml = new EnvironmentYaml(environment);

    return YamlHelper.getYamlRestResponse(environmentYaml, environment.getName() + ".yaml");
  }

  // TODO - NOTE: we probably don't need PUT and POST endpoints - there is really only one method - update (PUT)

  /**
   * Save the changes reflected in environmentYaml (in a JSON "wrapper")
   *
   * @param envId  the environment id
   * @param yamlPayload the yaml version of the environment
   * @return the rest response
   */
  @POST
  @Path("/{accountId}/{appId}/{envId}")
  @Timed
  @ExceptionMetered
  public RestResponse<Application> save(@PathParam("appId") String appId, @PathParam("envId") String envId,
      YamlPayload yamlPayload, @QueryParam("deleteEnabled") @DefaultValue("false") boolean deleteEnabled) {
    String yaml = yamlPayload.getYaml();
    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

    RestResponse rr = new RestResponse<>();
    rr.setResponseMessages(yamlPayload.getResponseMessages());

    // DOES NOTHING

    return rr;
  }

  /**
   * Update a environment that is sent as Yaml (in a JSON "wrapper")
   *
   * @param envId  the environment id
   * @param yamlPayload the yaml version of environment
   * @return the rest response
   */
  @PUT
  @Path("/{accountId}/{appId}/{envId}")
  @Timed
  @ExceptionMetered
  public RestResponse<Environment> update(@PathParam("appId") String appId, @PathParam("envId") String envId,
      YamlPayload yamlPayload, @QueryParam("deleteEnabled") @DefaultValue("false") boolean deleteEnabled) {
    String yaml = yamlPayload.getYaml();
    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

    RestResponse rr = new RestResponse<>();
    rr.setResponseMessages(yamlPayload.getResponseMessages());

    // get the before Yaml
    RestResponse beforeResponse = get(appId, envId);
    YamlPayload beforeYP = (YamlPayload) beforeResponse.getResource();
    String beforeYaml = beforeYP.getYaml();

    if (yaml.equals(beforeYaml)) {
      // no change
      YamlHelper.addResponseMessage(rr, ErrorCode.GENERAL_YAML_INFO, ResponseTypeEnum.INFO, "No change to the Yaml.");
      return rr;
    }

    EnvironmentYaml beforeEnvironmentYaml = null;

    if (beforeYaml != null && !beforeYaml.isEmpty()) {
      try {
        beforeEnvironmentYaml = mapper.readValue(beforeYaml, EnvironmentYaml.class);
      } catch (WingsException e) {
        throw e;
      } catch (Exception e) {
        // bad before Yaml
        e.printStackTrace();
        YamlHelper.addCouldNotMapBeforeYamlMessage(rr);
        return rr;
      }
    } else {
      // missing before Yaml
      YamlHelper.addMissingBeforeYamlMessage(rr);
      return rr;
    }

    EnvironmentYaml environmentYaml = null;

    if (yaml != null && !yaml.isEmpty()) {
      try {
        Environment environment = environmentService.get(appId, envId, false);

        environmentYaml = mapper.readValue(yaml, EnvironmentYaml.class);

        // save the changes
        environment.setName(environmentYaml.getName());
        environment.setDescription(environmentYaml.getDescription());
        String environmentTypeStr = environmentYaml.getEnvironmentType().toUpperCase();

        try {
          EnvironmentType et = EnvironmentType.valueOf(environmentTypeStr);
          environment.setEnvironmentType(et);
        } catch (Exception e) {
          e.printStackTrace();
          YamlHelper.addResponseMessage(rr, ErrorCode.GENERAL_YAML_ERROR, ResponseTypeEnum.ERROR,
              "The EnvironmentType: '" + environmentTypeStr + "' is not found in the EnvironmentType Enum!");
          return rr;
        }

        environment = environmentService.update(environment);

        // return the new resource
        if (environment != null) {
          // save the before yaml version
          String accountId = appService.get(appId).getAccountId();
          YamlVersion beforeYamLVersion = aYamlVersion()
                                              .withAccountId(accountId)
                                              .withEntityId(accountId)
                                              .withType(Type.ENVIRONMENT)
                                              .withYaml(beforeYaml)
                                              .build();
          yamlHistoryService.save(beforeYamLVersion);

          rr.setResource(environment);
        }

      } catch (WingsException e) {
        throw e;
      } catch (Exception e) {
        e.printStackTrace();
        YamlHelper.addUnrecognizedFieldsMessage(rr);
      }
    } else {
      // missing Yaml
      YamlHelper.addMissingYamlMessage(rr);
    }

    return rr;
  }
}