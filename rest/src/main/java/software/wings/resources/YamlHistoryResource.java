package software.wings.resources;

import static software.wings.security.PermissionAttribute.ResourceType.APPLICATION;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.RestResponse;
import software.wings.security.annotations.AuthRule;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.yaml.YamlHistory;
import software.wings.yaml.YamlType;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

/**
 * Configuration as Code Resource class.
 *
 * @author bsollish
 */
@Api("/yamlHistory")
@Path("/yamlHistory")
@Produces("application/json")
@AuthRule(APPLICATION)
public class YamlHistoryResource {
  // private AppService appService;
  // private ServiceResourceService serviceResourceService;

  private final Logger logger = LoggerFactory.getLogger(getClass());

  /**
   * Instantiates a new app yaml resource.
   *
   * @param appService             the app service
   * @param serviceResourceService the service (resource) service
   */
  @Inject
  public YamlHistoryResource() {}

  /**
   * Gets the Yaml history by yamlType and entityId
   *
   * @param accountId
   * @param yamlType
   * @param entityId
   * @return the rest response
   */
  @GET
  @Path("/{accountId}/{yamlType}/{entityId}")
  @Timed
  @ExceptionMetered
  public RestResponse<YamlHistory> get(@PathParam("accountId") String accountId,
      @PathParam("yamlType") YamlType yamlType, @PathParam("entityId") String entityId) {
    RestResponse rr = new RestResponse<>();

    /*
    List<Application> apps = appService.getAppsByAccountId(accountId);

    //logger.info("***************** apps: " + apps);

    // example of getting a sample object hierarchy for testing/debugging:
    //rr.setResource(YamlHelper.sampleConfigAsCodeDirectory());

    FolderNode configFolder = new FolderNode("Setup", Setup.class);
    configFolder.addChild(new YamlNode("setup.yaml", SetupYaml.class));
    FolderNode applicationsFolder = new FolderNode("Applications", Application.class);
    configFolder.addChild(applicationsFolder);

    // iterate over applications
    for (Application app : apps) {
      FolderNode appFolder = new FolderNode(app.getName(), Application.class);
      applicationsFolder.addChild(appFolder);
      appFolder.addChild(new YamlNode(app.getUuid(),app.getName() + ".yaml", AppYaml.class));
      FolderNode servicesFolder = new FolderNode("Services", Service.class);
      appFolder.addChild(servicesFolder);

      List<Service> services = serviceResourceService.findServicesByApp(app.getAppId());

      //logger.info("***************** services: " + services);

      // iterate over services
      for (Service service : services) {
        FolderNode serviceFolder = new FolderNode(service.getName(), Service.class);
        servicesFolder.addChild(serviceFolder);
        serviceFolder.addChild(new ServiceYamlNode(service.getUuid(), service.getAppId(),service.getName() + ".yaml",
    ServiceYaml.class)); FolderNode serviceCommandsFolder = new FolderNode("Commands", ServiceCommand.class);
        serviceFolder.addChild(serviceCommandsFolder);

        List<ServiceCommand> serviceCommands = service.getServiceCommands();

        //logger.info("***************** serviceCommands: " + serviceCommands);

        // iterate over service commands
        for (ServiceCommand serviceCommand : serviceCommands) {
          serviceCommandsFolder.addChild(new ServiceCommandYamlNode(serviceCommand.getUuid(), serviceCommand.getAppId(),
    serviceCommand.getServiceId(),serviceCommand.getName() + ".yaml", ServiceCommand.class));
        }
      }
    }

    rr.setResource(configFolder);
    */

    rr.setResource(new YamlHistory());

    return rr;
  }
}
