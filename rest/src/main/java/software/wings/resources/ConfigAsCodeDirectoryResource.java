package software.wings.resources;

import static software.wings.security.PermissionAttribute.ResourceType.APPLICATION;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.RestResponse;
import software.wings.beans.Service;
import software.wings.beans.Setup;
import software.wings.beans.command.ServiceCommand;
import software.wings.security.annotations.AuthRule;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.yaml.AppYaml;
import software.wings.yaml.EnvironmentYaml;
import software.wings.yaml.ServiceYaml;
import software.wings.yaml.SetupYaml;
import software.wings.yaml.directory.DirectoryNode;
import software.wings.yaml.directory.EnvironmentYamlNode;
import software.wings.yaml.directory.FolderNode;
import software.wings.yaml.directory.ServiceCommandYamlNode;
import software.wings.yaml.directory.ServiceYamlNode;
import software.wings.yaml.directory.YamlNode;

import java.util.List;
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
@Api("/configAsCode")
@Path("/configAsCode")
@Produces("application/json")
@AuthRule(APPLICATION)
public class ConfigAsCodeDirectoryResource {
  private AppService appService;
  private ServiceResourceService serviceResourceService;
  private EnvironmentService environmentService;

  private final Logger logger = LoggerFactory.getLogger(getClass());

  /**
   * Instantiates a new app yaml resource.
   *
   * @param appService             the app service
   * @param serviceResourceService the service (resource) service
   */
  @Inject
  public ConfigAsCodeDirectoryResource(
      AppService appService, ServiceResourceService serviceResourceService, EnvironmentService environmentService) {
    this.appService = appService;
    this.serviceResourceService = serviceResourceService;
    this.environmentService = environmentService;
  }

  /**
   * Gets the config as code directory by accountId
   *
   * @param accountId the account id
   * @return the rest response
   */
  @GET
  @Path("/{accountId}")
  @Timed
  @ExceptionMetered
  public RestResponse<DirectoryNode> get(@PathParam("accountId") String accountId) {
    RestResponse rr = new RestResponse<>();

    List<Application> apps = appService.getAppsByAccountId(accountId);

    // logger.info("***************** apps: " + apps);

    // example of getting a sample object hierarchy for testing/debugging:
    // rr.setResource(YamlHelper.sampleConfigAsCodeDirectory());

    FolderNode configFolder = new FolderNode("Setup", Setup.class);
    configFolder.addChild(new YamlNode("setup.yaml", SetupYaml.class));
    FolderNode applicationsFolder = new FolderNode("Applications", Application.class);
    configFolder.addChild(applicationsFolder);

    // iterate over applications
    for (Application app : apps) {
      FolderNode appFolder = new FolderNode(app.getName(), Application.class);
      applicationsFolder.addChild(appFolder);
      appFolder.addChild(new YamlNode(app.getUuid(), app.getName() + ".yaml", AppYaml.class));

      // ------------------- SERVICES SECTION -----------------------
      FolderNode servicesFolder = new FolderNode("Services", Service.class);
      appFolder.addChild(servicesFolder);

      List<Service> services = serviceResourceService.findServicesByApp(app.getAppId());

      // iterate over services
      for (Service service : services) {
        FolderNode serviceFolder = new FolderNode(service.getName(), Service.class);
        servicesFolder.addChild(serviceFolder);
        serviceFolder.addChild(
            new ServiceYamlNode(service.getUuid(), service.getAppId(), service.getName() + ".yaml", ServiceYaml.class));
        FolderNode serviceCommandsFolder = new FolderNode("Commands", ServiceCommand.class);
        serviceFolder.addChild(serviceCommandsFolder);

        List<ServiceCommand> serviceCommands = service.getServiceCommands();

        // logger.info("***************** serviceCommands: " + serviceCommands);

        // iterate over service commands
        for (ServiceCommand serviceCommand : serviceCommands) {
          serviceCommandsFolder.addChild(new ServiceCommandYamlNode(serviceCommand.getUuid(), serviceCommand.getAppId(),
              serviceCommand.getServiceId(), serviceCommand.getName() + ".yaml", ServiceCommand.class));
        }
      }
      // ----------------- END SERVICES SECTION ---------------------

      // ------------------- ENVIRONMENTS SECTION -----------------------
      FolderNode environmentsFolder = new FolderNode("Environments", Environment.class);
      appFolder.addChild(environmentsFolder);

      List<Environment> environments = environmentService.getEnvByApp(app.getAppId());

      // iterate over environments
      for (Environment environment : environments) {
        FolderNode envFolder = new FolderNode(environment.getName(), Environment.class);
        environmentsFolder.addChild(envFolder);
        envFolder.addChild(new EnvironmentYamlNode(
            environment.getUuid(), environment.getAppId(), environment.getName() + ".yaml", EnvironmentYaml.class));
      }
      // ----------------- END ENVIRONMENTS SECTION ---------------------
    }

    rr.setResource(configFolder);

    return rr;
  }
}