package software.wings.resources;

import static software.wings.security.PermissionAttribute.ResourceType.APPLICATION;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.swagger.annotations.Api;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Application;
import software.wings.beans.RestResponse;
import software.wings.beans.Service;
import software.wings.exception.WingsException;
import software.wings.security.annotations.AuthRule;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.yaml.AppYaml;
import software.wings.yaml.YamlHelper;
import software.wings.yaml.YamlPayload;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
 * Application Yaml Resource class.
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
  public RestResponse<YamlPayload> get(@PathParam("appId") String appId) {
    List<Service> services = serviceResourceService.findServicesByApp(appId);

    logger.info("***************** services: " + services);

    Application app = appService.get(appId);

    AppYaml appYaml = new AppYaml();
    appYaml.setName(app.getName());
    appYaml.setDescription(app.getDescription());
    appYaml.setServiceNamesFromServices(services);

    return YamlHelper.getYamlRestResponse(appYaml, app.getName() + ".yaml");
  }

  // TODO - NOTE: we probably don't need a PUT and a POST endpoint - there is really only one method - save

  /**
   * Save the changes reflected in appYaml (in a JSON "wrapper")
   *
   * @param appId  the app id
   * @param yamlPayload the yaml version of app
   * @return the rest response
   */
  @POST
  @Path("/{appId}")
  @Timed
  @ExceptionMetered
  public RestResponse<Application> save(@PathParam("appId") String appId, YamlPayload yamlPayload,
      @QueryParam("deleteEnabled") @DefaultValue("false") boolean deleteEnabled) {
    String yaml = yamlPayload.getYaml();
    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

    RestResponse rr = new RestResponse<>();
    rr.setResponseMessages(yamlPayload.getResponseMessages());

    /* TODO
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
  @Path("/{appId}")
  @Timed
  @ExceptionMetered
  public RestResponse<Application> update(@PathParam("appId") String appId, YamlPayload yamlPayload,
      @QueryParam("deleteEnabled") @DefaultValue("false") boolean deleteEnabled) {
    String yaml = yamlPayload.getYaml();
    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

    RestResponse rr = new RestResponse<>();
    rr.setResponseMessages(yamlPayload.getResponseMessages());

    //-------------
    RestResponse beforeResponse = get(appId);
    YamlPayload beforeYP = (YamlPayload) beforeResponse.getResource();
    String beforeYaml = beforeYP.getYaml();

    logger.info("************* BEFORE Yaml = " + beforeYaml);
    //-------------

    logger.info("************* AFTER Yaml = " + yaml);

    // what are the service changes? Which are additions and which are deletions?
    List<String> addedServices = new ArrayList<String>();
    List<String> deletedServices = new ArrayList<String>();

    AppYaml beforeAppYaml = null;

    if (beforeYaml != null && !beforeYaml.isEmpty()) {
      try {
        beforeAppYaml = mapper.readValue(beforeYaml, AppYaml.class);
      } catch (Exception e) {
        // bad before Yaml
        e.printStackTrace();
        YamlHelper.addCouldNotMapBeforeYamlMessage(rr);
      }
    } else {
      // missing before Yaml
      YamlHelper.addMissingBeforeYamlMessage(rr);
    }

    AppYaml appYaml = null;

    if (yaml != null && !yaml.isEmpty()) {
      try {
        appYaml = mapper.readValue(yaml, AppYaml.class);

        List<String> serviceNames = appYaml.getServiceNames();

        // initial the services to add from the after
        for (String s : serviceNames) {
          addedServices.add(s);
        }

        if (beforeAppYaml != null) {
          List<String> beforeServices = beforeAppYaml.getServiceNames();

          // initial the services to delete from the before, and remove the befores from the services to add list
          for (String s : beforeServices) {
            deletedServices.add(s);
            addedServices.remove(s);
          }
        }

        // remove the afters from the services to delete list
        for (String s : serviceNames) {
          deletedServices.remove(s);
        }

        logger.info("************* addedServices = " + addedServices);
        logger.info("************* deletedServices = " + deletedServices);

        Application app = appService.get(appId);

        logger.info("************* app.getName() = " + app.getName());

        // List<Service> services = app.getServices();
        List<Service> services = serviceResourceService.findServicesByApp(appId);

        logger.info("************* services.size() = " + services.size());

        Map<String, Service> serviceMap = new HashMap<String, Service>();

        // populate the map
        for (Service service : services) {
          logger.info("    ------------- adding to serviceMap: |" + service.getName() + "|");

          serviceMap.put(service.getName(), service);
        }

        // do additions
        for (String s : addedServices) {
          logger.info("    ------------- add Service: " + s);

          // create the new Service
          Service newService = new Service();
          newService.setAppId(appId);
          newService.setName(s);
          serviceResourceService.save(newService);

          // add new service to serviceMap
          // serviceMap.put(newService.getName(), newService);
        }

        if (deletedServices.size() > 0 && !deleteEnabled) {
          YamlHelper.addNonEmptyDeletionsWarningMessage(rr);
        } else {
          // do deletions - NOTE: CANNOT delete services with workflows!
          for (String servName : deletedServices) {
            logger.info("    ------------- delete Service: |" + servName + "|");

            if (serviceMap.containsKey(servName)) {
              logger.info("    ------------- HAS IT --------------");
            } else {
              logger.info("    ------------- DOES NOT HAVE IT --------------");
            }

            serviceResourceService.delete(appId, serviceMap.get(servName).getUuid());
            // serviceMap.remove(servName);
          }

          // save the changes
          app.setName(appYaml.getName());
          app.setDescription(appYaml.getDescription());
          // app.setServices(new ArrayList(serviceMap.values()));

          app = appService.update(app);

          // return the new resource
          if (app != null) {
            rr.setResource(app);
          }
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
