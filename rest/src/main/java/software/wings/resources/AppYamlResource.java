package software.wings.resources;

import static software.wings.beans.Setup.SetupStatus.COMPLETE;
import static software.wings.security.PermissionAttribute.ResourceType.APPLICATION;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.snakeyaml.DumperOptions;
import com.fasterxml.jackson.dataformat.yaml.snakeyaml.DumperOptions.FlowStyle;
import com.fasterxml.jackson.dataformat.yaml.snakeyaml.DumperOptions.ScalarStyle;
import com.fasterxml.jackson.dataformat.yaml.snakeyaml.Yaml;
import com.fasterxml.jackson.dataformat.yaml.snakeyaml.nodes.Tag;
import io.swagger.annotations.Api;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Application;
import software.wings.beans.ErrorCode;
import software.wings.beans.ResponseMessage;
import software.wings.beans.ResponseMessage.ResponseTypeEnum;
import software.wings.beans.RestResponse;
import software.wings.beans.Service;
import software.wings.beans.Setup.SetupStatus;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.security.annotations.AuthRule;
import software.wings.security.annotations.ListAPI;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.yaml.AppYaml;
import software.wings.yaml.Config;
import software.wings.yaml.SetupYaml;
import software.wings.yaml.YamlHelper;
import software.wings.yaml.YamlPayload;
import software.wings.yaml.YamlRepresenter;

import java.util.List;
import javax.inject.Inject;
import javax.ws.rs.BeanParam;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

/**
 * Application Resource class.
 *
 * @author bsollish
 */
@Api("/appYaml")
@Path("/appYaml")
@Produces("application/json")
@AuthRule(APPLICATION)
public class AppYamlResource {
  private AppService appService;
  private ServiceResourceService serviceResourceService;

  private final Logger logger = LoggerFactory.getLogger(getClass());

  /**
   * Instantiates a new app yaml resource.
   *
   * @param appService the app service
   * @param serviceResourceService the service (resource) service
   */
  @Inject
  public AppYamlResource(AppService appService, ServiceResourceService serviceResourceService) {
    this.appService = appService;
    this.serviceResourceService = serviceResourceService;
  }

  /**
   * Gets the yaml version of an app by appId
   *
   * @param appId  the app id
   * @return the rest response
   */
  @GET
  @Path("/{appId}")
  @Timed
  @ExceptionMetered
  public RestResponse<YamlPayload> getYaml(@PathParam("appId") String appId) {
    RestResponse rr = new RestResponse<>();

    List<Service> services = serviceResourceService.findServicesByApp(appId);

    logger.info("***************** services: " + services);

    AppYaml appYaml = new AppYaml();
    appYaml.setServices(services);

    Yaml yaml = new Yaml(YamlHelper.getRepresenter(), YamlHelper.getDumperOptions());
    String dumpedYaml = yaml.dump(appYaml);

    // remove first line of Yaml:
    dumpedYaml = dumpedYaml.substring(dumpedYaml.indexOf('\n') + 1);

    YamlPayload yp = new YamlPayload(dumpedYaml);
    Application app = appService.get(appId);
    yp.setName(app.getName() + ".yaml");

    rr.setResponseMessages(yp.getResponseMessages());

    if (yp.getYaml() != null && !yp.getYaml().isEmpty()) {
      rr.setResource(yp);
    }

    return rr;
  }

  /**
   * Save the changes reflected in setupYaml (in a JSON "wrapper")
   *
   * @param appId  the app id
   * @param yamlPayload the yaml version of app
   * @return the rest response
   */
  @POST
  @Path("/{accountId}")
  @Timed
  @ExceptionMetered
  public RestResponse<Application> saveFromYaml(@QueryParam("appId") String appId, YamlPayload yamlPayload) {
    String yaml = yamlPayload.getYaml();
    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

    RestResponse rr = new RestResponse<>();
    rr.setResponseMessages(yamlPayload.getResponseMessages());

    /*
    Application app = null;

    if (yaml != null && !yaml.isEmpty()) {
      try {
        app = mapper.readValue(yaml, Application.class);
        app.setAccountId(accountId);

        app = setupYamlService.save(app);

        if (app != null) {
          rr.setResource(app);
        }
      } catch (Exception e) {
        addUnrecognizedFieldsMessage(rr);
      }
    }
    */

    return rr;
  }

  /**
   * Update an app that is sent as Yaml (in a JSON "wrapper")
   *
   * @param appId  the app id
   * @param yamlPayload the yaml version of app
   * @return the rest response
   */
  @PUT
  @Path("/{accountId}")
  @Timed
  @ExceptionMetered
  public RestResponse<Application> updateFromYaml(@QueryParam("appId") String appId, YamlPayload yamlPayload) {
    String yaml = yamlPayload.getYaml();
    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

    RestResponse rr = new RestResponse<>();
    rr.setResponseMessages(yamlPayload.getResponseMessages());

    /*
    Application app = null;

    if (yaml != null && !yaml.isEmpty()) {
      try {
        app = mapper.readValue(yaml, Application.class);
        app.setUuid(appId);

        app = setupYamlService.update(app);

        if (app != null) {
          rr.setResource(app);
        }
      } catch (Exception e) {
        addUnrecognizedFieldsMessage(rr);
      }
    }
    */

    return rr;
  }
}
