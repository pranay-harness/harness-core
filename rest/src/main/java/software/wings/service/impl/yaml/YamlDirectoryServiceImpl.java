package software.wings.service.impl.yaml;

import static software.wings.beans.SearchFilter.Builder.aSearchFilter;
import static software.wings.dl.PageRequest.Builder.aPageRequest;

import org.hibernate.validator.constraints.NotEmpty;
import org.quartz.Trigger;
import software.wings.beans.AppDynamicsConfig;
import software.wings.beans.Application;
import software.wings.beans.BambooConfig;
import software.wings.beans.DockerConfig;
import software.wings.beans.ElkConfig;
import software.wings.beans.Environment;
import software.wings.beans.JenkinsConfig;
import software.wings.beans.Pipeline;
import software.wings.beans.SearchFilter.Operator;
import software.wings.beans.Service;
import software.wings.beans.SettingAttribute;
import software.wings.beans.Setup;
import software.wings.beans.SplunkConfig;
import software.wings.beans.Workflow;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.command.Command;
import software.wings.beans.command.ServiceCommand;
import software.wings.beans.config.ArtifactoryConfig;
import software.wings.beans.config.LogzConfig;
import software.wings.beans.config.NexusConfig;
import software.wings.dl.PageRequest;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.PipelineService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.WorkflowService;
import software.wings.service.intfc.yaml.YamlDirectoryService;
import software.wings.settings.SettingValue.SettingVariableTypes;
import software.wings.sm.StateMachine;
import software.wings.yaml.AmazonWebServicesYaml;
import software.wings.yaml.AppYaml;
import software.wings.yaml.ArtifactStreamYaml;
import software.wings.yaml.EnvironmentYaml;
import software.wings.yaml.GoogleCloudPlatformYaml;
import software.wings.yaml.PipelineYaml;
import software.wings.yaml.ServiceYaml;
import software.wings.yaml.SetupYaml;
import software.wings.yaml.WorkflowYaml;
import software.wings.yaml.directory.AppLevelYamlNode;
import software.wings.yaml.directory.DirectoryNode;
import software.wings.yaml.directory.FolderNode;
import software.wings.yaml.directory.ServiceLevelYamlNode;
import software.wings.yaml.directory.SettingAttributeYamlNode;
import software.wings.yaml.directory.YamlNode;
import software.wings.yaml.settingAttribute.PhysicalDataCenterYaml;

import java.util.List;
import javax.inject.Inject;

public class YamlDirectoryServiceImpl implements YamlDirectoryService {
  @Inject private AppService appService;
  @Inject private ServiceResourceService serviceResourceService;
  @Inject private EnvironmentService environmentService;
  @Inject private WorkflowService workflowService;
  @Inject private PipelineService pipelineService;
  // TODO - not sure what to use for this
  // @Inject private TriggerService triggerService;
  @Inject private SettingsService settingsService;
  @Inject private ArtifactStreamService artifactStreamService;

  @Override
  public DirectoryNode get(@NotEmpty String accountId) {
    FolderNode configFolder = new FolderNode("Setup", Setup.class);
    configFolder.addChild(new YamlNode("setup.yaml", SetupYaml.class));

    doApplications(configFolder, accountId);
    doCloudProviders(configFolder, accountId);
    doArtifactServers(configFolder, accountId);
    doCollaborationProviders(configFolder, accountId);
    doLoadBalancers(configFolder, accountId);
    doVerificationProviders(configFolder, accountId);

    return configFolder;
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
      doWorkflows(appFolder, app);
      doPipelines(appFolder, app);
      doTriggers(appFolder, app);
    }
  }

  private void doServices(FolderNode theFolder, Application app) {
    FolderNode servicesFolder = new FolderNode("Services", Service.class);
    theFolder.addChild(servicesFolder);

    List<Service> services = serviceResourceService.findServicesByApp(app.getAppId());

    if (services != null) {
      // iterate over services
      for (Service service : services) {
        FolderNode serviceFolder = new FolderNode(service.getName(), Service.class);
        servicesFolder.addChild(serviceFolder);
        serviceFolder.addChild(new AppLevelYamlNode(
            service.getUuid(), service.getAppId(), service.getName() + ".yaml", ServiceYaml.class));
        FolderNode serviceCommandsFolder = new FolderNode("Commands", ServiceCommand.class);
        serviceFolder.addChild(serviceCommandsFolder);

        // ------------------- SERVICE COMMANDS SECTION -----------------------
        List<ServiceCommand> serviceCommands = service.getServiceCommands();

        // iterate over service commands
        for (ServiceCommand serviceCommand : serviceCommands) {
          FolderNode scFolder = new FolderNode(serviceCommand.getName(), ServiceCommand.class);
          serviceCommandsFolder.addChild(scFolder);
          scFolder.addChild(new ServiceLevelYamlNode(serviceCommand.getUuid(), serviceCommand.getAppId(),
              serviceCommand.getServiceId(), serviceCommand.getName() + ".yaml", ServiceCommand.class));
          FolderNode versionsFolder = new FolderNode("Versions", Command.class);
          scFolder.addChild(versionsFolder);
        }
        // ------------------- END SERVICE COMMANDS SECTION -----------------------
      }
    }
  }

  private void doEnvironments(FolderNode theFolder, Application app) {
    FolderNode environmentsFolder = new FolderNode("Environments", Environment.class);
    theFolder.addChild(environmentsFolder);

    List<Environment> environments = environmentService.getEnvByApp(app.getAppId());

    if (environments != null) {
      // iterate over environments
      for (Environment environment : environments) {
        FolderNode envFolder = new FolderNode(environment.getName(), Environment.class);
        environmentsFolder.addChild(envFolder);
        envFolder.addChild(new AppLevelYamlNode(
            environment.getUuid(), environment.getAppId(), environment.getName() + ".yaml", EnvironmentYaml.class));
      }
    }
  }

  private void doWorkflows(FolderNode theFolder, Application app) {
    FolderNode workflowsFolder = new FolderNode("Workflows", Workflow.class);
    theFolder.addChild(workflowsFolder);

    PageRequest<Workflow> pageRequest =
        aPageRequest().addFilter(aSearchFilter().withField("appId", Operator.EQ, app.getAppId()).build()).build();
    List<Workflow> workflows = workflowService.listWorkflows(pageRequest).getResponse();

    if (workflows != null) {
      // iterate over workflows
      for (Workflow workflow : workflows) {
        FolderNode wrkflwFolder = new FolderNode(workflow.getName(), Workflow.class);
        workflowsFolder.addChild(wrkflwFolder);
        wrkflwFolder.addChild(new AppLevelYamlNode(
            workflow.getUuid(), workflow.getAppId(), workflow.getName() + ".yaml", WorkflowYaml.class));
        FolderNode versionsFolder = new FolderNode("Versions", StateMachine.class);
        wrkflwFolder.addChild(versionsFolder);
      }
    }
  }

  private void doPipelines(FolderNode theFolder, Application app) {
    FolderNode pipelinesFolder = new FolderNode("Pipelines", Pipeline.class);
    theFolder.addChild(pipelinesFolder);

    PageRequest<Pipeline> pageRequest =
        aPageRequest().addFilter(aSearchFilter().withField("appId", Operator.EQ, app.getAppId()).build()).build();
    List<Pipeline> pipelines = pipelineService.listPipelines(pageRequest).getResponse();

    if (pipelines != null) {
      // iterate over pipelines
      for (Pipeline pipeline : pipelines) {
        pipelinesFolder.addChild(new AppLevelYamlNode(
            pipeline.getUuid(), pipeline.getAppId(), pipeline.getName() + ".yaml", PipelineYaml.class));
      }
    }
  }

  private void doTriggers(FolderNode theFolder, Application app) {
    FolderNode triggersFolder = new FolderNode("Triggers", Trigger.class);
    theFolder.addChild(triggersFolder);

    PageRequest<ArtifactStream> pageRequest =
        aPageRequest().addFilter(aSearchFilter().withField("appId", Operator.EQ, app.getAppId()).build()).build();
    List<ArtifactStream> artifactStreams = artifactStreamService.list(pageRequest).getResponse();

    if (artifactStreams != null) {
      // iterate over artifactStreams
      for (ArtifactStream as : artifactStreams) {
        Service service = serviceResourceService.get(app.getAppId(), as.getServiceId());
        String name = "";
        if (service != null) {
          name = as.getSourceName() + "(" + service.getName() + ")";
        } else {
          // TODO - handle service not found
        }

        triggersFolder.addChild(
            new AppLevelYamlNode(as.getUuid(), as.getAppId(), name + ".yaml", ArtifactStreamYaml.class));
      }
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

    if (settingAttributes != null) {
      // iterate over providers
      for (SettingAttribute settingAttribute : settingAttributes) {
        typeFolder.addChild(new SettingAttributeYamlNode(settingAttribute.getUuid(),
            settingAttribute.getValue().getType(), settingAttribute.getName() + ".yaml", theClass));
      }
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

    if (settingAttributes != null) {
      // iterate over providers
      for (SettingAttribute settingAttribute : settingAttributes) {
        parentFolder.addChild(new SettingAttributeYamlNode(settingAttribute.getUuid(),
            settingAttribute.getValue().getType(), settingAttribute.getName() + ".yaml", theClass));
      }
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

    if (settingAttributes != null) {
      // iterate over providers
      for (SettingAttribute settingAttribute : settingAttributes) {
        typeFolder.addChild(new SettingAttributeYamlNode(settingAttribute.getUuid(),
            settingAttribute.getValue().getType(), settingAttribute.getName() + ".yaml", theClass));
      }
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

    if (settingAttributes != null) {
      // iterate over providers
      for (SettingAttribute settingAttribute : settingAttributes) {
        typeFolder.addChild(new SettingAttributeYamlNode(settingAttribute.getUuid(),
            settingAttribute.getValue().getType(), settingAttribute.getName() + ".yaml", theClass));
      }
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

    if (settingAttributes != null) {
      // iterate over providers
      for (SettingAttribute settingAttribute : settingAttributes) {
        typeFolder.addChild(new SettingAttributeYamlNode(settingAttribute.getUuid(),
            settingAttribute.getValue().getType(), settingAttribute.getName() + ".yaml", theClass));
      }
    }
  }
}
