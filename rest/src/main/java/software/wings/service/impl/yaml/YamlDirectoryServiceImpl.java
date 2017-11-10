package software.wings.service.impl.yaml;

import static software.wings.beans.FeatureName.GIT_SYNC;
import static software.wings.beans.SearchFilter.Builder.aSearchFilter;
import static software.wings.beans.yaml.YamlConstants.APPLICATIONS_FOLDER;
import static software.wings.beans.yaml.YamlConstants.ARTIFACT_SOURCES_FOLDER;
import static software.wings.beans.yaml.YamlConstants.CLOUD_PROVIDERS_FOLDER;
import static software.wings.beans.yaml.YamlConstants.COLLABORATION_PROVIDERS_FOLDER;
import static software.wings.beans.yaml.YamlConstants.COMMANDS_FOLDER;
import static software.wings.beans.yaml.YamlConstants.ENVIRONMENTS_FOLDER;
import static software.wings.beans.yaml.YamlConstants.INFRA_MAPPING_FOLDER;
import static software.wings.beans.yaml.YamlConstants.LOAD_BALANCERS_FOLDER;
import static software.wings.beans.yaml.YamlConstants.PATH_DELIMITER;
import static software.wings.beans.yaml.YamlConstants.PIPELINES_FOLDER;
import static software.wings.beans.yaml.YamlConstants.SERVICES_FOLDER;
import static software.wings.beans.yaml.YamlConstants.SETUP_FOLDER;
import static software.wings.beans.yaml.YamlConstants.VERIFICATION_PROVIDERS_FOLDER;
import static software.wings.beans.yaml.YamlConstants.WORKFLOWS_FOLDER;
import static software.wings.beans.yaml.YamlConstants.YAML_EXTENSION;
import static software.wings.dl.PageRequest.Builder.aPageRequest;

import org.hibernate.validator.constraints.NotEmpty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Account;
import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.ErrorCode;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.Pipeline;
import software.wings.beans.SearchFilter.Operator;
import software.wings.beans.Service;
import software.wings.beans.SettingAttribute;
import software.wings.beans.Workflow;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.command.ServiceCommand;
import software.wings.beans.yaml.Change.ChangeType;
import software.wings.beans.yaml.GitFileChange;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.exception.WingsException;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.PipelineService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.WorkflowService;
import software.wings.service.intfc.yaml.AppYamlResourceService;
import software.wings.service.intfc.yaml.YamlArtifactStreamService;
import software.wings.service.intfc.yaml.YamlDirectoryService;
import software.wings.service.intfc.yaml.YamlGitService;
import software.wings.service.intfc.yaml.YamlResourceService;
import software.wings.settings.SettingValue.SettingVariableTypes;
import software.wings.utils.Validator;
import software.wings.yaml.YamlVersion.Type;
import software.wings.yaml.directory.AppLevelYamlNode;
import software.wings.yaml.directory.DirectoryNode;
import software.wings.yaml.directory.DirectoryPath;
import software.wings.yaml.directory.EnvLevelYamlNode;
import software.wings.yaml.directory.FolderNode;
import software.wings.yaml.directory.ServiceLevelYamlNode;
import software.wings.yaml.directory.SettingAttributeYamlNode;
import software.wings.yaml.directory.YamlNode;
import software.wings.yaml.gitSync.YamlGitConfig;
import software.wings.yaml.gitSync.YamlGitConfig.SyncMode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import javax.inject.Inject;

public class YamlDirectoryServiceImpl implements YamlDirectoryService {
  private final Logger logger = LoggerFactory.getLogger(getClass());

  @Inject private AppService appService;
  @Inject private ServiceResourceService serviceResourceService;
  @Inject private EnvironmentService environmentService;
  @Inject private InfrastructureMappingService infraMappingService;
  @Inject private WorkflowService workflowService;
  @Inject private PipelineService pipelineService;
  // TODO - not sure what to use for this
  // @Inject private TriggerService triggerService;
  @Inject private SettingsService settingsService;
  @Inject private ArtifactStreamService artifactStreamService;
  @Inject private YamlArtifactStreamService yamlArtifactStreamService;
  @Inject private YamlGitService yamlGitSyncService;
  @Inject private AppYamlResourceService appYamlResourceService;
  @Inject private YamlResourceService yamlResourceService;
  @Inject private FeatureFlagService featureFlagService;

  @Override
  public YamlGitConfig weNeedToPushChanges(String accountId) {
    // check Git Sync feature flag
    if (!featureFlagService.isEnabled(GIT_SYNC, accountId)) {
      return null;
    }

    // for now, we are only checking the top level (full tree/directory)
    YamlGitConfig ygs = yamlGitSyncService.get(accountId, SETUP_FOLDER);

    if (ygs != null && ygs.isEnabled() && ygs.getSyncMode() != SyncMode.GIT_TO_HARNESS) {
      return ygs;
    }
    return null;
  }

  public List<GitFileChange> traverseDirectory(
      List<GitFileChange> gitFileChanges, String accountId, FolderNode fn, String path) {
    path = path + "/" + fn.getName();

    for (DirectoryNode dn : fn.getChildren()) {
      // logger.info(path + " :: " + dn.getName());

      if (dn instanceof YamlNode) {
        String entityId = ((YamlNode) dn).getUuid();
        String yaml = "";
        String appId = "";
        String settingVariableType = "";

        switch (dn.getShortClassName()) {
            //          case "Account":
            //            yaml = setupYamlResourceService.getSetup(accountId).getResource().getYaml();
            //            break;
          case "Application":
            yaml = appYamlResourceService.getApp(entityId).getResource().getYaml();
            break;
          case "Service":
            appId = ((AppLevelYamlNode) dn).getAppId();
            yaml = yamlResourceService.getService(appId, entityId).getResource().getYaml();
            break;
          case "Environment":
            appId = ((AppLevelYamlNode) dn).getAppId();
            yaml = yamlResourceService.getEnvironment(appId, entityId).getResource().getYaml();
            break;
          case "ServiceCommand":
            appId = ((ServiceLevelYamlNode) dn).getAppId();
            yaml = yamlResourceService.getServiceCommand(appId, entityId).getResource().getYaml();
            break;
          case "ArtifactStream":
            appId = ((AppLevelYamlNode) dn).getAppId();
            yaml = yamlArtifactStreamService.getArtifactStreamYamlString(appId, entityId);
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

        gitFileChanges.add(GitFileChange.Builder.aGitFileChange()
                               .withAccountId(accountId)
                               .withFilePath(dn.getName() == null ? dn.getName() : path + "/" + dn.getName())
                               .withFileContent(yaml)
                               .withChangeType(ChangeType.ADD)
                               .build());
      }

      if (dn instanceof FolderNode) {
        traverseDirectory(gitFileChanges, accountId, (FolderNode) dn, path);
      }
    }

    return gitFileChanges;
  }

  @Override
  public DirectoryNode getDirectory(@NotEmpty String accountId) {
    return getDirectory(accountId, accountId);
  }

  @Override
  public FolderNode getDirectory(@NotEmpty String accountId, String entityId) {
    DirectoryPath directoryPath = new DirectoryPath(SETUP_FOLDER);

    FolderNode configFolder = new FolderNode(accountId, SETUP_FOLDER, Account.class, directoryPath, yamlGitSyncService);
    long startTime = System.nanoTime();

    //--------------------------------------
    // parallelization using CompletionService
    final ExecutorService pool = Executors.newFixedThreadPool(6);
    final ExecutorCompletionService<FolderNode> completionService = new ExecutorCompletionService<>(pool);

    completionService.submit(() -> doApplications(accountId, directoryPath.clone()));

    completionService.submit(() -> doCloudProviders(accountId, directoryPath.clone()));

    completionService.submit(() -> doArtifactServers(accountId, directoryPath.clone()));

    completionService.submit(() -> doCollaborationProviders(accountId, directoryPath.clone()));

    completionService.submit(() -> doLoadBalancers(accountId, directoryPath.clone()));

    completionService.submit(() -> doVerificationProviders(accountId, directoryPath.clone()));

    // collect results to this map so we can rebuild the correct order
    Map<String, FolderNode> map = new HashMap<String, FolderNode>();
    // the number of items submitted to the completionService
    int count = 6;

    for (int i = 0; i < count; ++i) {
      try {
        final Future<FolderNode> future = completionService.take();

        try {
          final FolderNode fn = future.get();

          if (fn == null) {
            logger.info("********* failure in completionService");
          }

          map.put(fn.getName(), fn);
        } catch (InterruptedException e) {
          e.printStackTrace();
        } catch (ExecutionException e) {
          e.printStackTrace();
        }
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }

    // this controls the returned order
    configFolder.addChild(map.get(APPLICATIONS_FOLDER));
    configFolder.addChild(map.get(CLOUD_PROVIDERS_FOLDER));
    configFolder.addChild(map.get(ARTIFACT_SOURCES_FOLDER));
    configFolder.addChild(map.get(COLLABORATION_PROVIDERS_FOLDER));
    configFolder.addChild(map.get(LOAD_BALANCERS_FOLDER));
    configFolder.addChild(map.get(VERIFICATION_PROVIDERS_FOLDER));
    //--------------------------------------

    long endTime = System.nanoTime();
    double elapsedTime = (endTime - startTime) / 1e6;

    logger.info("********* ELAPSED_TIME: " + elapsedTime + " *********");

    return configFolder;
  }

  private FolderNode doApplications(String accountId, DirectoryPath directoryPath) {
    FolderNode applicationsFolder = new FolderNode(
        accountId, APPLICATIONS_FOLDER, Application.class, directoryPath.add(APPLICATIONS_FOLDER), yamlGitSyncService);

    List<Application> apps = appService.getAppsByAccountId(accountId);

    //--------------------------------------
    // parallelization using CompletionService (part 1)
    final ExecutorService pool = Executors.newFixedThreadPool(4);
    final ExecutorCompletionService<FolderNode> completionService = new ExecutorCompletionService<>(pool);
    //--------------------------------------

    // iterate over applications
    for (Application app : apps) {
      DirectoryPath appPath = directoryPath.clone();
      FolderNode appFolder = new FolderNode(
          accountId, app.getName(), Application.class, appPath.add(app.getName()), app.getUuid(), yamlGitSyncService);
      applicationsFolder.addChild(appFolder);
      String yamlFileName = app.getName() + ".yaml";
      appFolder.addChild(new YamlNode(accountId, app.getUuid(), yamlFileName, Application.class,
          appPath.clone().add(yamlFileName), yamlGitSyncService, Type.APP));

      //--------------------------------------
      // parallelization using CompletionService (part 2)
      completionService.submit(() -> doServices(app, appPath.clone()));

      completionService.submit(() -> doEnvironments(app, appPath.clone()));

      completionService.submit(() -> doWorkflows(app, appPath.clone()));

      completionService.submit(() -> doPipelines(app, appPath.clone()));

      //      completionService.submit(() -> doTriggers(app, appPath.clone()));

      // collect results to this map so we can rebuild the correct order
      Map<String, FolderNode> map = new HashMap<String, FolderNode>();
      // the number of items submitted to the completionService
      int count = 4;

      for (int i = 0; i < count; ++i) {
        try {
          final Future<FolderNode> future = completionService.take();

          try {
            final FolderNode fn = future.get();

            if (fn == null) {
              logger.info("********* failure in completionService");
            }

            map.put(fn.getName(), fn);
          } catch (InterruptedException e) {
            e.printStackTrace();
          } catch (ExecutionException e) {
            e.printStackTrace();
          }
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }

      // this controls the returned order
      appFolder.addChild(map.get(SERVICES_FOLDER));
      appFolder.addChild(map.get(ENVIRONMENTS_FOLDER));
      appFolder.addChild(map.get(WORKFLOWS_FOLDER));
      appFolder.addChild(map.get(PIPELINES_FOLDER));
      //      appFolder.addChild(map.get(TRIGGERS_FOLDER));
      //--------------------------------------
    }

    return applicationsFolder;
  }

  private FolderNode doServices(Application app, DirectoryPath directoryPath) {
    String accountId = app.getAccountId();
    FolderNode servicesFolder = new FolderNode(accountId, SERVICES_FOLDER, Service.class,
        directoryPath.add(SERVICES_FOLDER), app.getUuid(), yamlGitSyncService);

    List<Service> services = serviceResourceService.findServicesByApp(app.getAppId());

    if (services != null) {
      // iterate over services
      for (Service service : services) {
        DirectoryPath servicePath = directoryPath.clone();
        String yamlFileName = service.getName() + YAML_EXTENSION;
        FolderNode serviceFolder = new FolderNode(accountId, service.getName(), Service.class,
            servicePath.add(service.getName()), service.getAppId(), yamlGitSyncService);
        servicesFolder.addChild(serviceFolder);
        serviceFolder.addChild(new AppLevelYamlNode(accountId, service.getUuid(), service.getAppId(), yamlFileName,
            Service.class, servicePath.clone().add(yamlFileName), yamlGitSyncService, Type.SERVICE));

        // ------------------- SERVICE COMMANDS SECTION -----------------------

        DirectoryPath serviceCommandPath = servicePath.clone().add(COMMANDS_FOLDER);
        FolderNode serviceCommandsFolder = new FolderNode(accountId, COMMANDS_FOLDER, ServiceCommand.class,
            serviceCommandPath, service.getAppId(), yamlGitSyncService);
        serviceFolder.addChild(serviceCommandsFolder);

        List<ServiceCommand> serviceCommands = service.getServiceCommands();

        // iterate over service commands
        for (ServiceCommand serviceCommand : serviceCommands) {
          String commandYamlFileName = serviceCommand.getName() + YAML_EXTENSION;
          serviceCommandsFolder.addChild(new ServiceLevelYamlNode(accountId, serviceCommand.getUuid(),
              serviceCommand.getAppId(), serviceCommand.getServiceId(), commandYamlFileName, ServiceCommand.class,
              serviceCommandPath.clone().add(commandYamlFileName), yamlGitSyncService, Type.SERVICE_COMMAND));
        }

        // ------------------- END SERVICE COMMANDS SECTION -----------------------

        // ------------------- ARTIFACT STREAMS SECTION -----------------------
        DirectoryPath artifactStreamsPath = servicePath.clone().add(ARTIFACT_SOURCES_FOLDER);
        FolderNode artifactStreamsFolder = new FolderNode(accountId, ARTIFACT_SOURCES_FOLDER, ArtifactStream.class,
            artifactStreamsPath, service.getAppId(), yamlGitSyncService);
        serviceFolder.addChild(artifactStreamsFolder);

        List<ArtifactStream> artifactStreamList =
            artifactStreamService.getArtifactStreamsForService(service.getAppId(), service.getUuid());
        artifactStreamList.stream().forEach(artifactStream -> {
          String artifactYamlFileName = artifactStream.getSourceName() + YAML_EXTENSION;
          artifactStreamsFolder.addChild(new ServiceLevelYamlNode(accountId, artifactStream.getUuid(),
              artifactStream.getAppId(), service.getUuid(), artifactYamlFileName, ArtifactStream.class,
              artifactStreamsPath.clone().add(artifactYamlFileName), yamlGitSyncService, Type.ARTIFACT_STREAM));
        });

        // ------------------- END ARTIFACT STREAMS SECTION -----------------------
      }
    }

    return servicesFolder;
  }

  private FolderNode doEnvironments(Application app, DirectoryPath directoryPath) {
    String accountId = app.getAccountId();
    FolderNode environmentsFolder = new FolderNode(accountId, ENVIRONMENTS_FOLDER, Environment.class,
        directoryPath.add(ENVIRONMENTS_FOLDER), app.getUuid(), yamlGitSyncService);

    List<Environment> environments = environmentService.getEnvByApp(app.getAppId());

    if (environments != null) {
      // iterate over environments
      for (Environment environment : environments) {
        DirectoryPath envPath = directoryPath.clone();

        String yamlFileName = environment.getName() + YAML_EXTENSION;
        FolderNode envFolder = new FolderNode(accountId, environment.getName(), Environment.class,
            envPath.add(environment.getName()), environment.getAppId(), yamlGitSyncService);
        environmentsFolder.addChild(envFolder);
        envFolder.addChild(new AppLevelYamlNode(accountId, environment.getUuid(), environment.getAppId(), yamlFileName,
            Environment.class, envPath.clone().add(yamlFileName), yamlGitSyncService, Type.ENVIRONMENT));

        // ------------------- INFRA MAPPING SECTION -----------------------

        DirectoryPath infraMappingPath = envPath.clone().add(INFRA_MAPPING_FOLDER);
        FolderNode infraMappingsFolder = new FolderNode(accountId, INFRA_MAPPING_FOLDER, InfrastructureMapping.class,
            infraMappingPath, environment.getAppId(), yamlGitSyncService);
        envFolder.addChild(infraMappingsFolder);

        PageRequest<InfrastructureMapping> pageRequest = PageRequest.Builder.aPageRequest()
                                                             .addFilter("appId", Operator.EQ, environment.getAppId())
                                                             .addFilter("envId", Operator.EQ, environment.getUuid())
                                                             .build();
        PageResponse<InfrastructureMapping> infraMappingList = infraMappingService.list(pageRequest);

        // iterate over service commands
        infraMappingList.stream().forEach(infraMapping -> {
          String infraMappingYamlFileName = infraMapping.getName() + YAML_EXTENSION;
          infraMappingsFolder.addChild(new EnvLevelYamlNode(accountId, infraMapping.getUuid(), infraMapping.getAppId(),
              infraMapping.getEnvId(), infraMappingYamlFileName, InfrastructureMapping.class,
              infraMappingPath.clone().add(infraMappingYamlFileName), yamlGitSyncService, Type.INFRA_MAPPING));
        });

        //        DirectoryPath envPath = directoryPath.clone();
        //        FolderNode envFolder =
        //            new AppLevelYamlNode(accountId, environment.getUuid(), environment.getAppId(),
        //            environment.getName() + ".yaml", Environment.class,
        //                envPath.add(environment.getName()), yamlGitSyncService, Type.ENVIRONMENT);
        //        environmentsFolder.addChild(envFolder);
        //
        //        // ------------------- INFRA MAPPING SECTION -----------------------
        //
        //        DirectoryPath infraMappingPath = envPath.clone().add(INFRA_MAPPING_FOLDER);
        //        FolderNode infraMappingFolder =
        //            new FolderNode(accountId, INFRA_MAPPING_FOLDER, InfrastructureMapping.class, infraMappingPath,
        //            environment.getAppId(), yamlGitSyncService);
        //        envFolder.addChild(infraMappingFolder);
        //
        //        PageRequest<InfrastructureMapping> pageRequest = PageRequest.Builder.aPageRequest().addFilter("appId",
        //        Operator.EQ, environment.getAppId())
        //            .addFilter("envId", Operator.EQ, environment.getUuid()).build();
        //        PageResponse<InfrastructureMapping> infraMappingList = infraMappingService.list(pageRequest);
        //
        //        // iterate over infra mappings
        //        infraMappingList.stream().forEach(infraMapping -> {
        //          String yamlFileName = infraMapping.getName() + YAML_EXTENSION;
        //          infraMappingFolder.addChild(new EnvLevelYamlNode(accountId, infraMapping.getUuid(),
        //          infraMapping.getAppId(), infraMapping.getEnvId(),
        //              infraMapping.getName(), ServiceCommand.class, infraMappingPath.clone().add(yamlFileName),
        //              yamlGitSyncService, Type.INFRA_MAPPING));
        //        });
        //
        //        // ------------------- END INFRA MAPPING SECTION -----------------------
      }
    }

    return environmentsFolder;
  }

  private FolderNode doWorkflows(Application app, DirectoryPath directoryPath) {
    String accountId = app.getAccountId();
    FolderNode workflowsFolder = new FolderNode(accountId, WORKFLOWS_FOLDER, Workflow.class,
        directoryPath.add(WORKFLOWS_FOLDER), app.getUuid(), yamlGitSyncService);

    PageRequest<Workflow> pageRequest =
        aPageRequest().addFilter(aSearchFilter().withField("appId", Operator.EQ, app.getAppId()).build()).build();
    List<Workflow> workflows = workflowService.listWorkflows(pageRequest).getResponse();

    if (workflows != null) {
      // iterate over workflows
      for (Workflow workflow : workflows) {
        DirectoryPath workflowPath = directoryPath.clone();
        String workflowYamlFileName = workflow.getName() + YAML_EXTENSION;
        workflowsFolder.addChild(
            new AppLevelYamlNode(accountId, workflow.getUuid(), workflow.getAppId(), workflowYamlFileName,
                Workflow.class, workflowPath.add(workflowYamlFileName), yamlGitSyncService, Type.WORKFLOW));
      }
    }

    return workflowsFolder;
  }

  private FolderNode doPipelines(Application app, DirectoryPath directoryPath) {
    String accountId = app.getAccountId();
    FolderNode pipelinesFolder = new FolderNode(accountId, PIPELINES_FOLDER, Pipeline.class,
        directoryPath.add(PIPELINES_FOLDER), app.getUuid(), yamlGitSyncService);

    PageRequest<Pipeline> pageRequest =
        aPageRequest().addFilter(aSearchFilter().withField("appId", Operator.EQ, app.getAppId()).build()).build();
    List<Pipeline> pipelines = pipelineService.listPipelines(pageRequest).getResponse();

    if (pipelines != null) {
      // iterate over pipelines
      for (Pipeline pipeline : pipelines) {
        DirectoryPath pipelinePath = directoryPath.clone();
        String pipelineYamlFileName = pipeline.getName() + YAML_EXTENSION;
        pipelinesFolder.addChild(
            new AppLevelYamlNode(accountId, pipeline.getUuid(), pipeline.getAppId(), pipelineYamlFileName,
                Pipeline.class, pipelinePath.add(pipelineYamlFileName), yamlGitSyncService, Type.PIPELINE));
      }
    }

    return pipelinesFolder;
  }

  private FolderNode doCloudProviders(String accountId, DirectoryPath directoryPath) {
    // create cloud providers (and physical data centers)
    FolderNode cloudProvidersFolder = new FolderNode(accountId, CLOUD_PROVIDERS_FOLDER, SettingAttribute.class,
        directoryPath.add("cloud_providers"), yamlGitSyncService);

    // TODO - should these use AwsConfig GcpConfig, etc. instead?
    doCloudProviderType(accountId, cloudProvidersFolder, SettingVariableTypes.AWS, directoryPath.clone());
    doCloudProviderType(accountId, cloudProvidersFolder, SettingVariableTypes.GCP, directoryPath.clone());
    doCloudProviderType(
        accountId, cloudProvidersFolder, SettingVariableTypes.PHYSICAL_DATA_CENTER, directoryPath.clone());

    return cloudProvidersFolder;
  }

  private void doCloudProviderType(
      String accountId, FolderNode parentFolder, SettingVariableTypes type, DirectoryPath directoryPath) {
    List<SettingAttribute> settingAttributes = settingsService.getGlobalSettingAttributesByType(accountId, type.name());

    if (settingAttributes != null) {
      // iterate over providers
      for (SettingAttribute settingAttribute : settingAttributes) {
        DirectoryPath cpPath = directoryPath.clone();
        parentFolder.addChild(new SettingAttributeYamlNode(accountId, settingAttribute.getUuid(),
            settingAttribute.getValue().getType(), settingAttribute.getName() + ".yaml", SettingAttribute.class,
            cpPath.add(settingAttribute.getName()), yamlGitSyncService));
      }
    }
  }

  private FolderNode doArtifactServers(String accountId, DirectoryPath directoryPath) {
    // create artifact servers
    FolderNode artifactServersFolder = new FolderNode(accountId, ARTIFACT_SOURCES_FOLDER, SettingAttribute.class,
        directoryPath.add("artifact_servers"), yamlGitSyncService);

    doArtifactServerType(accountId, artifactServersFolder, SettingVariableTypes.JENKINS, directoryPath.clone());
    doArtifactServerType(accountId, artifactServersFolder, SettingVariableTypes.BAMBOO, directoryPath.clone());
    doArtifactServerType(accountId, artifactServersFolder, SettingVariableTypes.DOCKER, directoryPath.clone());
    doArtifactServerType(accountId, artifactServersFolder, SettingVariableTypes.NEXUS, directoryPath.clone());
    doArtifactServerType(accountId, artifactServersFolder, SettingVariableTypes.ARTIFACTORY, directoryPath.clone());

    return artifactServersFolder;
  }

  private void doArtifactServerType(
      String accountId, FolderNode parentFolder, SettingVariableTypes type, DirectoryPath directoryPath) {
    List<SettingAttribute> settingAttributes = settingsService.getGlobalSettingAttributesByType(accountId, type.name());

    if (settingAttributes != null) {
      // iterate over providers
      for (SettingAttribute settingAttribute : settingAttributes) {
        DirectoryPath asPath = directoryPath.clone();
        parentFolder.addChild(new SettingAttributeYamlNode(accountId, settingAttribute.getUuid(),
            settingAttribute.getValue().getType(), settingAttribute.getName() + ".yaml", SettingAttribute.class,
            asPath.add(settingAttribute.getName()), yamlGitSyncService));
      }
    }
  }

  private FolderNode doCollaborationProviders(String accountId, DirectoryPath directoryPath) {
    // create collaboration providers
    FolderNode collaborationProvidersFolder = new FolderNode(accountId, COLLABORATION_PROVIDERS_FOLDER,
        SettingAttribute.class, directoryPath.add("collaboration_providers"), yamlGitSyncService);

    doCollaborationProviderType(
        accountId, collaborationProvidersFolder, SettingVariableTypes.SMTP, directoryPath.clone());
    doCollaborationProviderType(
        accountId, collaborationProvidersFolder, SettingVariableTypes.SLACK, directoryPath.clone());

    return collaborationProvidersFolder;
  }

  private void doCollaborationProviderType(
      String accountId, FolderNode parentFolder, SettingVariableTypes type, DirectoryPath directoryPath) {
    List<SettingAttribute> settingAttributes = settingsService.getGlobalSettingAttributesByType(accountId, type.name());

    if (settingAttributes != null) {
      // iterate over providers
      for (SettingAttribute settingAttribute : settingAttributes) {
        DirectoryPath cpPath = directoryPath.clone();
        parentFolder.addChild(new SettingAttributeYamlNode(accountId, settingAttribute.getUuid(),
            settingAttribute.getValue().getType(), settingAttribute.getName() + ".yaml", SettingAttribute.class,
            cpPath.add(settingAttribute.getName()), yamlGitSyncService));
      }
    }
  }

  private FolderNode doLoadBalancers(String accountId, DirectoryPath directoryPath) {
    // create load balancers
    FolderNode loadBalancersFolder = new FolderNode(accountId, LOAD_BALANCERS_FOLDER, SettingAttribute.class,
        directoryPath.add("load_balancers"), yamlGitSyncService);

    doLoadBalancerType(accountId, loadBalancersFolder, SettingVariableTypes.ELB, directoryPath.clone());

    return loadBalancersFolder;
  }

  private void doLoadBalancerType(
      String accountId, FolderNode parentFolder, SettingVariableTypes type, DirectoryPath directoryPath) {
    List<SettingAttribute> settingAttributes = settingsService.getGlobalSettingAttributesByType(accountId, type.name());

    if (settingAttributes != null) {
      // iterate over providers
      for (SettingAttribute settingAttribute : settingAttributes) {
        DirectoryPath lbPath = directoryPath.clone();
        parentFolder.addChild(new SettingAttributeYamlNode(accountId, settingAttribute.getUuid(),
            settingAttribute.getValue().getType(), settingAttribute.getName() + ".yaml", SettingAttribute.class,
            lbPath.add(settingAttribute.getName()), yamlGitSyncService));
      }
    }
  }

  private FolderNode doVerificationProviders(String accountId, DirectoryPath directoryPath) {
    // create verification providers
    FolderNode verificationProvidersFolder = new FolderNode(accountId, VERIFICATION_PROVIDERS_FOLDER,
        SettingAttribute.class, directoryPath.add("verification_providers"), yamlGitSyncService);

    doVerificationProviderType(
        accountId, verificationProvidersFolder, SettingVariableTypes.JENKINS, directoryPath.clone());
    doVerificationProviderType(
        accountId, verificationProvidersFolder, SettingVariableTypes.APP_DYNAMICS, directoryPath.clone());
    doVerificationProviderType(
        accountId, verificationProvidersFolder, SettingVariableTypes.SPLUNK, directoryPath.clone());
    doVerificationProviderType(accountId, verificationProvidersFolder, SettingVariableTypes.ELK, directoryPath.clone());
    doVerificationProviderType(
        accountId, verificationProvidersFolder, SettingVariableTypes.LOGZ, directoryPath.clone());

    return verificationProvidersFolder;
  }

  private void doVerificationProviderType(
      String accountId, FolderNode parentFolder, SettingVariableTypes type, DirectoryPath directoryPath) {
    List<SettingAttribute> settingAttributes = settingsService.getGlobalSettingAttributesByType(accountId, type.name());

    if (settingAttributes != null) {
      // iterate over providers
      for (SettingAttribute settingAttribute : settingAttributes) {
        DirectoryPath vpPath = directoryPath.clone();
        parentFolder.addChild(new SettingAttributeYamlNode(accountId, settingAttribute.getUuid(),
            settingAttribute.getValue().getType(), settingAttribute.getName() + ".yaml", SettingAttribute.class,
            vpPath.add(settingAttribute.getName()), yamlGitSyncService));
      }
    }
  }

  private String getRootPathTop() {
    return SETUP_FOLDER;
  }

  @Override
  public String getRootPathByApp(Application app) {
    return getRootPathTop() + PATH_DELIMITER + APPLICATIONS_FOLDER + PATH_DELIMITER + app.getName();
  }

  @Override
  public String getRootPathByService(Service service) {
    Application app = appService.get(service.getAppId());
    return getRootPathByService(service, getRootPathByApp(app));
  }

  @Override
  public String getRootPathByService(Service service, String applicationPath) {
    return applicationPath + PATH_DELIMITER + SERVICES_FOLDER + PATH_DELIMITER + service.getName();
  }

  @Override
  public String getRootPathByServiceCommand(Service service, ServiceCommand serviceCommand) {
    return getRootPathByService(service) + PATH_DELIMITER + COMMANDS_FOLDER;
  }

  @Override
  public String getRootPathByEnvironment(Environment environment) {
    Application app = appService.get(environment.getAppId());
    return getRootPathByEnvironment(environment, getRootPathByApp(app));
  }

  @Override
  public String getRootPathByEnvironment(Environment environment, String appPath) {
    return appPath + PATH_DELIMITER + ENVIRONMENTS_FOLDER;
  }

  @Override
  public String getRootPathByInfraMapping(InfrastructureMapping infraMapping) {
    Environment environment = environmentService.get(infraMapping.getAppId(), infraMapping.getEnvId(), false);
    Validator.notNullCheck("Environment is null", environment);
    String rootPathByEnvironment = getRootPathByEnvironment(environment);
    return rootPathByEnvironment + PATH_DELIMITER + environment.getName() + PATH_DELIMITER + INFRA_MAPPING_FOLDER;
  }

  @Override
  public String getRootPathByPipeline(Pipeline pipeline) {
    Application app = appService.get(pipeline.getAppId());
    return getRootPathByApp(app) + PATH_DELIMITER + PIPELINES_FOLDER;
  }

  @Override
  public String getRootPathByWorkflow(Workflow workflow) {
    Application app = appService.get(workflow.getAppId());
    return getRootPathByApp(app) + PATH_DELIMITER + WORKFLOWS_FOLDER;
  }

  @Override
  public String getRootPathByArtifactStream(ArtifactStream artifactStream) {
    Service service = serviceResourceService.get(artifactStream.getAppId(), artifactStream.getServiceId());
    return getRootPathByService(service) + PATH_DELIMITER + ARTIFACT_SOURCES_FOLDER;
  }

  @Override
  public String getRootPathBySettingAttribute(
      SettingAttribute settingAttribute, SettingVariableTypes settingVariableType) {
    StringBuilder sb = new StringBuilder();
    sb.append(getRootPathTop() + PATH_DELIMITER);

    switch (settingVariableType) {
      // cloud providers
      case AWS:
      case GCP:
      case PHYSICAL_DATA_CENTER:
        sb.append(CLOUD_PROVIDERS_FOLDER);
        break;

      // artifact servers - these don't have separate folders
      case JENKINS:
      case BAMBOO:
      case DOCKER:
      case NEXUS:
      case ARTIFACTORY:
        sb.append(ARTIFACT_SOURCES_FOLDER);
        break;

      // collaboration providers
      case SMTP:
      case SLACK:
        sb.append(COLLABORATION_PROVIDERS_FOLDER);
        break;

      // load balancers
      case ELB:
        sb.append(LOAD_BALANCERS_FOLDER);
        break;

      // verification providers
      // JENKINS is also a (logical) part of this group
      case APP_DYNAMICS:
      case SPLUNK:
      case ELK:
      case LOGZ:
        sb.append(VERIFICATION_PROVIDERS_FOLDER);
        break;
      case HOST_CONNECTION_ATTRIBUTES:
        break;
      default:
        throw new WingsException(ErrorCode.GENERAL_YAML_ERROR);
    }
    return sb.toString();
  }

  @Override
  public String getRootPathBySettingAttribute(SettingAttribute settingAttribute) {
    return getRootPathBySettingAttribute(settingAttribute, settingAttribute.getValue().getSettingType());
  }
}
