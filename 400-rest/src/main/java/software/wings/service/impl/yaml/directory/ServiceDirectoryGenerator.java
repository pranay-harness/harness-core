package software.wings.service.impl.yaml.directory;

import static io.harness.beans.PageRequest.*;
import static io.harness.beans.SearchFilter.Operator.EQ;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import static software.wings.beans.ConfigFile.DEFAULT_TEMPLATE_ID;
import static software.wings.beans.Service.ServiceKeys;
import static software.wings.beans.appmanifest.AppManifestKind.AZURE_APP_SERVICE_MANIFEST;
import static software.wings.beans.appmanifest.AppManifestKind.K8S_MANIFEST;
import static software.wings.beans.yaml.YamlConstants.ARTIFACT_SOURCES_FOLDER;
import static software.wings.beans.yaml.YamlConstants.COMMANDS_FOLDER;
import static software.wings.beans.yaml.YamlConstants.CONFIG_FILES_FOLDER;
import static software.wings.beans.yaml.YamlConstants.DEPLOYMENT_SPECIFICATION_FOLDER;
import static software.wings.beans.yaml.YamlConstants.INDEX_YAML;
import static software.wings.beans.yaml.YamlConstants.MANIFEST_FILE_FOLDER;
import static software.wings.beans.yaml.YamlConstants.MANIFEST_FOLDER;
import static software.wings.beans.yaml.YamlConstants.MANIFEST_FOLDER_APP_SERVICE;
import static software.wings.beans.yaml.YamlConstants.SERVICES_FOLDER;
import static software.wings.beans.yaml.YamlConstants.YAML_EXTENSION;
import static software.wings.yaml.YamlVersion.Type;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.FeatureName;
import io.harness.beans.PageRequest;
import io.harness.beans.SortOrder;
import io.harness.ff.FeatureFlagService;

import software.wings.api.DeploymentType;
import software.wings.beans.Application;
import software.wings.beans.ConfigFile;
import software.wings.beans.LambdaSpecification;
import software.wings.beans.Service;
import software.wings.beans.appmanifest.AppManifestKind;
import software.wings.beans.appmanifest.ApplicationManifest;
import software.wings.beans.appmanifest.ManifestFile;
import software.wings.beans.appmanifest.StoreType;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.command.ServiceCommand;
import software.wings.beans.container.ContainerTask;
import software.wings.beans.container.EcsServiceSpecification;
import software.wings.beans.container.HelmChartSpecification;
import software.wings.beans.container.PcfServiceSpecification;
import software.wings.beans.container.UserDataSpecification;
import software.wings.beans.yaml.YamlConstants;
import software.wings.service.intfc.ApplicationManifestService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.ConfigService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.utils.ArtifactType;
import software.wings.utils.Utils;
import software.wings.yaml.directory.AppLevelYamlNode;
import software.wings.yaml.directory.DirectoryPath;
import software.wings.yaml.directory.FolderNode;
import software.wings.yaml.directory.ServiceLevelYamlNode;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;
import java.util.Set;
import org.jetbrains.annotations.NotNull;

@Singleton
@OwnedBy(HarnessTeam.CDP)
public class ServiceDirectoryGenerator {
  @Inject private ServiceResourceService serviceResourceService;
  @Inject private ArtifactStreamService artifactStreamService;
  @Inject private FeatureFlagService featureFlagService;
  @Inject private ConfigService configService;
  @Inject private ApplicationManifestService applicationManifestService;
  @Inject private ManifestFileFolderNodeGenerator manifestFileFolderNodeGenerator;

  public FolderNode doServices(
      Application app, DirectoryPath directoryPath, boolean applyPermissions, Set<String> allowedServices) {
    String accountId = app.getAccountId();
    FolderNode servicesFolder =
        new FolderNode(accountId, SERVICES_FOLDER, Service.class, directoryPath.add(SERVICES_FOLDER), app.getUuid());

    if (applyPermissions && isEmpty(allowedServices)) {
      return servicesFolder;
    }
    PageRequest<Service> pageRequest = PageRequestBuilder.<Service>aPageRequest()
                                           .addFilter(ServiceKeys.appId, EQ, app.getAppId())
                                           .addOrder(ServiceKeys.name, SortOrder.OrderType.ASC)
                                           .build();
    List<Service> services = serviceResourceService.list(pageRequest, false, false, false, null).getResponse();

    if (services != null) {
      // iterate over services
      for (Service service : services) {
        if (applyPermissions && !allowedServices.contains(service.getUuid())) {
          continue;
        }

        DirectoryPath servicePath = directoryPath.clone();
        String yamlFileName = INDEX_YAML;
        FolderNode serviceFolder = new FolderNode(
            accountId, service.getName(), Service.class, servicePath.add(service.getName()), service.getAppId());
        servicesFolder.addChild(serviceFolder);
        serviceFolder.addChild(new AppLevelYamlNode(accountId, service.getUuid(), service.getAppId(), yamlFileName,
            Service.class, servicePath.clone().add(yamlFileName), Type.SERVICE));

        // ------------------- SERVICE COMMANDS SECTION -----------------------

        if (!serviceResourceService.hasInternalCommands(service)) {
          DirectoryPath serviceCommandPath = servicePath.clone().add(COMMANDS_FOLDER);
          FolderNode serviceCommandsFolder =
              new FolderNode(accountId, COMMANDS_FOLDER, ServiceCommand.class, serviceCommandPath, service.getAppId());
          serviceFolder.addChild(serviceCommandsFolder);

          List<ServiceCommand> serviceCommands =
              serviceResourceService.getServiceCommands(service.getAppId(), service.getUuid());

          // iterate over service commands
          for (ServiceCommand serviceCommand : serviceCommands) {
            String commandYamlFileName = serviceCommand.getName() + YAML_EXTENSION;
            serviceCommandsFolder.addChild(new ServiceLevelYamlNode(accountId, serviceCommand.getUuid(),
                serviceCommand.getAppId(), serviceCommand.getServiceId(), commandYamlFileName, ServiceCommand.class,
                serviceCommandPath.clone().add(commandYamlFileName), Type.SERVICE_COMMAND));
          }
        }

        // ------------------- END SERVICE COMMANDS SECTION -----------------------

        // ------------------- DEPLOYMENT SPECIFICATION SECTION -----------------------

        DirectoryPath deploymentSpecsPath = servicePath.clone().add(DEPLOYMENT_SPECIFICATION_FOLDER);
        if (service.getArtifactType() == ArtifactType.DOCKER) {
          FolderNode deploymentSpecsFolder = new FolderNode(
              accountId, DEPLOYMENT_SPECIFICATION_FOLDER, ContainerTask.class, deploymentSpecsPath, service.getAppId());
          serviceFolder.addChild(deploymentSpecsFolder);

          ContainerTask kubernetesContainerTask = serviceResourceService.getContainerTaskByDeploymentType(
              service.getAppId(), service.getUuid(), DeploymentType.KUBERNETES.name());
          if (kubernetesContainerTask != null) {
            String kubernetesSpecFileName = YamlConstants.KUBERNETES_CONTAINER_TASK_YAML_FILE_NAME + YAML_EXTENSION;
            deploymentSpecsFolder.addChild(new ServiceLevelYamlNode(accountId, kubernetesContainerTask.getUuid(),
                kubernetesContainerTask.getAppId(), service.getUuid(), kubernetesSpecFileName, ContainerTask.class,
                deploymentSpecsPath.clone().add(kubernetesSpecFileName), Type.DEPLOYMENT_SPEC));
          }

          ContainerTask ecsContainerTask = serviceResourceService.getContainerTaskByDeploymentType(
              service.getAppId(), service.getUuid(), DeploymentType.ECS.name());
          if (ecsContainerTask != null) {
            String ecsSpecFileName = YamlConstants.ECS_CONTAINER_TASK_YAML_FILE_NAME + YAML_EXTENSION;
            deploymentSpecsFolder.addChild(new ServiceLevelYamlNode(accountId, ecsContainerTask.getUuid(),
                ecsContainerTask.getAppId(), service.getUuid(), ecsSpecFileName, ContainerTask.class,
                deploymentSpecsPath.clone().add(ecsSpecFileName), Type.DEPLOYMENT_SPEC));
          }

          // This is Service json spec for ECS
          EcsServiceSpecification serviceSpecification =
              serviceResourceService.getEcsServiceSpecification(service.getAppId(), service.getUuid());
          if (serviceSpecification != null) {
            String ecsServiceSpecFileName = YamlConstants.ECS_SERVICE_SPEC_YAML_FILE_NAME + YAML_EXTENSION;
            deploymentSpecsFolder.addChild(
                new ServiceLevelYamlNode(accountId, serviceSpecification.getUuid(), serviceSpecification.getAppId(),
                    service.getUuid(), ecsServiceSpecFileName, EcsServiceSpecification.class,
                    deploymentSpecsPath.clone().add(ecsServiceSpecFileName), Type.DEPLOYMENT_SPEC));
          }

          HelmChartSpecification helmChartSpecification =
              serviceResourceService.getHelmChartSpecification(service.getAppId(), service.getUuid());
          if (helmChartSpecification != null) {
            String helmChartFileName = YamlConstants.HELM_CHART_YAML_FILE_NAME + YAML_EXTENSION;
            deploymentSpecsFolder.addChild(new ServiceLevelYamlNode(accountId, helmChartSpecification.getUuid(),
                helmChartSpecification.getAppId(), service.getUuid(), helmChartFileName, HelmChartSpecification.class,
                deploymentSpecsPath.clone().add(helmChartFileName), Type.DEPLOYMENT_SPEC));
          }
        } else if (service.getArtifactType() == ArtifactType.AWS_LAMBDA) {
          FolderNode deploymentSpecsFolder = new FolderNode(accountId, DEPLOYMENT_SPECIFICATION_FOLDER,
              LambdaSpecification.class, deploymentSpecsPath, service.getAppId());
          serviceFolder.addChild(deploymentSpecsFolder);

          LambdaSpecification lambdaSpecification =
              serviceResourceService.getLambdaSpecification(service.getAppId(), service.getUuid());
          if (lambdaSpecification != null) {
            String lambdaSpecFileName = YamlConstants.LAMBDA_SPEC_YAML_FILE_NAME + YAML_EXTENSION;
            deploymentSpecsFolder.addChild(new ServiceLevelYamlNode(accountId, lambdaSpecification.getUuid(),
                lambdaSpecification.getAppId(), service.getUuid(), lambdaSpecFileName, LambdaSpecification.class,
                deploymentSpecsPath.clone().add(lambdaSpecFileName), Type.DEPLOYMENT_SPEC));
          }
        } else if (service.getArtifactType() == ArtifactType.AMI) {
          FolderNode deploymentSpecsFolder = new FolderNode(accountId, DEPLOYMENT_SPECIFICATION_FOLDER,
              UserDataSpecification.class, deploymentSpecsPath, service.getAppId());
          serviceFolder.addChild(deploymentSpecsFolder);

          UserDataSpecification userDataSpecification =
              serviceResourceService.getUserDataSpecification(service.getAppId(), service.getUuid());
          if (userDataSpecification != null) {
            String userDataSpecFileName = YamlConstants.USER_DATA_SPEC_YAML_FILE_NAME + YAML_EXTENSION;
            deploymentSpecsFolder.addChild(new ServiceLevelYamlNode(accountId, userDataSpecification.getUuid(),
                userDataSpecification.getAppId(), service.getUuid(), userDataSpecFileName, UserDataSpecification.class,
                deploymentSpecsPath.clone().add(userDataSpecFileName), Type.DEPLOYMENT_SPEC));
          }
        } else if (service.getArtifactType() == ArtifactType.PCF) {
          FolderNode deploymentSpecsFolder = new FolderNode(accountId, DEPLOYMENT_SPECIFICATION_FOLDER,
              PcfServiceSpecification.class, deploymentSpecsPath, service.getAppId());
          serviceFolder.addChild(deploymentSpecsFolder);

          PcfServiceSpecification pcfServiceSpecification =
              serviceResourceService.getPcfServiceSpecification(service.getAppId(), service.getUuid());
          if (pcfServiceSpecification != null) {
            String pcfServiceSpecificationFileName = YamlConstants.PCF_MANIFEST_YAML_FILE_NAME + YAML_EXTENSION;
            deploymentSpecsFolder.addChild(new ServiceLevelYamlNode(accountId, pcfServiceSpecification.getUuid(),
                pcfServiceSpecification.getAppId(), service.getUuid(), pcfServiceSpecificationFileName,
                PcfServiceSpecification.class, deploymentSpecsPath.clone().add(pcfServiceSpecificationFileName),
                Type.DEPLOYMENT_SPEC));
          }
        }

        // ------------------- END DEPLOYMENT SPECIFICATION SECTION -----------------------

        // ------------------- ARTIFACT STREAMS SECTION -----------------------
        if (!featureFlagService.isEnabled(FeatureName.ARTIFACT_STREAM_REFACTOR, accountId)) {
          DirectoryPath artifactStreamsPath = servicePath.clone().add(ARTIFACT_SOURCES_FOLDER);
          FolderNode artifactStreamsFolder = new FolderNode(
              accountId, ARTIFACT_SOURCES_FOLDER, ArtifactStream.class, artifactStreamsPath, service.getAppId());
          serviceFolder.addChild(artifactStreamsFolder);

          List<ArtifactStream> artifactStreamList =
              artifactStreamService.getArtifactStreamsForService(service.getAppId(), service.getUuid());
          artifactStreamList.forEach(artifactStream -> {
            String artifactYamlFileName = artifactStream.getName() + YAML_EXTENSION;
            artifactStreamsFolder.addChild(new ServiceLevelYamlNode(accountId, artifactStream.getUuid(),
                artifactStream.fetchAppId(), service.getUuid(), artifactYamlFileName, ArtifactStream.class,
                artifactStreamsPath.clone().add(artifactYamlFileName), Type.ARTIFACT_STREAM));
          });
        }

        // ------------------- END ARTIFACT STREAMS SECTION -----------------------

        // ------------------- CONFIG FILES SECTION -----------------------
        DirectoryPath configFilesPath = servicePath.clone().add(CONFIG_FILES_FOLDER);
        FolderNode configFilesFolder =
            new FolderNode(accountId, CONFIG_FILES_FOLDER, ConfigFile.class, configFilesPath, service.getAppId());
        serviceFolder.addChild(configFilesFolder);

        List<ConfigFile> configFiles =
            configService.getConfigFilesForEntity(service.getAppId(), DEFAULT_TEMPLATE_ID, service.getUuid());
        configFiles.forEach(configFile -> {
          String configFileName = Utils.normalize(configFile.getRelativeFilePath()) + YAML_EXTENSION;
          configFilesFolder.addChild(
              new ServiceLevelYamlNode(accountId, configFile.getUuid(), configFile.getAppId(), configFile.getEntityId(),
                  configFileName, ConfigFile.class, configFilesPath.clone().add(configFileName), Type.CONFIG_FILE));
        });

        // ------------------- END CONFIG FILES SECTION -----------------------

        // ------------------- APPLICATION MANIFEST FILES SECTION -----------------------

        FolderNode applicationManifestFolder =
            generateApplicationManifestNodeForService(accountId, service, servicePath);
        if (applicationManifestFolder != null) {
          serviceFolder.addChild(applicationManifestFolder);
        }
        // ------------------- END APPLICATION MANIFEST FILES SECTION -----------------------

        // ------------------- VALUES YAML OVERRIDE SECTION -----------------------

        FolderNode valuesFolder = generateValuesFolder(accountId, service, servicePath);
        if (valuesFolder != null) {
          serviceFolder.addChild(valuesFolder);
        }
        // ------------------- END VALUES YAML OVERRIDE SECTION -----------------------

        // ------------------- OC PARAMS OVERRIDE SECTION -----------------------

        FolderNode ocParamsFolder = generateOcParamsFolder(accountId, service, servicePath);
        if (ocParamsFolder != null) {
          serviceFolder.addChild(ocParamsFolder);
        }
        // ------------------- END OC PARAMS OVERRIDE SECTION -----------------------
      }
    }

    return servicesFolder;
  }

  private FolderNode generateValuesFolder(String accountId, Service service, DirectoryPath servicePath) {
    return generateKindBasedFolder(accountId, service, servicePath, AppManifestKind.VALUES);
  }

  private FolderNode generateOcParamsFolder(String accountId, Service service, DirectoryPath servicePath) {
    // Hiding OC Params Folder when not OC_TEMPLATES or CUSTOM_OPENSHIFT_TEMPLATE
    ApplicationManifest appManifest =
        applicationManifestService.getAppManifest(service.getAppId(), null, service.getUuid(), K8S_MANIFEST);
    if (appManifest == null) {
      return null;
    }
    if (appManifest.getStoreType() != StoreType.OC_TEMPLATES
        && appManifest.getStoreType() != StoreType.CUSTOM_OPENSHIFT_TEMPLATE) {
      return null;
    }
    return generateKindBasedFolder(accountId, service, servicePath, AppManifestKind.OC_PARAMS);
  }

  private FolderNode generateKindBasedFolder(
      String accountId, Service service, DirectoryPath servicePath, AppManifestKind appManifestKind) {
    ApplicationManifest appManifest =
        applicationManifestService.getByServiceId(service.getAppId(), service.getUuid(), appManifestKind);
    if (appManifest == null) {
      return null;
    }
    DirectoryPath folderPath = servicePath.clone().add(appManifestKind.getYamlFolderName());
    FolderNode folder = new FolderNode(
        accountId, appManifestKind.getYamlFolderName(), ApplicationManifest.class, folderPath, service.getAppId());
    folder.addChild(new ServiceLevelYamlNode(accountId, appManifest.getUuid(), service.getAppId(), service.getUuid(),
        INDEX_YAML, ApplicationManifest.class, folderPath.clone().add(INDEX_YAML), Type.APPLICATION_MANIFEST));
    if (appManifest.getStoreType() == StoreType.Local) {
      List<ManifestFile> manifestFiles =
          applicationManifestService.getManifestFilesByAppManifestId(service.getAppId(), appManifest.getUuid());
      if (isNotEmpty(manifestFiles)) {
        ManifestFile valuesFile = manifestFiles.get(0);
        folder.addChild(new ServiceLevelYamlNode(accountId, valuesFile.getUuid(), service.getAppId(), service.getUuid(),
            valuesFile.getFileName(), ManifestFile.class, folderPath.clone().add(valuesFile.getFileName()),
            Type.APPLICATION_MANIFEST_FILE));
      }
    }
    return folder;
  }

  @VisibleForTesting
  FolderNode generateApplicationManifestNodeForService(String accountId, Service service, DirectoryPath servicePath) {
    DirectoryPath applicationManifestPath = getApplicationManifestDirectoryPath(service, servicePath);

    String manifestFolderName = getApplicationManifestFolderName(service);

    FolderNode applicationManifestFolder = new FolderNode(
        accountId, manifestFolderName, ApplicationManifest.class, applicationManifestPath, service.getAppId());

    DirectoryPath manifestFilePath = applicationManifestPath.clone().add(MANIFEST_FILE_FOLDER);

    if (featureFlagService.isEnabled(FeatureName.HELM_CHART_AS_ARTIFACT, accountId)) {
      List<ApplicationManifest> applicationManifests =
          applicationManifestService.getManifestsByServiceId(service.getAppId(), service.getUuid(),
              isAzureAppServiceDeploymentType(service) ? AZURE_APP_SERVICE_MANIFEST : K8S_MANIFEST);
      if (isNotEmpty(applicationManifests)) {
        for (ApplicationManifest applicationManifest : applicationManifests) {
          if (applicationManifest != null) {
            String yamlFileName = getApplicationManifestYamlName(applicationManifest);
            applicationManifestFolder.addChild(new ServiceLevelYamlNode(accountId, applicationManifest.getUuid(),
                service.getAppId(), service.getUuid(), yamlFileName, ApplicationManifest.class,
                applicationManifestPath.clone().add(yamlFileName), Type.APPLICATION_MANIFEST));

            if (StoreType.Local == applicationManifest.getStoreType()) {
              FolderNode manifestFileFolder = manifestFileFolderNodeGenerator.generateManifestFileFolderNode(
                  accountId, service, applicationManifest, manifestFilePath);
              applicationManifestFolder.addChild(manifestFileFolder);
            }
          }
        }
      }

      if (isNotEmpty(applicationManifestFolder.getChildren())) {
        return applicationManifestFolder;
      }
    } else {
      ApplicationManifest applicationManifest = getApplicationManifestByService(service);
      if (applicationManifest != null) {
        String yamlFileName = getApplicationManifestYamlName(applicationManifest);
        applicationManifestFolder.addChild(new ServiceLevelYamlNode(accountId, applicationManifest.getUuid(),
            service.getAppId(), service.getUuid(), yamlFileName, ApplicationManifest.class,
            applicationManifestPath.clone().add(yamlFileName), Type.APPLICATION_MANIFEST));

        if (StoreType.Local == applicationManifest.getStoreType()) {
          FolderNode manifestFileFolder = manifestFileFolderNodeGenerator.generateManifestFileFolderNode(
              accountId, service, applicationManifest, manifestFilePath);
          applicationManifestFolder.addChild(manifestFileFolder);
        }
        return applicationManifestFolder;
      }
    }

    return null;
  }

  @NotNull
  private DirectoryPath getApplicationManifestDirectoryPath(Service service, DirectoryPath servicePath) {
    return isAzureAppServiceDeploymentType(service) ? servicePath.clone().add(MANIFEST_FOLDER_APP_SERVICE)
                                                    : servicePath.clone().add(MANIFEST_FOLDER);
  }

  @NotNull
  private String getApplicationManifestFolderName(Service service) {
    return isAzureAppServiceDeploymentType(service) ? MANIFEST_FOLDER_APP_SERVICE : MANIFEST_FOLDER;
  }

  private boolean isAzureAppServiceDeploymentType(Service service) {
    return service != null && DeploymentType.AZURE_WEBAPP == service.getDeploymentType();
  }

  @NotNull
  private String getApplicationManifestYamlName(ApplicationManifest applicationManifest) {
    return applicationManifest.getName() != null ? applicationManifest.getName() + YAML_EXTENSION : INDEX_YAML;
  }

  public ApplicationManifest getApplicationManifestByService(Service service) {
    AppManifestKind appManifestKind =
        isAzureAppServiceDeploymentType(service) ? AZURE_APP_SERVICE_MANIFEST : K8S_MANIFEST;
    return applicationManifestService.getByServiceId(service.getAppId(), service.getUuid(), appManifestKind);
  }
}
