package software.wings.service.impl.yaml;

import static software.wings.beans.SearchFilter.Builder.aSearchFilter;
import static software.wings.dl.PageRequest.Builder.aPageRequest;

import org.hibernate.validator.constraints.NotEmpty;
import org.quartz.Trigger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.PipelineService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.WorkflowService;
import software.wings.service.intfc.yaml.AppYamlResourceService;
import software.wings.service.intfc.yaml.EntityUpdateService;
import software.wings.service.intfc.yaml.ServiceYamlResourceService;
import software.wings.service.intfc.yaml.SetupYamlResourceService;
import software.wings.service.intfc.yaml.YamlDirectoryService;
import software.wings.service.intfc.yaml.YamlGitSyncService;
import software.wings.service.intfc.yaml.YamlResourceService;
import software.wings.settings.SettingValue.SettingVariableTypes;
import software.wings.yaml.AmazonWebServicesYaml;
import software.wings.yaml.GoogleCloudPlatformYaml;
import software.wings.yaml.directory.AppLevelYamlNode;
import software.wings.yaml.directory.DirectoryNode;
import software.wings.yaml.directory.DirectoryNode.NodeType;
import software.wings.yaml.directory.DirectoryPath;
import software.wings.yaml.directory.FolderNode;
import software.wings.yaml.directory.ServiceLevelYamlNode;
import software.wings.yaml.directory.SettingAttributeYamlNode;
import software.wings.yaml.directory.YamlNode;
import software.wings.yaml.gitSync.EntityUpdateEvent.SourceType;
import software.wings.yaml.gitSync.EntityUpdateListEvent;
import software.wings.yaml.gitSync.GitSyncFile;
import software.wings.yaml.gitSync.YamlGitSync;
import software.wings.yaml.settingAttribute.PhysicalDataCenterYaml;

import java.util.List;
import javax.inject.Inject;

public class YamlDirectoryServiceImpl implements YamlDirectoryService {
  private final Logger logger = LoggerFactory.getLogger(getClass());

  @Inject private AppService appService;
  @Inject private ServiceResourceService serviceResourceService;
  @Inject private EnvironmentService environmentService;
  @Inject private WorkflowService workflowService;
  @Inject private PipelineService pipelineService;
  // TODO - not sure what to use for this
  // @Inject private TriggerService triggerService;
  @Inject private SettingsService settingsService;
  @Inject private ArtifactStreamService artifactStreamService;
  @Inject private YamlGitSyncService yamlGitSyncService;
  @Inject private EntityUpdateService entityUpdateService;
  @Inject private AccountService accountService;
  @Inject private AppYamlResourceService appYamlResourceService;
  @Inject private ServiceYamlResourceService serviceYamlResourceService;
  @Inject private YamlResourceService yamlResourceService;
  @Inject private SetupYamlResourceService setupYamlResourceService;

  @Override
  public DirectoryNode pushDirectory(@NotEmpty String accountId, boolean filterCustomGitSync) {
    String setupEntityId = "setup";
    FolderNode top = getDirectory(accountId, setupEntityId, filterCustomGitSync);

    if (top.getType() != NodeType.FOLDER) {
      // TODO - handle error
      return null;
    }

    YamlGitSync ygs = yamlGitSyncService.get(setupEntityId);

    if (ygs == null) {
      // TODO - handle error
      return null;
    }

    EntityUpdateListEvent eule =
        EntityUpdateListEvent.Builder.anEntityUpdateListEvent().withAccountId(accountId).withTreeSync(true).build();

    Account account = accountService.get(accountId);
    // TODO - we may want to add a new SourceType for this scenario (?)
    eule.addEntityUpdateEvent(entityUpdateService.setupListUpdate(account, SourceType.ENTITY_UPDATE));

    // traverse the directory and add all the files
    eule = traverseDirectory(eule, top, "", SourceType.ENTITY_UPDATE);

    entityUpdateService.queueEntityUpdateList(eule);

    return top;
  }

  public EntityUpdateListEvent traverseDirectory(
      EntityUpdateListEvent eule, FolderNode fn, String path, SourceType sourceType) {
    path = path + "/" + fn.getName();

    for (DirectoryNode dn : fn.getChildren()) {
      logger.info(path + " :: " + dn.getName());

      if (dn instanceof YamlNode) {
        String entityId = ((YamlNode) dn).getUuid();
        String yaml = "";
        String accountId = eule.getAccountId();
        String appId = "";
        String settingVariableType = "";

        switch (dn.getShortClassName()) {
          case "Account":
            yaml = setupYamlResourceService.getSetup(accountId).getResource().getYaml();
            break;
          case "Application":
            appId = entityId;
            yaml = appYamlResourceService.getApp(entityId).getResource().getYaml();
            break;
          case "Service":
            appId = ((AppLevelYamlNode) dn).getAppId();
            Service service = serviceResourceService.get(appId, entityId);
            if (service != null) {
              yaml = serviceYamlResourceService.getServiceYaml(service);
            }
            break;
          case "Environment":
            appId = ((AppLevelYamlNode) dn).getAppId();
            yaml = yamlResourceService.getEnvironment(appId, entityId).getResource().getYaml();
            break;
          case "ServiceCommand":
            appId = ((ServiceLevelYamlNode) dn).getAppId();
            yaml = yamlResourceService.getServiceCommand(appId, entityId).getResource().getYaml();
            break;
          case "Workflow":
            appId = ((AppLevelYamlNode) dn).getAppId();
            yaml = yamlResourceService.getWorkflow(appId, entityId).getResource().getYaml();
            break;
          case "Pipeline":
            appId = ((AppLevelYamlNode) dn).getAppId();
            yaml = yamlResourceService.getPipeline(appId, entityId).getResource().getYaml();
            break;
          case "Trigger":
            appId = ((AppLevelYamlNode) dn).getAppId();
            yaml = yamlResourceService.getTrigger(appId, entityId).getResource().getYaml();
            break;
          case "SettingAttribute":
            yaml = yamlResourceService.getSettingAttribute(accountId, entityId).getResource().getYaml();
            settingVariableType = ((SettingAttributeYamlNode) dn).getSettingVariableType();
            break;
          default:
            // nothing to do
        }

        eule.addGitSyncFile(GitSyncFile.Builder.aGitSyncFile()
                                .withName(dn.getName())
                                .withYaml(yaml)
                                .withSourceType(sourceType)
                                .withClass(dn.getTheClass())
                                .withRootPath(path)
                                .withEntityId(entityId)
                                .withAccountId(accountId)
                                .withAppId(appId)
                                .withSettingVariableType(settingVariableType)
                                .build());
      }

      if (dn instanceof FolderNode) {
        traverseDirectory(eule, (FolderNode) dn, path, sourceType);
      }
    }

    return eule;
  }

  @Override
  public DirectoryNode getDirectory(@NotEmpty String accountId) {
    return getDirectory(accountId, accountId, false);
  }

  @Override
  public FolderNode getDirectory(@NotEmpty String accountId, String entityId, boolean filterCustomGitSync) {
    DirectoryPath directoryPath = new DirectoryPath("setup");

    FolderNode configFolder = new FolderNode("Setup", Account.class, directoryPath, yamlGitSyncService);
    configFolder.addChild(
        new YamlNode(accountId, "setup.yaml", Account.class, directoryPath.clone().add(accountId), yamlGitSyncService));

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
        new FolderNode("Applications", Application.class, directoryPath.add("applications"), yamlGitSyncService);
    theFolder.addChild(applicationsFolder);

    List<Application> apps = appService.getAppsByAccountId(accountId);

    // iterate over applications
    for (Application app : apps) {
      DirectoryPath appPath = directoryPath.clone();
      FolderNode appFolder = new FolderNode(
          app.getName(), Application.class, appPath.add(app.getUuid()), app.getUuid(), yamlGitSyncService);
      applicationsFolder.addChild(appFolder);
      appFolder.addChild(
          new YamlNode(app.getUuid(), app.getName() + ".yaml", Application.class, appPath, yamlGitSyncService));

      doServices(appFolder, app, appPath.clone());
      doEnvironments(appFolder, app, appPath.clone());
      doWorkflows(appFolder, app, appPath.clone());
      doPipelines(appFolder, app, appPath.clone());
      doTriggers(appFolder, app, appPath.clone());
    }
  }

  private void doServices(FolderNode theFolder, Application app, DirectoryPath directoryPath) {
    FolderNode servicesFolder =
        new FolderNode("Services", Service.class, directoryPath.add("services"), app.getUuid(), yamlGitSyncService);
    theFolder.addChild(servicesFolder);

    List<Service> services = serviceResourceService.findServicesByApp(app.getAppId());

    if (services != null) {
      // iterate over services
      for (Service service : services) {
        DirectoryPath servicePath = directoryPath.clone();
        FolderNode serviceFolder = new FolderNode(service.getName(), Service.class, servicePath.add(service.getUuid()),
            service.getAppId(), yamlGitSyncService);
        servicesFolder.addChild(serviceFolder);
        serviceFolder.addChild(new AppLevelYamlNode(service.getUuid(), service.getAppId(), service.getName() + ".yaml",
            Service.class, servicePath, yamlGitSyncService));
        DirectoryPath serviceCommandPath = servicePath.clone().add("service_commands");
        FolderNode serviceCommandsFolder = new FolderNode(
            "Commands", ServiceCommand.class, serviceCommandPath, service.getAppId(), yamlGitSyncService);
        serviceFolder.addChild(serviceCommandsFolder);

        // ------------------- SERVICE COMMANDS SECTION -----------------------
        List<ServiceCommand> serviceCommands = service.getServiceCommands();

        // iterate over service commands
        for (ServiceCommand serviceCommand : serviceCommands) {
          serviceCommandsFolder.addChild(new ServiceLevelYamlNode(serviceCommand.getUuid(), serviceCommand.getAppId(),
              serviceCommand.getServiceId(), serviceCommand.getName() + ".yaml", ServiceCommand.class,
              serviceCommandPath.clone().add(serviceCommand.getUuid()), yamlGitSyncService));
        }
        // ------------------- END SERVICE COMMANDS SECTION -----------------------
      }
    }
  }

  private void doEnvironments(FolderNode theFolder, Application app, DirectoryPath directoryPath) {
    FolderNode environmentsFolder = new FolderNode(
        "Environments", Environment.class, directoryPath.add("environments"), app.getUuid(), yamlGitSyncService);
    theFolder.addChild(environmentsFolder);

    List<Environment> environments = environmentService.getEnvByApp(app.getAppId());

    if (environments != null) {
      // iterate over environments
      for (Environment environment : environments) {
        DirectoryPath envPath = directoryPath.clone();
        environmentsFolder.addChild(
            new AppLevelYamlNode(environment.getUuid(), environment.getAppId(), environment.getName() + ".yaml",
                Environment.class, envPath.add(environment.getUuid()), yamlGitSyncService));
      }
    }
  }

  private void doWorkflows(FolderNode theFolder, Application app, DirectoryPath directoryPath) {
    FolderNode workflowsFolder =
        new FolderNode("Workflows", Workflow.class, directoryPath.add("workflows"), app.getUuid(), yamlGitSyncService);
    theFolder.addChild(workflowsFolder);

    PageRequest<Workflow> pageRequest =
        aPageRequest().addFilter(aSearchFilter().withField("appId", Operator.EQ, app.getAppId()).build()).build();
    List<Workflow> workflows = workflowService.listWorkflows(pageRequest).getResponse();

    if (workflows != null) {
      // iterate over workflows
      for (Workflow workflow : workflows) {
        DirectoryPath workflowPath = directoryPath.clone();
        workflowsFolder.addChild(new AppLevelYamlNode(workflow.getUuid(), workflow.getAppId(),
            workflow.getName() + ".yaml", Workflow.class, workflowPath.add(workflow.getUuid()), yamlGitSyncService));
      }
    }
  }

  private void doPipelines(FolderNode theFolder, Application app, DirectoryPath directoryPath) {
    FolderNode pipelinesFolder =
        new FolderNode("Pipelines", Pipeline.class, directoryPath.add("pipelines"), app.getUuid(), yamlGitSyncService);
    theFolder.addChild(pipelinesFolder);

    PageRequest<Pipeline> pageRequest =
        aPageRequest().addFilter(aSearchFilter().withField("appId", Operator.EQ, app.getAppId()).build()).build();
    List<Pipeline> pipelines = pipelineService.listPipelines(pageRequest).getResponse();

    if (pipelines != null) {
      // iterate over pipelines
      for (Pipeline pipeline : pipelines) {
        DirectoryPath pipelinePath = directoryPath.clone();
        pipelinesFolder.addChild(new AppLevelYamlNode(pipeline.getUuid(), pipeline.getAppId(),
            pipeline.getName() + ".yaml", Pipeline.class, pipelinePath.add(pipeline.getUuid()), yamlGitSyncService));
      }
    }
  }

  private void doTriggers(FolderNode theFolder, Application app, DirectoryPath directoryPath) {
    FolderNode triggersFolder =
        new FolderNode("Triggers", Trigger.class, directoryPath.add("triggers"), app.getUuid(), yamlGitSyncService);
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

        triggersFolder.addChild(new AppLevelYamlNode(as.getUuid(), as.getAppId(), name + ".yaml", ArtifactStream.class,
            asPath.add(as.getUuid()), yamlGitSyncService));
      }
    }
  }

  private void doCloudProviders(FolderNode theFolder, String accountId, DirectoryPath directoryPath) {
    // create cloud providers (and physical data centers)
    FolderNode cloudProvidersFolder = new FolderNode(
        "Cloud Providers", SettingAttribute.class, directoryPath.add("cloud_providers"), yamlGitSyncService);
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
    FolderNode typeFolder = new FolderNode(nodeName, SettingAttribute.class,
        directoryPath.add(nodeName.toLowerCase().replace(' ', '_')), yamlGitSyncService);
    parentFolder.addChild(typeFolder);

    List<SettingAttribute> settingAttributes = settingsService.getGlobalSettingAttributesByType(accountId, type.name());

    if (settingAttributes != null) {
      // iterate over providers
      for (SettingAttribute settingAttribute : settingAttributes) {
        DirectoryPath cpPath = directoryPath.clone();
        typeFolder.addChild(new SettingAttributeYamlNode(settingAttribute.getUuid(),
            settingAttribute.getValue().getType(), settingAttribute.getName() + ".yaml", SettingAttribute.class,
            cpPath.add(settingAttribute.getUuid()), yamlGitSyncService));
      }
    }
  }

  private void doArtifactServers(FolderNode theFolder, String accountId, DirectoryPath directoryPath) {
    // create artifact servers
    FolderNode artifactServersFolder = new FolderNode(
        "Artifact Servers", SettingAttribute.class, directoryPath.add("artifact_servers"), yamlGitSyncService);
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
        parentFolder.addChild(new SettingAttributeYamlNode(settingAttribute.getUuid(),
            settingAttribute.getValue().getType(), settingAttribute.getName() + ".yaml", SettingAttribute.class,
            asPath.add(settingAttribute.getUuid()), yamlGitSyncService));
      }
    }
  }

  private void doCollaborationProviders(FolderNode theFolder, String accountId, DirectoryPath directoryPath) {
    // create collaboration providers
    FolderNode collaborationProvidersFolder = new FolderNode("Collaboration Providers", SettingAttribute.class,
        directoryPath.add("collaboration_providers"), yamlGitSyncService);
    theFolder.addChild(collaborationProvidersFolder);

    doCollaborationProviderType(accountId, collaborationProvidersFolder, "SMTP", SettingVariableTypes.SMTP,
        SettingAttribute.class, directoryPath.clone());
    doCollaborationProviderType(accountId, collaborationProvidersFolder, "Slack", SettingVariableTypes.SLACK,
        SettingAttribute.class, directoryPath.clone());
  }

  private void doCollaborationProviderType(String accountId, FolderNode parentFolder, String nodeName,
      SettingVariableTypes type, Class theClass, DirectoryPath directoryPath) {
    FolderNode typeFolder = new FolderNode(nodeName, SettingAttribute.class,
        directoryPath.add(nodeName.toLowerCase().replace(' ', '_')), yamlGitSyncService);
    parentFolder.addChild(typeFolder);

    List<SettingAttribute> settingAttributes = settingsService.getGlobalSettingAttributesByType(accountId, type.name());

    if (settingAttributes != null) {
      // iterate over providers
      for (SettingAttribute settingAttribute : settingAttributes) {
        DirectoryPath cpPath = directoryPath.clone();
        typeFolder.addChild(new SettingAttributeYamlNode(settingAttribute.getUuid(),
            settingAttribute.getValue().getType(), settingAttribute.getName() + ".yaml", SettingAttribute.class,
            cpPath.add(settingAttribute.getUuid()), yamlGitSyncService));
      }
    }
  }

  private void doLoadBalancers(FolderNode theFolder, String accountId, DirectoryPath directoryPath) {
    // create load balancers
    FolderNode loadBalancersFolder = new FolderNode(
        "Load Balancers", SettingAttribute.class, directoryPath.add("load_balancers"), yamlGitSyncService);
    theFolder.addChild(loadBalancersFolder);

    doLoadBalancerType(accountId, loadBalancersFolder, "Elastic Classic Load Balancers", SettingVariableTypes.ELB,
        SettingAttribute.class, directoryPath.clone());
  }

  private void doLoadBalancerType(String accountId, FolderNode parentFolder, String nodeName, SettingVariableTypes type,
      Class theClass, DirectoryPath directoryPath) {
    FolderNode typeFolder = new FolderNode(nodeName, SettingAttribute.class,
        directoryPath.add(nodeName.toLowerCase().replace(' ', '_')), yamlGitSyncService);
    parentFolder.addChild(typeFolder);

    List<SettingAttribute> settingAttributes = settingsService.getGlobalSettingAttributesByType(accountId, type.name());

    if (settingAttributes != null) {
      // iterate over providers
      for (SettingAttribute settingAttribute : settingAttributes) {
        DirectoryPath lbPath = directoryPath.clone();
        typeFolder.addChild(new SettingAttributeYamlNode(settingAttribute.getUuid(),
            settingAttribute.getValue().getType(), settingAttribute.getName() + ".yaml", SettingAttribute.class,
            lbPath.add(settingAttribute.getUuid()), yamlGitSyncService));
      }
    }
  }

  private void doVerificationProviders(FolderNode theFolder, String accountId, DirectoryPath directoryPath) {
    // create verification providers
    FolderNode verificationProvidersFolder = new FolderNode("Verification Providers", SettingAttribute.class,
        directoryPath.add("verification_providers"), yamlGitSyncService);
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
    FolderNode typeFolder = new FolderNode(nodeName, SettingAttribute.class,
        directoryPath.add(nodeName.toLowerCase().replace(' ', '_')), yamlGitSyncService);
    parentFolder.addChild(typeFolder);

    List<SettingAttribute> settingAttributes = settingsService.getGlobalSettingAttributesByType(accountId, type.name());

    if (settingAttributes != null) {
      // iterate over providers
      for (SettingAttribute settingAttribute : settingAttributes) {
        DirectoryPath vpPath = directoryPath.clone();
        typeFolder.addChild(new SettingAttributeYamlNode(settingAttribute.getUuid(),
            settingAttribute.getValue().getType(), settingAttribute.getName() + ".yaml", SettingAttribute.class,
            vpPath.add(settingAttribute.getUuid()), yamlGitSyncService));
      }
    }
  }
}
