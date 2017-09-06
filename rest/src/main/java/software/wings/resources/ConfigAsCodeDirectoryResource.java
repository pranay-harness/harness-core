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
import software.wings.beans.SettingAttribute;
import software.wings.beans.Setup;
import software.wings.beans.command.ServiceCommand;
import software.wings.dl.WingsPersistence;
import software.wings.security.annotations.AuthRule;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.SettingsService;
import software.wings.settings.SettingValue.SettingVariableTypes;
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
  private SettingsService settingsService;

  @com.google.inject.Inject private WingsPersistence wingsPersistence;

  private final Logger logger = LoggerFactory.getLogger(getClass());

  /**
   * Instantiates a new app yaml resource.
   *
   * @param appService             the app service
   * @param serviceResourceService the service (resource) service
   */
  @Inject
  public ConfigAsCodeDirectoryResource(AppService appService, ServiceResourceService serviceResourceService,
      EnvironmentService environmentService, SettingsService settingsService) {
    this.appService = appService;
    this.serviceResourceService = serviceResourceService;
    this.environmentService = environmentService;
    this.settingsService = settingsService;
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

    // example of getting a sample object hierarchy for testing/debugging:
    // rr.setResource(YamlHelper.sampleConfigAsCodeDirectory());

    FolderNode configFolder = new FolderNode("Setup", Setup.class);
    configFolder.addChild(new YamlNode("setup.yaml", SetupYaml.class));

    doApplications(configFolder, accountId);
    doCloudProviders(configFolder, accountId);
    doArtifactServers(configFolder);
    doCollaborationProviders(configFolder);
    doLoadBalancers(configFolder);
    doVerificationProviders(configFolder);

    rr.setResource(configFolder);

    return rr;
  }

  private void doApplications(FolderNode theFolder, String accountId) {
    FolderNode applicationsFolder = new FolderNode("Applications", Application.class);
    theFolder.addChild(applicationsFolder);

    List<Application> apps = appService.getAppsByAccountId(accountId);

    // iterate over applications
    for (Application app : apps) {
      FolderNode appFolder = new FolderNode(app.getName(), Application.class);
      applicationsFolder.addChild(appFolder);
      appFolder.addChild(new YamlNode(app.getUuid(), app.getName() + ".yaml", AppYaml.class));

      doServices(appFolder, app);
      doEnvironments(appFolder, app);
    }
  }

  private void doServices(FolderNode theFolder, Application app) {
    FolderNode servicesFolder = new FolderNode("Services", Service.class);
    theFolder.addChild(servicesFolder);

    List<Service> services = serviceResourceService.findServicesByApp(app.getAppId());

    // iterate over services
    for (Service service : services) {
      FolderNode serviceFolder = new FolderNode(service.getName(), Service.class);
      servicesFolder.addChild(serviceFolder);
      serviceFolder.addChild(
          new ServiceYamlNode(service.getUuid(), service.getAppId(), service.getName() + ".yaml", ServiceYaml.class));
      FolderNode serviceCommandsFolder = new FolderNode("Commands", ServiceCommand.class);
      serviceFolder.addChild(serviceCommandsFolder);

      // ------------------- SERVICE COMMANDS SECTION -----------------------
      List<ServiceCommand> serviceCommands = service.getServiceCommands();

      // iterate over service commands
      for (ServiceCommand serviceCommand : serviceCommands) {
        serviceCommandsFolder.addChild(new ServiceCommandYamlNode(serviceCommand.getUuid(), serviceCommand.getAppId(),
            serviceCommand.getServiceId(), serviceCommand.getName() + ".yaml", ServiceCommand.class));
      }
      // ------------------- END SERVICE COMMANDS SECTION -----------------------
    }
  }

  private void doEnvironments(FolderNode theFolder, Application app) {
    FolderNode environmentsFolder = new FolderNode("Environments", Environment.class);
    theFolder.addChild(environmentsFolder);

    List<Environment> environments = environmentService.getEnvByApp(app.getAppId());

    // iterate over environments
    for (Environment environment : environments) {
      FolderNode envFolder = new FolderNode(environment.getName(), Environment.class);
      environmentsFolder.addChild(envFolder);
      envFolder.addChild(new EnvironmentYamlNode(
          environment.getUuid(), environment.getAppId(), environment.getName() + ".yaml", EnvironmentYaml.class));
    }
  }

  private void doCloudProviders(FolderNode theFolder, String accountId) {
    // create cloud providers (and physical data centers)
    // TODO - Application.class is WRONG for this!
    FolderNode cloudProvidersFolder = new FolderNode("Cloud Providers", Application.class);
    theFolder.addChild(cloudProvidersFolder);

    // AWS
    // TODO - Application.class is WRONG for this!
    FolderNode awsFolder = new FolderNode("Amazon Web Services", Application.class);
    cloudProvidersFolder.addChild(awsFolder);

    // GCP
    // TODO - Application.class is WRONG for this!
    FolderNode gcpFolder = new FolderNode("Google Cloud Platform", Application.class);
    cloudProvidersFolder.addChild(gcpFolder);

    // Physical Data Center
    // TODO - Application.class is WRONG for this!
    FolderNode pdcFolder = new FolderNode("Physical Data Center", Application.class);
    cloudProvidersFolder.addChild(pdcFolder);

    // These should work - but don't due to a bug
    // List<SettingAttribute> settingAttributes = settingsService.getSettingAttributesByType(GLOBAL_APP_ID,
    // SettingVariableTypes.PHYSICAL_DATA_CENTER.name());  List<SettingAttribute> settingAttributes =
    // settingsService.getGlobalSettingAttributesByType(accountId, SettingVariableTypes.PHYSICAL_DATA_CENTER.name());

    // TODO - this direct query call is temporary until bug is fixed
    String type = SettingVariableTypes.PHYSICAL_DATA_CENTER.name();
    List<SettingAttribute> settingAttributes = wingsPersistence.createQuery(SettingAttribute.class)
                                                   .field("accountId")
                                                   .equal(accountId)
                                                   .field("value.type")
                                                   .equal(type)
                                                   .asList();

    logger.info("********** settingAttributes: " + settingAttributes);
  }

  private void doArtifactServers(FolderNode theFolder) {
    // create artifact servers
    // TODO - Application.class is WRONG for this!
    FolderNode artifactServersFolder = new FolderNode("Artifact Servers", Application.class);
    theFolder.addChild(artifactServersFolder);
  }

  private void doCollaborationProviders(FolderNode theFolder) {
    // create collaboration providers
    // TODO - Application.class is WRONG for this!
    FolderNode collaborationProvidersFolder = new FolderNode("Collaboration Providers", Application.class);
    theFolder.addChild(collaborationProvidersFolder);
  }

  private void doLoadBalancers(FolderNode theFolder) {
    // create load balancers
    // TODO - Application.class is WRONG for this!
    FolderNode loadBalancersFolder = new FolderNode("Load Balancers", Application.class);
    theFolder.addChild(loadBalancersFolder);

    // Elastic Classic Load Balancer
    // TODO - Application.class is WRONG for this!
    FolderNode elbFolder = new FolderNode("Elastic Classic Load Balancers", Application.class);
    loadBalancersFolder.addChild(elbFolder);
  }

  private void doVerificationProviders(FolderNode theFolder) {
    // create verification providers
    // TODO - Application.class is WRONG for this!
    FolderNode verificationProvidersFolder = new FolderNode("Verification Providers", Application.class);
    theFolder.addChild(verificationProvidersFolder);
  }
}