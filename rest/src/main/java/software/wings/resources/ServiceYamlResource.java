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
import software.wings.beans.command.ServiceCommand;
import software.wings.security.annotations.AuthRule;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.yaml.ServiceYaml;
import software.wings.yaml.YamlHelper;
import software.wings.yaml.YamlPayload;

import java.util.ArrayList;
import java.util.List;
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
@Api("/serviceYaml")
@Path("/serviceYaml")
@Produces("application/json")
@AuthRule(APPLICATION)
public class ServiceYamlResource {
  private ServiceResourceService serviceResourceService;

  private final Logger logger = LoggerFactory.getLogger(getClass());

  /**
   * Instantiates a new app yaml resource.
   *
   * @param serviceResourceService the service (resource) service
   */
  @Inject
  public ServiceYamlResource(ServiceResourceService serviceResourceService) {
    this.serviceResourceService = serviceResourceService;
  }

  /**
   * Gets the yaml version of a service by serviceId
   *
   * @param appId  the app id
   * @param serviceId  the service id
   * @return the rest response
   */
  @GET
  @Path("/{appId}/{serviceId}")
  @Timed
  @ExceptionMetered
  public RestResponse<YamlPayload> get(@PathParam("appId") String appId, @PathParam("serviceId") String serviceId) {
    Service service = serviceResourceService.get(appId, serviceId);
    List<ServiceCommand> serviceCommands = service.getServiceCommands();

    ServiceYaml serviceYaml = new ServiceYaml(service);
    serviceYaml.setServiceCommands(serviceCommands);

    return YamlHelper.getYamlRestResponse(serviceYaml, service.getName() + ".yaml");
  }

  // TODO - NOTE: we probably don't need a PUT and a POST endpoint - there is really only one method - save

  /**
   * Save the changes reflected in serviceYaml (in a JSON "wrapper")
   *
   * @param serviceId  the service id
   * @param yamlPayload the yaml version of service
   * @return the rest response
   */
  @POST
  @Path("/{appId}/{serviceId}")
  @Timed
  @ExceptionMetered
  public RestResponse<Application> save(@PathParam("appId") String appId, @PathParam("serviceId") String serviceId,
      YamlPayload yamlPayload, @QueryParam("deleteEnabled") @DefaultValue("false") boolean deleteEnabled) {
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
   * Update a service that is sent as Yaml (in a JSON "wrapper")
   *
   * @param serviceId  the service id
   * @param yamlPayload the yaml version of service
   * @return the rest response
   */
  @PUT
  @Path("/{appId}/{serviceId}")
  @Timed
  @ExceptionMetered
  public RestResponse<Application> update(@PathParam("appId") String appId, @PathParam("serviceId") String serviceId,
      YamlPayload yamlPayload, @QueryParam("deleteEnabled") @DefaultValue("false") boolean deleteEnabled) {
    String yaml = yamlPayload.getYaml();
    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

    RestResponse rr = new RestResponse<>();
    rr.setResponseMessages(yamlPayload.getResponseMessages());

    //-------------
    RestResponse beforeResponse = get(appId, serviceId);
    YamlPayload beforeYP = (YamlPayload) beforeResponse.getResource();
    String beforeYaml = beforeYP.getYaml();

    logger.info("************* BEFORE Yaml = " + beforeYaml);
    //-------------

    logger.info("************* AFTER Yaml = " + yaml);

    // TODO - what are the serviceCommand changes? Which are additions and which are deletions?

    List<String> addedServiceCommands = new ArrayList<String>();
    List<String> deletedServiceCommands = new ArrayList<String>();

    Service beforeService = null;

    if (beforeYaml != null && !beforeYaml.isEmpty()) {
      try {
        beforeService = mapper.readValue(beforeYaml, Service.class);
        beforeService.setUuid(serviceId);
        beforeService.setAppId(appId);
      } catch (Exception e) {
        // bad before Yaml
        e.printStackTrace();
        YamlHelper.addCouldNotMapBeforeYamlMessage(rr);
      }
    } else {
      // missing before Yaml
      YamlHelper.addMissingBeforeYamlMessage(rr);
    }

    Service service = null;

    if (yaml != null && !yaml.isEmpty()) {
      try {
        service = mapper.readValue(yaml, Service.class);
        service.setUuid(serviceId);
        service.setAppId(appId);

        List<ServiceCommand> serviceCommands = service.getServiceCommands();

        // initial the service commands to add from the after
        for (ServiceCommand sc : serviceCommands) {
          addedServiceCommands.add(sc.getName());
        }

        if (beforeService != null) {
          List<ServiceCommand> beforeServiceCommands = beforeService.getServiceCommands();

          // initial the service commands to delete from the before, and remove the befores from the service commands to
          // add list
          for (ServiceCommand sc : beforeServiceCommands) {
            deletedServiceCommands.add(sc.getName());
            addedServiceCommands.remove(sc.getName());
          }
        }

        // remove the afters from the service commands to delete list
        for (ServiceCommand sc : serviceCommands) {
          deletedServiceCommands.remove(sc.getName());
        }

        if (deletedServiceCommands.size() > 0 && !deleteEnabled) {
          YamlHelper.addNonEmptyDeletionsWarningMessage(rr);
        } else {
          // TODO - do the service command creations and deletions

          logger.info("************* addedServiceCommands = " + addedServiceCommands);
          logger.info("************* deletedServiceCommands = " + deletedServiceCommands);

          /*
          // save the changes
          service = serviceResourceService.update(service);

          // return the new resource
          if (service != null) {
            rr.setResource(service);
          }
          */
        }

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
