package software.wings.resources;

import static software.wings.security.PermissionAttribute.ResourceType.SETTING;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.AppDynamicsConfig;
import software.wings.beans.Application;
import software.wings.beans.BambooConfig;
import software.wings.beans.DockerConfig;
import software.wings.beans.ElkConfig;
import software.wings.beans.Environment;
import software.wings.beans.JenkinsConfig;
import software.wings.beans.RestResponse;
import software.wings.beans.Service;
import software.wings.beans.SettingAttribute;
import software.wings.beans.Setup;
import software.wings.beans.SplunkConfig;
import software.wings.beans.command.ServiceCommand;
import software.wings.beans.config.ArtifactoryConfig;
import software.wings.beans.config.LogzConfig;
import software.wings.beans.config.NexusConfig;
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
   * @param environmentService     the environment service
   * @param settingsService        the settings service
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
    doArtifactServers(configFolder, accountId);
    doCollaborationProviders(configFolder, accountId);
    doLoadBalancers(configFolder, accountId);
    doVerificationProviders(configFolder, accountId);

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

    // TODO - should these use AwsConfig GcpConfig, etc. instead?
    doCloudProviderType(
        accountId, cloudProvidersFolder, "Amazon Web Services", SettingVariableTypes.AWS, AmazonWebServicesYaml.class);
    doCloudProviderType(accountId, cloudProvidersFolder, "Google Cloud Platform", SettingVariableTypes.GCP,
        GoogleCloudPlatformYaml.class);
    doCloudProviderType(accountId, cloudProvidersFolder, "Physical Data Centers",
        SettingVariableTypes.PHYSICAL_DATA_CENTER, PhysicalDataCenterYaml.class);
  }

  private void doCloudProviderType(
      String accountId, FolderNode parentFolder, String nodeName, SettingVariableTypes type, Class theClass) {
    FolderNode typeFolder = new FolderNode(nodeName, SettingAttribute.class);
    parentFolder.addChild(typeFolder);

    List<SettingAttribute> settingAttributes = settingsService.getGlobalSettingAttributesByType(accountId, type.name());

    // iterate over providers
    for (SettingAttribute settingAttribute : settingAttributes) {
      typeFolder.addChild(new CloudProviderYamlNode(settingAttribute.getUuid(), settingAttribute.getValue().getType(),
          settingAttribute.getName() + ".yaml", theClass));
    }
  }

  private void doArtifactServers(FolderNode theFolder, String accountId) {
    // create artifact servers
    FolderNode artifactServersFolder = new FolderNode("Artifact Servers", SettingAttribute.class);
    theFolder.addChild(artifactServersFolder);

    doArtifactServerType(accountId, artifactServersFolder, SettingVariableTypes.JENKINS, JenkinsConfig.class);
    doArtifactServerType(accountId, artifactServersFolder, SettingVariableTypes.BAMBOO, BambooConfig.class);
    doArtifactServerType(accountId, artifactServersFolder, SettingVariableTypes.DOCKER, DockerConfig.class);
    doArtifactServerType(accountId, artifactServersFolder, SettingVariableTypes.NEXUS, NexusConfig.class);
    doArtifactServerType(accountId, artifactServersFolder, SettingVariableTypes.ARTIFACTORY, ArtifactoryConfig.class);
  }

  private void doArtifactServerType(
      String accountId, FolderNode parentFolder, SettingVariableTypes type, Class theClass) {
    List<SettingAttribute> settingAttributes = settingsService.getGlobalSettingAttributesByType(accountId, type.name());

    // iterate over providers
    for (SettingAttribute settingAttribute : settingAttributes) {
      parentFolder.addChild(new CloudProviderYamlNode(settingAttribute.getUuid(), settingAttribute.getValue().getType(),
          settingAttribute.getName() + ".yaml", theClass));
    }
  }

  private void doCollaborationProviders(FolderNode theFolder, String accountId) {
    // create collaboration providers
    // TODO - Application.class is WRONG for this!
    FolderNode collaborationProvidersFolder = new FolderNode("Collaboration Providers", Application.class);
    theFolder.addChild(collaborationProvidersFolder);

    // TODO - SettingAttribute.class may be WRONG for these!
    doCollaborationProviderType(
        accountId, collaborationProvidersFolder, "SMTP", SettingVariableTypes.SMTP, SettingAttribute.class);
    doCollaborationProviderType(
        accountId, collaborationProvidersFolder, "Slack", SettingVariableTypes.SLACK, SettingAttribute.class);
  }

  private void doCollaborationProviderType(
      String accountId, FolderNode parentFolder, String nodeName, SettingVariableTypes type, Class theClass) {
    FolderNode typeFolder = new FolderNode(nodeName, SettingAttribute.class);
    parentFolder.addChild(typeFolder);

    List<SettingAttribute> settingAttributes = settingsService.getGlobalSettingAttributesByType(accountId, type.name());

    // iterate over providers
    for (SettingAttribute settingAttribute : settingAttributes) {
      typeFolder.addChild(new CloudProviderYamlNode(settingAttribute.getUuid(), settingAttribute.getValue().getType(),
          settingAttribute.getName() + ".yaml", theClass));
    }
  }

  private void doLoadBalancers(FolderNode theFolder, String accountId) {
    // create load balancers
    // TODO - Application.class is WRONG for this!
    FolderNode loadBalancersFolder = new FolderNode("Load Balancers", Application.class);
    theFolder.addChild(loadBalancersFolder);

    // TODO - SettingAttribute.class may be WRONG for these!
    doLoadBalancerType(accountId, loadBalancersFolder, "Elastic Classic Load Balancers", SettingVariableTypes.ELB,
        SettingAttribute.class);
  }

  private void doLoadBalancerType(
      String accountId, FolderNode parentFolder, String nodeName, SettingVariableTypes type, Class theClass) {
    FolderNode typeFolder = new FolderNode(nodeName, SettingAttribute.class);
    parentFolder.addChild(typeFolder);

    List<SettingAttribute> settingAttributes = settingsService.getGlobalSettingAttributesByType(accountId, type.name());

    // iterate over providers
    for (SettingAttribute settingAttribute : settingAttributes) {
      typeFolder.addChild(new CloudProviderYamlNode(settingAttribute.getUuid(), settingAttribute.getValue().getType(),
          settingAttribute.getName() + ".yaml", theClass));
    }
  }

  private void doVerificationProviders(FolderNode theFolder, String accountId) {
    // create verification providers
    FolderNode verificationProvidersFolder = new FolderNode("Verification Providers", SettingAttribute.class);
    theFolder.addChild(verificationProvidersFolder);

    doVerificationProviderType(
        accountId, verificationProvidersFolder, "Jenkins", SettingVariableTypes.JENKINS, JenkinsConfig.class);
    doVerificationProviderType(accountId, verificationProvidersFolder, "AppDynamics", SettingVariableTypes.APP_DYNAMICS,
        AppDynamicsConfig.class);
    doVerificationProviderType(
        accountId, verificationProvidersFolder, "Splunk", SettingVariableTypes.SPLUNK, SplunkConfig.class);
    doVerificationProviderType(
        accountId, verificationProvidersFolder, "ELK", SettingVariableTypes.ELK, ElkConfig.class);
    doVerificationProviderType(
        accountId, verificationProvidersFolder, "LOGZ", SettingVariableTypes.LOGZ, LogzConfig.class);
  }

  private void doVerificationProviderType(
      String accountId, FolderNode parentFolder, String nodeName, SettingVariableTypes type, Class theClass) {
    FolderNode typeFolder = new FolderNode(nodeName, SettingAttribute.class);
    parentFolder.addChild(typeFolder);

    List<SettingAttribute> settingAttributes = settingsService.getGlobalSettingAttributesByType(accountId, type.name());

    // iterate over providers
    for (SettingAttribute settingAttribute : settingAttributes) {
      typeFolder.addChild(new CloudProviderYamlNode(settingAttribute.getUuid(), settingAttribute.getValue().getType(),
          settingAttribute.getName() + ".yaml", theClass));
    }
  }
}