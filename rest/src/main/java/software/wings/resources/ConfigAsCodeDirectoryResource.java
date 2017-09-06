package software.wings.resources;

import static software.wings.security.PermissionAttribute.ResourceType.SETTING;

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
import software.wings.security.annotations.AuthRule;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.SettingsService;
import software.wings.settings.SettingValue.SettingVariableTypes;
import software.wings.yaml.AmazonWebServicesYaml;
import software.wings.yaml.AppYaml;
import software.wings.yaml.EnvironmentYaml;
import software.wings.yaml.GoogleCloudPlatformYaml;
import software.wings.yaml.PhysicalDataCenterYaml;
import software.wings.yaml.ServiceYaml;
import software.wings.yaml.SetupYaml;
import software.wings.yaml.directory.CloudProviderYamlNode;
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
@AuthRule(SETTING)
public class ConfigAsCodeDirectoryResource {
  private AppService appService;
  private ServiceResourceService serviceResourceService;
  private EnvironmentService environmentService;
  private SettingsService settingsService;

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
    FolderNode cloudProvidersFolder = new FolderNode("Cloud Providers", SettingAttribute.class);
    theFolder.addChild(cloudProvidersFolder);

    doCloudProviderType(
        accountId, cloudProvidersFolder, "Amazon Web Services", SettingVariableTypes.AWS, AmazonWebServicesYaml.class);
    doCloudProviderType(accountId, cloudProvidersFolder, "Google Cloud Platform", SettingVariableTypes.GCP,
        GoogleCloudPlatformYaml.class);
    doCloudProviderType(accountId, cloudProvidersFolder, "Physical Data Centers",
        SettingVariableTypes.PHYSICAL_DATA_CENTER, PhysicalDataCenterYaml.class);

    /*
    // AWS
    FolderNode amazonWebServicesFolder = new FolderNode("Amazon Web Services", SettingAttribute.class);
    cloudProvidersFolder.addChild(amazonWebServicesFolder);

    List<SettingAttribute> settingAttributesAWS = settingsService.getGlobalSettingAttributesByType(accountId,
    SettingVariableTypes.AWS.name());

    // iterate over amazon web service providers
    for (SettingAttribute settingAttribute : settingAttributesAWS) {
      FolderNode awsFolder = new FolderNode(settingAttribute.getName(), SettingAttribute.class);
      amazonWebServicesFolder.addChild(awsFolder);
      awsFolder.addChild(new YamlNode(settingAttribute.getUuid(), settingAttribute.getName() + ".yaml",
    PhysicalDataCenterYaml.class));
    }

    // GCP
    FolderNode googleCloudPlatformFolder = new FolderNode("Google Cloud Platform", SettingAttribute.class);
    cloudProvidersFolder.addChild(googleCloudPlatformFolder);

    List<SettingAttribute> settingAttributesGCP = settingsService.getGlobalSettingAttributesByType(accountId,
    SettingVariableTypes.GCP.name());

    // iterate over google cloud platform providers
    for (SettingAttribute settingAttribute : settingAttributesGCP) {
      FolderNode gcpFolder = new FolderNode(settingAttribute.getName(), SettingAttribute.class);
      googleCloudPlatformFolder.addChild(gcpFolder);
      gcpFolder.addChild(new YamlNode(settingAttribute.getUuid(), settingAttribute.getName() + ".yaml",
    PhysicalDataCenterYaml.class));
    }

    // Physical Data Center
    FolderNode physicalDataCentersFolder = new FolderNode("Physical Data Center", SettingAttribute.class);
    cloudProvidersFolder.addChild(physicalDataCentersFolder);

    List<SettingAttribute> settingAttributesPDC = settingsService.getGlobalSettingAttributesByType(accountId,
    SettingVariableTypes.PHYSICAL_DATA_CENTER.name());

    // iterate over physical data centers
    for (SettingAttribute settingAttribute : settingAttributesPDC) {
      FolderNode pdcFolder = new FolderNode(settingAttribute.getName(), SettingAttribute.class);
      physicalDataCentersFolder.addChild(pdcFolder);
      pdcFolder.addChild(new YamlNode(settingAttribute.getUuid(), settingAttribute.getName() + ".yaml",
    PhysicalDataCenterYaml.class));
    }
    */
  }

  private void doCloudProviderType(
      String accountId, FolderNode parentFolder, String nodeName, SettingVariableTypes type, Class theClass) {
    FolderNode typeFolder = new FolderNode(nodeName, SettingAttribute.class);
    parentFolder.addChild(typeFolder);

    List<SettingAttribute> settingAttributes = settingsService.getGlobalSettingAttributesByType(accountId, type.name());

    // iterate over providers
    for (SettingAttribute settingAttribute : settingAttributes) {
      FolderNode cloudProviderFolder = new FolderNode(settingAttribute.getName(), SettingAttribute.class);
      typeFolder.addChild(cloudProviderFolder);
      cloudProviderFolder.addChild(new CloudProviderYamlNode(settingAttribute.getUuid(),
          settingAttribute.getValue().getType(), settingAttribute.getName() + ".yaml", theClass));
    }
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