package software.wings.resources;

import static software.wings.security.PermissionAttribute.ResourceType.APPLICATION;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.dataformat.yaml.snakeyaml.Yaml;
import io.swagger.annotations.Api;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Application;
import software.wings.beans.RestResponse;
import software.wings.beans.Service;
import software.wings.beans.Setup;
import software.wings.beans.command.ServiceCommand;
import software.wings.security.annotations.AuthRule;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.yaml.AppYaml;
import software.wings.yaml.ServiceYaml;
import software.wings.yaml.SetupYaml;
import software.wings.yaml.YamlHelper;
import software.wings.yaml.YamlPayload;
import software.wings.yaml.directory.DirectoryNode;
import software.wings.yaml.directory.FolderNode;
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

  private final Logger logger = LoggerFactory.getLogger(getClass());

  /**
   * Instantiates a new app yaml resource.
   *
   * @param appService             the app service
   * @param serviceResourceService the service (resource) service
   */
  @Inject
  public ConfigAsCodeDirectoryResource(AppService appService, ServiceResourceService serviceResourceService) {
    this.appService = appService;
    this.serviceResourceService = serviceResourceService;
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
  public RestResponse<YamlPayload> get(@PathParam("accountId") String accountId) {
    RestResponse rr = new RestResponse<>();

    List<Application> apps = appService.getAppsByAccountId(accountId);

    logger.info("***************** apps: " + apps);

    rr.setResource(sampleConfigAsCodeDirectory());

    return rr;
  }

  private FolderNode sampleConfigAsCodeDirectory() {
    FolderNode config = new FolderNode("config", Setup.class);
    config.addChild(new YamlNode("setup.yaml", SetupYaml.class));
    FolderNode applications = new FolderNode("applications", Application.class);
    config.addChild(applications);

    FolderNode myapp1 = new FolderNode("Myapp1", Application.class);
    applications.addChild(myapp1);
    myapp1.addChild(new YamlNode("Myapp1.yaml", AppYaml.class));
    FolderNode myapp1_services = new FolderNode("services", Service.class);
    applications.addChild(myapp1_services);

    FolderNode myapp1_Login = new FolderNode("Login", Service.class);
    myapp1_services.addChild(myapp1_Login);
    myapp1_Login.addChild(new YamlNode("Login.yaml", ServiceYaml.class));
    FolderNode myapp1_Login_serviceCommands = new FolderNode("service-commands", ServiceCommand.class);
    myapp1_Login.addChild(myapp1_Login_serviceCommands);
    myapp1_Login_serviceCommands.addChild(new YamlNode("start.yaml", ServiceCommand.class));
    myapp1_Login_serviceCommands.addChild(new YamlNode("install.yaml", ServiceCommand.class));
    myapp1_Login_serviceCommands.addChild(new YamlNode("stop.yaml", ServiceCommand.class));

    FolderNode myapp1_Order = new FolderNode("Order", Service.class);
    myapp1_services.addChild(myapp1_Order);
    myapp1_Order.addChild(new YamlNode("Order.yaml", ServiceYaml.class));
    FolderNode myapp1_Order_serviceCommands = new FolderNode("service-commands", ServiceCommand.class);
    myapp1_Order.addChild(myapp1_Order_serviceCommands);

    FolderNode myapp2 = new FolderNode("Myapp2", Application.class);
    applications.addChild(myapp2);
    myapp2.addChild(new YamlNode("Myapp2.yaml", AppYaml.class));
    FolderNode myapp2_services = new FolderNode("services", Service.class);
    applications.addChild(myapp2_services);

    return config;
  }
}