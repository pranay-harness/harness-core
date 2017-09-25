package software.wings.service.impl.yaml;

import static software.wings.beans.SearchFilter.Builder.aSearchFilter;
import static software.wings.dl.PageRequest.Builder.aPageRequest;

import org.hibernate.validator.constraints.NotEmpty;
import org.quartz.Trigger;
import software.wings.beans.Account;
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
import software.wings.beans.SplunkConfig;
import software.wings.beans.Workflow;
import software.wings.beans.artifact.ArtifactStream;
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
import software.wings.yaml.AmazonWebServicesYaml;
import software.wings.yaml.GoogleCloudPlatformYaml;
import software.wings.yaml.directory.AppLevelYamlNode;
import software.wings.yaml.directory.DirectoryNode;
import software.wings.yaml.directory.DirectoryPath;
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
  public DirectoryNode getDirectory(@NotEmpty String accountId) {
    DirectoryPath directoryPath = new DirectoryPath("setup");

    FolderNode configFolder = new FolderNode("Setup", Account.class, directoryPath);
    configFolder.addChild(new YamlNode(accountId, "setup.yaml", Account.class, directoryPath.clone().add(accountId)));

    doApplications(configFolder, accountId, directoryPath.clone());
    doCloudProviders(configFolder, accountId, directoryPath.clone());
    doArtifactServers(configFolder, accountId, directoryPath.clone());
    doCollaborationProviders(configFolder, accountId, directoryPath.clone());
    doLoadBalancers(configFolder, accountId, directoryPath.clone());
    doVerificationProviders(configFolder, accountId, directoryPath.clone());

    return configFolder;
  }

  private void doApplications(FolderNode theFolder, String accountId, DirectoryPath directoryPath) {
    FolderNode applicationsFolder =
        new FolderNode("Applications", Application.class, directoryPath.add("applications"));
    theFolder.addChild(applicationsFolder);

    List<Application> apps = appService.getAppsByAccountId(accountId);

    // iterate over applications
    for (Application app : apps) {
      DirectoryPath appPath = directoryPath.clone();
      FolderNode appFolder = new FolderNode(app.getName(), Application.class, appPath.add(app.getUuid()));
      applicationsFolder.addChild(appFolder);
      appFolder.addChild(new YamlNode(app.getUuid(), app.getName() + ".yaml", Application.class, appPath));

      doServices(appFolder, app, appPath.clone());
      doEnvironments(appFolder, app, appPath.clone());
      doWorkflows(appFolder, app, appPath.clone());
      doPipelines(appFolder, app, appPath.clone());
      doTriggers(appFolder, app, appPath.clone());
    }
  }

  private void doServices(FolderNode theFolder, Application app, DirectoryPath directoryPath) {
    FolderNode servicesFolder = new FolderNode("Services", Service.class, directoryPath.add("services"));
    theFolder.addChild(servicesFolder);

    List<Service> services = serviceResourceService.findServicesByApp(app.getAppId());

    if (services != null) {
      // iterate over services
      for (Service service : services) {
        DirectoryPath servicePath = directoryPath.clone();
        FolderNode serviceFolder = new FolderNode(service.getName(), Service.class, servicePath.add(service.getUuid()));
        servicesFolder.addChild(serviceFolder);
        serviceFolder.addChild(new AppLevelYamlNode(
            service.getUuid(), service.getAppId(), service.getName() + ".yaml", Service.class, servicePath));
        DirectoryPath serviceCommandPath = servicePath.clone().add("service_commands");
        FolderNode serviceCommandsFolder = new FolderNode("Commands", ServiceCommand.class, serviceCommandPath);
        serviceFolder.addChild(serviceCommandsFolder);

        // ------------------- SERVICE COMMANDS SECTION -----------------------
        List<ServiceCommand> serviceCommands = service.getServiceCommands();

        // iterate over service commands
        for (ServiceCommand serviceCommand : serviceCommands) {
          serviceCommandsFolder.addChild(new ServiceLevelYamlNode(serviceCommand.getUuid(), serviceCommand.getAppId(),
              serviceCommand.getServiceId(), serviceCommand.getName() + ".yaml", ServiceCommand.class,
              serviceCommandPath.clone().add(serviceCommand.getUuid())));
        }
        // ------------------- END SERVICE COMMANDS SECTION -----------------------
      }
    }
  }

  private void doEnvironments(FolderNode theFolder, Application app, DirectoryPath directoryPath) {
    FolderNode environmentsFolder =
        new FolderNode("Environments", Environment.class, directoryPath.add("environments"));
    theFolder.addChild(environmentsFolder);

    List<Environment> environments = environmentService.getEnvByApp(app.getAppId());

    if (environments != null) {
      // iterate over environments
      for (Environment environment : environments) {
        DirectoryPath envPath = directoryPath.clone();
        environmentsFolder.addChild(new AppLevelYamlNode(environment.getUuid(), environment.getAppId(),
            environment.getName() + ".yaml", Environment.class, envPath.add(environment.getUuid())));
      }
    }
  }

  private void doWorkflows(FolderNode theFolder, Application app, DirectoryPath directoryPath) {
    FolderNode workflowsFolder = new FolderNode("Workflows", Workflow.class, directoryPath.add("workflows"));
    theFolder.addChild(workflowsFolder);

    PageRequest<Workflow> pageRequest =
        aPageRequest().addFilter(aSearchFilter().withField("appId", Operator.EQ, app.getAppId()).build()).build();
    List<Workflow> workflows = workflowService.listWorkflows(pageRequest).getResponse();

    if (workflows != null) {
      // iterate over workflows
      for (Workflow workflow : workflows) {
        DirectoryPath workflowPath = directoryPath.clone();
        workflowsFolder.addChild(new AppLevelYamlNode(workflow.getUuid(), workflow.getAppId(),
            workflow.getName() + ".yaml", Workflow.class, workflowPath.add(workflow.getUuid())));
      }
    }
  }

  private void doPipelines(FolderNode theFolder, Application app, DirectoryPath directoryPath) {
    FolderNode pipelinesFolder = new FolderNode("Pipelines", Pipeline.class, directoryPath.add("pipelines"));
    theFolder.addChild(pipelinesFolder);

    PageRequest<Pipeline> pageRequest =
        aPageRequest().addFilter(aSearchFilter().withField("appId", Operator.EQ, app.getAppId()).build()).build();
    List<Pipeline> pipelines = pipelineService.listPipelines(pageRequest).getResponse();

    if (pipelines != null) {
      // iterate over pipelines
      for (Pipeline pipeline : pipelines) {
        DirectoryPath pipelinePath = directoryPath.clone();
        pipelinesFolder.addChild(new AppLevelYamlNode(pipeline.getUuid(), pipeline.getAppId(),
            pipeline.getName() + ".yaml", Pipeline.class, pipelinePath.add(pipeline.getUuid())));
      }
    }
  }

  private void doTriggers(FolderNode theFolder, Application app, DirectoryPath directoryPath) {
    FolderNode triggersFolder = new FolderNode("Triggers", Trigger.class, directoryPath.add("triggers"));
    theFolder.addChild(triggersFolder);

    PageRequest<ArtifactStream> pageRequest =
        aPageRequest().addFilter(aSearchFilter().withField("appId", Operator.EQ, app.getAppId()).build()).build();
    List<ArtifactStream> artifactStreams = artifactStreamService.list(pageRequest).getResponse();

    if (artifactStreams != null) {
      // iterate over artifactStreams
      for (ArtifactStream as : artifactStreams) {
        DirectoryPath asPath = directoryPath.clone();
        Service service = serviceResourceService.get(app.getAppId(), as.getServiceId());
        String name = "";
        if (service != null) {
          name = as.getSourceName() + "(" + service.getName() + ")";
        } else {
          // TODO - handle service not found
        }

        triggersFolder.addChild(new AppLevelYamlNode(
            as.getUuid(), as.getAppId(), name + ".yaml", ArtifactStream.class, asPath.add(as.getUuid())));
      }
    }
  }

  private void doCloudProviders(FolderNode theFolder, String accountId, DirectoryPath directoryPath) {
    // create cloud providers (and physical data centers)
    FolderNode cloudProvidersFolder =
        new FolderNode("Cloud Providers", SettingAttribute.class, directoryPath.add("cloud_providers"));
    theFolder.addChild(cloudProvidersFolder);

    // TODO - should these use AwsConfig GcpConfig, etc. instead?
    doCloudProviderType(accountId, cloudProvidersFolder, "Amazon Web Services", SettingVariableTypes.AWS,
        AmazonWebServicesYaml.class, directoryPath.clone());
    doCloudProviderType(accountId, cloudProvidersFolder, "Google Cloud Platform", SettingVariableTypes.GCP,
        GoogleCloudPlatformYaml.class, directoryPath.clone());
    doCloudProviderType(accountId, cloudProvidersFolder, "Physical Data Centers",
        SettingVariableTypes.PHYSICAL_DATA_CENTER, PhysicalDataCenterYaml.class, directoryPath.clone());
  }

  private void doCloudProviderType(String accountId, FolderNode parentFolder, String nodeName,
      SettingVariableTypes type, Class theClass, DirectoryPath directoryPath) {
    FolderNode typeFolder =
        new FolderNode(nodeName, SettingAttribute.class, directoryPath.add(nodeName.toLowerCase().replace(' ', '_')));
    parentFolder.addChild(typeFolder);

    List<SettingAttribute> settingAttributes = settingsService.getGlobalSettingAttributesByType(accountId, type.name());

    if (settingAttributes != null) {
      // iterate over providers
      for (SettingAttribute settingAttribute : settingAttributes) {
        DirectoryPath cpPath = directoryPath.clone();
        typeFolder.addChild(
            new SettingAttributeYamlNode(settingAttribute.getUuid(), settingAttribute.getValue().getType(),
                settingAttribute.getName() + ".yaml", SettingAttribute.class, cpPath.add(settingAttribute.getUuid())));
      }
    }
  }

  private void doArtifactServers(FolderNode theFolder, String accountId, DirectoryPath directoryPath) {
    // create artifact servers
    FolderNode artifactServersFolder =
        new FolderNode("Artifact Servers", SettingAttribute.class, directoryPath.add("artifact_servers"));
    theFolder.addChild(artifactServersFolder);

    doArtifactServerType(
        accountId, artifactServersFolder, SettingVariableTypes.JENKINS, JenkinsConfig.class, directoryPath.clone());
    doArtifactServerType(
        accountId, artifactServersFolder, SettingVariableTypes.BAMBOO, BambooConfig.class, directoryPath.clone());
    doArtifactServerType(
        accountId, artifactServersFolder, SettingVariableTypes.DOCKER, DockerConfig.class, directoryPath.clone());
    doArtifactServerType(
        accountId, artifactServersFolder, SettingVariableTypes.NEXUS, NexusConfig.class, directoryPath.clone());
    doArtifactServerType(accountId, artifactServersFolder, SettingVariableTypes.ARTIFACTORY, ArtifactoryConfig.class,
        directoryPath.clone());
  }

  private void doArtifactServerType(String accountId, FolderNode parentFolder, SettingVariableTypes type,
      Class theClass, DirectoryPath directoryPath) {
    List<SettingAttribute> settingAttributes = settingsService.getGlobalSettingAttributesByType(accountId, type.name());

    if (settingAttributes != null) {
      // iterate over providers
      for (SettingAttribute settingAttribute : settingAttributes) {
        DirectoryPath asPath = directoryPath.clone();
        parentFolder.addChild(
            new SettingAttributeYamlNode(settingAttribute.getUuid(), settingAttribute.getValue().getType(),
                settingAttribute.getName() + ".yaml", SettingAttribute.class, asPath.add(settingAttribute.getUuid())));
      }
    }
  }

  private void doCollaborationProviders(FolderNode theFolder, String accountId, DirectoryPath directoryPath) {
    // create collaboration providers
    FolderNode collaborationProvidersFolder =
        new FolderNode("Collaboration Providers", SettingAttribute.class, directoryPath.add("collaboration_providers"));
    theFolder.addChild(collaborationProvidersFolder);

    doCollaborationProviderType(accountId, collaborationProvidersFolder, "SMTP", SettingVariableTypes.SMTP,
        SettingAttribute.class, directoryPath.clone());
    doCollaborationProviderType(accountId, collaborationProvidersFolder, "Slack", SettingVariableTypes.SLACK,
        SettingAttribute.class, directoryPath.clone());
  }

  private void doCollaborationProviderType(String accountId, FolderNode parentFolder, String nodeName,
      SettingVariableTypes type, Class theClass, DirectoryPath directoryPath) {
    FolderNode typeFolder =
        new FolderNode(nodeName, SettingAttribute.class, directoryPath.add(nodeName.toLowerCase().replace(' ', '_')));
    parentFolder.addChild(typeFolder);

    List<SettingAttribute> settingAttributes = settingsService.getGlobalSettingAttributesByType(accountId, type.name());

    if (settingAttributes != null) {
      // iterate over providers
      for (SettingAttribute settingAttribute : settingAttributes) {
        DirectoryPath cpPath = directoryPath.clone();
        typeFolder.addChild(
            new SettingAttributeYamlNode(settingAttribute.getUuid(), settingAttribute.getValue().getType(),
                settingAttribute.getName() + ".yaml", SettingAttribute.class, cpPath.add(settingAttribute.getUuid())));
      }
    }
  }

  private void doLoadBalancers(FolderNode theFolder, String accountId, DirectoryPath directoryPath) {
    // create load balancers
    FolderNode loadBalancersFolder =
        new FolderNode("Load Balancers", SettingAttribute.class, directoryPath.add("load_balancers"));
    theFolder.addChild(loadBalancersFolder);

    doLoadBalancerType(accountId, loadBalancersFolder, "Elastic Classic Load Balancers", SettingVariableTypes.ELB,
        SettingAttribute.class, directoryPath.clone());
  }

  private void doLoadBalancerType(String accountId, FolderNode parentFolder, String nodeName, SettingVariableTypes type,
      Class theClass, DirectoryPath directoryPath) {
    FolderNode typeFolder =
        new FolderNode(nodeName, SettingAttribute.class, directoryPath.add(nodeName.toLowerCase().replace(' ', '_')));
    parentFolder.addChild(typeFolder);

    List<SettingAttribute> settingAttributes = settingsService.getGlobalSettingAttributesByType(accountId, type.name());

    if (settingAttributes != null) {
      // iterate over providers
      for (SettingAttribute settingAttribute : settingAttributes) {
        DirectoryPath lbPath = directoryPath.clone();
        typeFolder.addChild(
            new SettingAttributeYamlNode(settingAttribute.getUuid(), settingAttribute.getValue().getType(),
                settingAttribute.getName() + ".yaml", SettingAttribute.class, lbPath.add(settingAttribute.getUuid())));
      }
    }
  }

  private void doVerificationProviders(FolderNode theFolder, String accountId, DirectoryPath directoryPath) {
    // create verification providers
    FolderNode verificationProvidersFolder =
        new FolderNode("Verification Providers", SettingAttribute.class, directoryPath.add("verification_providers"));
    theFolder.addChild(verificationProvidersFolder);

    doVerificationProviderType(accountId, verificationProvidersFolder, "Jenkins", SettingVariableTypes.JENKINS,
        JenkinsConfig.class, directoryPath.clone());
    doVerificationProviderType(accountId, verificationProvidersFolder, "AppDynamics", SettingVariableTypes.APP_DYNAMICS,
        AppDynamicsConfig.class, directoryPath.clone());
    doVerificationProviderType(accountId, verificationProvidersFolder, "Splunk", SettingVariableTypes.SPLUNK,
        SplunkConfig.class, directoryPath.clone());
    doVerificationProviderType(accountId, verificationProvidersFolder, "ELK", SettingVariableTypes.ELK, ElkConfig.class,
        directoryPath.clone());
    doVerificationProviderType(accountId, verificationProvidersFolder, "LOGZ", SettingVariableTypes.LOGZ,
        LogzConfig.class, directoryPath.clone());
  }

  private void doVerificationProviderType(String accountId, FolderNode parentFolder, String nodeName,
      SettingVariableTypes type, Class theClass, DirectoryPath directoryPath) {
    FolderNode typeFolder =
        new FolderNode(nodeName, SettingAttribute.class, directoryPath.add(nodeName.toLowerCase().replace(' ', '_')));
    parentFolder.addChild(typeFolder);

    List<SettingAttribute> settingAttributes = settingsService.getGlobalSettingAttributesByType(accountId, type.name());

    if (settingAttributes != null) {
      // iterate over providers
      for (SettingAttribute settingAttribute : settingAttributes) {
        DirectoryPath vpPath = directoryPath.clone();
        typeFolder.addChild(
            new SettingAttributeYamlNode(settingAttribute.getUuid(), settingAttribute.getValue().getType(),
                settingAttribute.getName() + ".yaml", SettingAttribute.class, vpPath.add(settingAttribute.getUuid())));
      }
    }
  }
}
