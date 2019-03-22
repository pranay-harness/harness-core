package io.harness.generator;

import static io.harness.generator.SettingGenerator.Settings.AWS_TEST_CLOUD_PROVIDER;
import static io.harness.generator.SettingGenerator.Settings.AZURE_TEST_CLOUD_PROVIDER;
import static io.harness.generator.SettingGenerator.Settings.DEV_TEST_CONNECTOR;
import static io.harness.generator.SettingGenerator.Settings.HARNESS_GCP_EXPLORATION;
import static io.harness.generator.SettingGenerator.Settings.PHYSICAL_DATA_CENTER;
import static io.harness.generator.SettingGenerator.Settings.WINRM_TEST_CONNECTOR;
import static io.harness.govern.Switch.unhandled;
import static java.util.Arrays.asList;
import static software.wings.beans.AwsInfrastructureMapping.Builder.anAwsInfrastructureMapping;
import static software.wings.beans.EcsInfrastructureMapping.Builder.anEcsInfrastructureMapping;
import static software.wings.beans.GcpKubernetesInfrastructureMapping.Builder.aGcpKubernetesInfrastructureMapping;
import static software.wings.beans.InfrastructureMappingType.AWS_ECS;
import static software.wings.beans.InfrastructureMappingType.AWS_SSH;
import static software.wings.beans.InfrastructureMappingType.AZURE_INFRA;
import static software.wings.beans.InfrastructureMappingType.GCP_KUBERNETES;
import static software.wings.beans.InfrastructureMappingType.PHYSICAL_DATA_CENTER_WINRM;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.mongodb.DuplicateKeyException;
import io.github.benas.randombeans.api.EnhancedRandom;
import io.harness.exception.WingsException;
import io.harness.generator.EnvironmentGenerator.Environments;
import io.harness.generator.InfrastructureProvisionerGenerator.InfrastructureProvisioners;
import io.harness.generator.OwnerManager.Owners;
import io.harness.generator.ServiceGenerator.Services;
import software.wings.api.DeploymentType;
import software.wings.beans.Account;
import software.wings.beans.Application;
import software.wings.beans.AwsInfrastructureMapping;
import software.wings.beans.AwsInstanceFilter;
import software.wings.beans.AwsInstanceFilter.Tag;
import software.wings.beans.AzureInfrastructureMapping;
import software.wings.beans.AzureInfrastructureMapping.Builder;
import software.wings.beans.EcsInfrastructureMapping;
import software.wings.beans.Environment;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.InfrastructureMappingType;
import software.wings.beans.InfrastructureProvisioner;
import software.wings.beans.PhysicalInfrastructureMappingWinRm;
import software.wings.beans.Service;
import software.wings.beans.ServiceTemplate;
import software.wings.beans.SettingAttribute;
import software.wings.beans.appmanifest.AppManifestKind;
import software.wings.beans.appmanifest.ApplicationManifest;
import software.wings.beans.appmanifest.ManifestFile;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ApplicationManifestService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.ServiceTemplateService;
import software.wings.settings.SettingValue.SettingVariableTypes;

import java.util.List;

@Singleton
public class InfrastructureMappingGenerator {
  @Inject private OwnerManager ownerManager;
  @Inject private EnvironmentGenerator environmentGenerator;
  @Inject private ServiceGenerator serviceGenerator;
  @Inject private SettingGenerator settingGenerator;
  @Inject private InfrastructureProvisionerGenerator infrastructureProvisionerGenerator;

  @Inject private AppService appService;
  @Inject private InfrastructureMappingService infrastructureMappingService;
  @Inject private EnvironmentService environmentService;
  @Inject private ApplicationManifestService applicationManifestService;
  @Inject private ServiceTemplateService serviceTemplateService;
  @Inject private WingsPersistence wingsPersistence;

  private static final String AZURE_SUBSCRIPTION_ID = "20d6a917-99fa-4b1b-9b2e-a3d624e9dcf0";
  private static final String AZURE_RESOURCE_GROUP = "rathna-rg";
  private static final String AZURE_DEPLOY_HOST = "vm1-test-rathna.centralus.cloudapp.azure.com";
  public enum InfrastructureMappings {
    AWS_SSH_TEST,
    TERRAFORM_AWS_SSH_TEST,
    AWS_SSH_FUNCTIONAL_TEST,
    PHYSICAL_WINRM_TEST,
    AZURE_WINRM_TEST,
    ECS_EC2_TEST,
    ECS_FARGATE_TEST,
    K8S_ROLLING_TEST,
    K8S_CANARY_TEST,
    K8S_BLUE_GREEN_TEST
  }

  public InfrastructureMapping ensurePredefined(
      Randomizer.Seed seed, Owners owners, InfrastructureMappings predefined) {
    switch (predefined) {
      case AWS_SSH_TEST:
        return ensureAwsSshTest(seed, owners);
      case AWS_SSH_FUNCTIONAL_TEST:
        return ensureAwsSshFunctionalTest(seed, owners);
      case TERRAFORM_AWS_SSH_TEST:
        return ensureTerraformAwsSshTest(seed, owners);
      case PHYSICAL_WINRM_TEST:
        return ensurePhysicalWinRMTest(seed, owners);
      case AZURE_WINRM_TEST:
        return ensureAzureWinRMTest(seed, owners);
      case ECS_EC2_TEST:
        return ensureEcsEc2Test(seed, owners);
      case K8S_ROLLING_TEST:
        return ensureK8sTest(seed, owners, "fn-test-rolling");
      case K8S_BLUE_GREEN_TEST:
        return ensureK8sTest(seed, owners, "fn-test-bg");
      case K8S_CANARY_TEST:
        return ensureK8sTest(seed, owners, "fn-test-canary");
      default:
        unhandled(predefined);
    }
    return null;
  }

  private InfrastructureMapping ensureAzureWinRMTest(Randomizer.Seed seed, Owners owners) {
    Environment environment = owners.obtainEnvironment();
    if (environment == null) {
      environment = environmentGenerator.ensurePredefined(seed, owners, Environments.GENERIC_TEST);
      owners.add(environment);
    }

    Service service = owners.obtainService();
    if (service == null) {
      service = serviceGenerator.ensurePredefined(seed, owners, Services.WINDOWS_TEST);
      owners.add(service);
    }

    final SettingAttribute azureCloudProvider =
        settingGenerator.ensurePredefined(seed, owners, AZURE_TEST_CLOUD_PROVIDER);
    final SettingAttribute winRmSettingAttribute =
        settingGenerator.ensurePredefined(seed, owners, WINRM_TEST_CONNECTOR);

    return ensureInfrastructureMapping(seed, owners,
        AzureInfrastructureMapping.Builder.anAzureInfrastructureMapping()
            .withName("Windows non prod - winrm azure workflow test")
            .withAutoPopulate(false)
            .withInfraMappingType(AZURE_INFRA.name())
            .withDeploymentType(DeploymentType.WINRM.name())
            .withComputeProviderType(SettingVariableTypes.AZURE.name())
            .withComputeProviderSettingId(azureCloudProvider.getUuid())
            .withWinRmConnectionAttributes(winRmSettingAttribute.getUuid())
            .withSubscriptionId(AZURE_SUBSCRIPTION_ID)
            .withResourceGroup(AZURE_RESOURCE_GROUP)
            .withUsePublicDns(true)
            .build());
  }

  private InfrastructureMapping ensureEcsEc2Test(Randomizer.Seed seed, Owners owners) {
    Environment environment = owners.obtainEnvironment();
    if (environment == null) {
      environment = environmentGenerator.ensurePredefined(seed, owners, Environments.GENERIC_TEST);
      owners.add(environment);
    }

    Service service = owners.obtainService();
    if (service == null) {
      service = serviceGenerator.ensurePredefined(seed, owners, Services.ECS_TEST);
      owners.add(service);
    }

    final SettingAttribute ecsCloudProvider = settingGenerator.ensurePredefined(seed, owners, AWS_TEST_CLOUD_PROVIDER);

    InfrastructureMapping newInfrastructureMapping =
        anEcsInfrastructureMapping()
            .withName("Ecs Ec2 type deployment Functional test" + System.currentTimeMillis())
            .withAutoPopulate(false)
            .withInfraMappingType(AWS_ECS.name())
            .withDeploymentType(DeploymentType.ECS.name())
            .withComputeProviderType(SettingVariableTypes.AWS.name())
            .withComputeProviderSettingId(ecsCloudProvider.getUuid())
            .withClusterName("SdkTesting")
            .withLaunchType("EC2")
            .withRegion("us-east-1")
            .withServiceId(service.getUuid())
            .withAssignPublicIp(false)
            .withEnvId(environment.getUuid())
            .withAccountId(owners.obtainAccount().getUuid())
            .withAppId(owners.obtainApplication().getUuid())
            .withServiceTemplateId(owners.obtainServiceTemplate().getUuid())
            .build();
    try {
      return infrastructureMappingService.save(newInfrastructureMapping, true);
    } catch (WingsException de) {
      if (de.getCause() instanceof DuplicateKeyException) {
        InfrastructureMapping exists = exists(newInfrastructureMapping);
        if (exists != null) {
          return exists;
        }
      }
      throw de;
    }
  }

  private InfrastructureMapping ensureK8sTest(Randomizer.Seed seed, Owners owners, String namespace) {
    Environment environment = owners.obtainEnvironment();
    if (environment == null) {
      environment = environmentGenerator.ensurePredefined(seed, owners, Environments.GENERIC_TEST);
      owners.add(environment);
    }

    ApplicationManifest applicationManifest =
        applicationManifestService.getByEnvId(environment.getAppId(), environment.getUuid(), AppManifestKind.VALUES);

    if (applicationManifest == null) {
      environmentService.createValues(environment.getAppId(), environment.getUuid(), null,
          ManifestFile.builder().fileName("values.yaml").fileContent("serviceType: ClusterIP\n").build());
    }

    Service service = owners.obtainService();
    if (service == null) {
      service = serviceGenerator.ensurePredefined(seed, owners, Services.K8S_V2_TEST);
      owners.add(service);
    }

    final SettingAttribute gcpCloudProvider = settingGenerator.ensurePredefined(seed, owners, HARNESS_GCP_EXPLORATION);

    String namespaceUnique = namespace + '-' + System.currentTimeMillis();

    InfrastructureMapping newInfrastructureMapping =
        aGcpKubernetesInfrastructureMapping()
            .withName("exploration-harness-test-" + namespaceUnique)
            .withAutoPopulate(false)
            .withInfraMappingType(GCP_KUBERNETES.name())
            .withDeploymentType(DeploymentType.KUBERNETES.name())
            .withComputeProviderType(SettingVariableTypes.GCP.name())
            .withComputeProviderSettingId(gcpCloudProvider.getUuid())
            .withClusterName("us-west1-a/harness-test")
            .withNamespace(namespaceUnique)
            .withServiceId(service.getUuid())
            .withEnvId(environment.getUuid())
            .withAccountId(owners.obtainAccount().getUuid())
            .withAppId(owners.obtainApplication().getUuid())
            .withServiceTemplateId(owners.obtainServiceTemplate().getUuid())
            .build();
    try {
      return infrastructureMappingService.save(newInfrastructureMapping, true);
    } catch (WingsException de) {
      if (de.getCause() instanceof DuplicateKeyException) {
        InfrastructureMapping exists = exists(newInfrastructureMapping);
        if (exists != null) {
          return exists;
        }
      }
      throw de;
    }
  }

  private InfrastructureMapping ensurePhysicalWinRMTest(Randomizer.Seed seed, Owners owners) {
    Environment environment = owners.obtainEnvironment();
    if (environment == null) {
      environment = environmentGenerator.ensurePredefined(seed, owners, Environments.GENERIC_TEST);
      owners.add(environment);
    }

    Service service = owners.obtainService();
    if (service == null) {
      service = serviceGenerator.ensurePredefined(seed, owners, Services.WINDOWS_TEST);
      owners.add(service);
    }

    final SettingAttribute physicalInfraSettingAttr =
        settingGenerator.ensurePredefined(seed, owners, PHYSICAL_DATA_CENTER);
    final SettingAttribute winRmSettingAttribute =
        settingGenerator.ensurePredefined(seed, owners, WINRM_TEST_CONNECTOR);

    return ensureInfrastructureMapping(seed, owners,
        PhysicalInfrastructureMappingWinRm.Builder.aPhysicalInfrastructureMappingWinRm()
            .withName("Windows - winrm physical-infra workflow test")
            .withAutoPopulate(false)
            .withInfraMappingType(PHYSICAL_DATA_CENTER_WINRM.name())
            .withDeploymentType(DeploymentType.WINRM.name())
            .withComputeProviderType(SettingVariableTypes.PHYSICAL_DATA_CENTER.name())
            .withComputeProviderSettingId(physicalInfraSettingAttr.getUuid())
            .withWinRmConnectionAttributes(winRmSettingAttribute.getUuid())
            .withHostNames(asList(AZURE_DEPLOY_HOST))
            .build());
  }

  private InfrastructureMapping ensureAwsSshTest(Randomizer.Seed seed, Owners owners) {
    Environment environment = owners.obtainEnvironment();
    if (environment == null) {
      environment = environmentGenerator.ensurePredefined(seed, owners, Environments.GENERIC_TEST);
      owners.add(environment);
    }

    Service service = owners.obtainService();
    if (service == null) {
      service = serviceGenerator.ensurePredefined(seed, owners, Services.GENERIC_TEST);
      owners.add(service);
    }

    final List<Tag> tags = asList(Tag.builder().key("Purpose").value("test").build(),
        Tag.builder().key("User").value(System.getProperty("user.name")).build());

    final SettingAttribute awsTestSettingAttribute =
        settingGenerator.ensurePredefined(seed, owners, AWS_TEST_CLOUD_PROVIDER);
    final SettingAttribute devKeySettingAttribute = settingGenerator.ensurePredefined(seed, owners, DEV_TEST_CONNECTOR);

    return ensureInfrastructureMapping(seed, owners,
        anAwsInfrastructureMapping()
            .withName("Aws non prod - ssh workflow test")
            .withAutoPopulate(false)
            .withInfraMappingType(AWS_SSH.name())
            .withDeploymentType(DeploymentType.SSH.name())
            .withComputeProviderType(SettingVariableTypes.AWS.name())
            .withComputeProviderSettingId(awsTestSettingAttribute.getUuid())
            .withHostConnectionAttrs(devKeySettingAttribute.getUuid())
            .withUsePublicDns(true)
            .withRegion("us-east-1")
            .withAwsInstanceFilter(AwsInstanceFilter.builder().tags(tags).build())
            .build());
  }

  private InfrastructureMapping ensureAwsSshFunctionalTest(Randomizer.Seed seed, Owners owners) {
    Environment environment = owners.obtainEnvironment();
    if (environment == null) {
      environment = environmentGenerator.ensurePredefined(seed, owners, Environments.FUNCTIONAL_TEST);
      owners.add(environment);
    }

    Service service = owners.obtainService();
    if (service == null) {
      service = serviceGenerator.ensurePredefined(seed, owners, Services.FUNCTIONAL_TEST);
      owners.add(service);
    }

    final List<Tag> tags =
        asList(Tag.builder().key("Purpose").value("test").build(), Tag.builder().key("User").value("root").build());

    final SettingAttribute awsTestSettingAttribute =
        settingGenerator.ensurePredefined(seed, owners, AWS_TEST_CLOUD_PROVIDER);
    final SettingAttribute devKeySettingAttribute = settingGenerator.ensurePredefined(seed, owners, DEV_TEST_CONNECTOR);

    return ensureInfrastructureMapping(seed, owners,
        anAwsInfrastructureMapping()
            .withName("Aws non prod - ssh workflow test")
            .withAutoPopulate(false)
            .withInfraMappingType(AWS_SSH.name())
            .withDeploymentType(DeploymentType.SSH.name())
            .withComputeProviderType(SettingVariableTypes.AWS.name())
            .withComputeProviderSettingId(awsTestSettingAttribute.getUuid())
            .withHostConnectionAttrs(devKeySettingAttribute.getUuid())
            .withUsePublicDns(true)
            .withRegion("us-east-1")
            .withAwsInstanceFilter(AwsInstanceFilter.builder().tags(tags).build())
            .build());
  }

  private InfrastructureMapping ensureTerraformAwsSshTest(Randomizer.Seed seed, Owners owners) {
    InfrastructureProvisioner infrastructureProvisioner = owners.obtainInfrastructureProvisioner();
    if (infrastructureProvisioner == null) {
      infrastructureProvisioner =
          infrastructureProvisionerGenerator.ensurePredefined(seed, owners, InfrastructureProvisioners.TERRAFORM_TEST);
      owners.add(infrastructureProvisioner);
    }

    Environment environment = owners.obtainEnvironment();
    if (environment == null) {
      environment = environmentGenerator.ensurePredefined(seed, owners, Environments.GENERIC_TEST);
      owners.add(environment);
    }

    Service service = owners.obtainService();
    if (service == null) {
      service = serviceGenerator.ensurePredefined(seed, owners, Services.GENERIC_TEST);
      owners.add(service);
    }

    final SettingAttribute awsTestSettingAttribute =
        settingGenerator.ensurePredefined(seed, owners, AWS_TEST_CLOUD_PROVIDER);
    final SettingAttribute devKeySettingAttribute = settingGenerator.ensurePredefined(seed, owners, DEV_TEST_CONNECTOR);

    return ensureInfrastructureMapping(seed, owners,
        anAwsInfrastructureMapping()
            .withName("Aws non prod - ssh terraform provisioner test")
            .withProvisionerId(infrastructureProvisioner.getUuid())
            .withAutoPopulate(false)
            .withInfraMappingType(AWS_SSH.name())
            .withDeploymentType(DeploymentType.SSH.name())
            .withComputeProviderType(SettingVariableTypes.AWS.name())
            .withComputeProviderSettingId(awsTestSettingAttribute.getUuid())
            .withHostConnectionAttrs(devKeySettingAttribute.getUuid())
            .withUsePublicDns(true)
            .withAwsInstanceFilter(AwsInstanceFilter.builder().build())
            .build());
  }

  public InfrastructureMapping ensureRandom(Randomizer.Seed seed, Owners owners) {
    if (owners == null) {
      owners = ownerManager.create();
    }

    EnhancedRandom random = Randomizer.instance(seed);
    InfrastructureMappings predefined = random.nextObject(InfrastructureMappings.class);
    return ensurePredefined(seed, owners, predefined);
  }

  void fillOwners(InfrastructureMapping infrastructureMapping, Owners owners) {
    if (owners.obtainApplication() == null && infrastructureMapping.getAppId() != null) {
      Application application = appService.get(infrastructureMapping.getAppId());
      owners.add(application);
    }

    if (owners.obtainInfrastructureProvisioner() == null && infrastructureMapping.getProvisionerId() != null) {
      InfrastructureProvisioner infrastructureProvisioner =
          wingsPersistence.get(InfrastructureProvisioner.class, infrastructureMapping.getProvisionerId());
      owners.add(infrastructureProvisioner);
    }

    if (owners.obtainEnvironment() == null && infrastructureMapping.getEnvId() != null) {
      Environment environment = wingsPersistence.get(Environment.class, infrastructureMapping.getEnvId());
      owners.add(environment);
    }

    if (owners.obtainService() == null && infrastructureMapping.getServiceId() != null) {
      Service service = wingsPersistence.get(Service.class, infrastructureMapping.getServiceId());
      owners.add(service);
    }
  }

  public InfrastructureMapping exists(InfrastructureMapping infrastructureMapping) {
    return wingsPersistence.createQuery(InfrastructureMapping.class)
        .filter(InfrastructureMapping.APP_ID_KEY, infrastructureMapping.getAppId())
        .filter(InfrastructureMapping.ENV_ID_KEY, infrastructureMapping.getEnvId())
        .filter(InfrastructureMapping.NAME_KEY, infrastructureMapping.getName())
        .get();
  }

  public InfrastructureMapping ensureInfrastructureMapping(
      Randomizer.Seed seed, Owners owners, InfrastructureMapping infrastructureMapping) {
    EnhancedRandom random = Randomizer.instance(seed);

    if (owners == null) {
      owners = ownerManager.create();
    }
    fillOwners(infrastructureMapping, owners);

    InfrastructureMappingType infrastructureMappingType;

    if (infrastructureMapping.getInfraMappingType() != null) {
      infrastructureMappingType = InfrastructureMappingType.valueOf(infrastructureMapping.getInfraMappingType());
    } else {
      infrastructureMappingType = random.nextObject(InfrastructureMappingType.class);
    }

    InfrastructureMapping newInfrastructureMapping = null;
    switch (infrastructureMappingType) {
      case AWS_SSH:
        AwsInfrastructureMapping awsInfrastructureMapping = (AwsInfrastructureMapping) infrastructureMapping;
        final AwsInfrastructureMapping.Builder builder =
            anAwsInfrastructureMapping().withInfraMappingType(infrastructureMappingType.name());

        if (infrastructureMapping.getAppId() != null) {
          builder.withAppId(infrastructureMapping.getAppId());
        } else {
          final Application application = owners.obtainApplication();
          builder.withAppId(application.getUuid());
        }

        if (infrastructureMapping.getEnvId() != null) {
          builder.withEnvId(infrastructureMapping.getEnvId());
        } else {
          Environment environment = owners.obtainEnvironment();
          if (environment == null) {
            environment = environmentGenerator.ensureRandom(seed, owners);
            owners.add(environment);
          }
          builder.withEnvId(environment.getUuid());
        }

        if (infrastructureMapping.getName() != null) {
          builder.withName(infrastructureMapping.getName());
        } else {
          builder.withName(random.nextObject(String.class));
        }

        InfrastructureMapping existing = exists(builder.build());
        if (existing != null) {
          return existing;
        }

        builder.withAutoPopulate(infrastructureMapping.isAutoPopulate());

        if (infrastructureMapping.getAccountId() != null) {
          builder.withAccountId(infrastructureMapping.getAccountId());
        } else {
          final Account account = owners.obtainAccount();
          builder.withAccountId(account.getUuid());
        }

        if (infrastructureMapping.getServiceId() != null) {
          builder.withServiceId(infrastructureMapping.getServiceId());
        } else {
          Service service = owners.obtainService();
          if (service == null) {
            service = serviceGenerator.ensureRandom(seed, owners);
            owners.add(service);
          }
          builder.withServiceId(service.getUuid());
        }

        if (awsInfrastructureMapping.getRegion() != null) {
          builder.withRegion(awsInfrastructureMapping.getRegion());
        } else {
          builder.withRegion("us-east-1");
        }

        if (awsInfrastructureMapping.getAwsInstanceFilter() != null) {
          builder.withAwsInstanceFilter(awsInfrastructureMapping.getAwsInstanceFilter());
        } else {
          // TODO: provide more options
          builder.withProvisionInstances(false);
          builder.withAwsInstanceFilter(AwsInstanceFilter.builder().build());
        }

        builder.withUsePublicDns(awsInfrastructureMapping.isUsePublicDns());

        if (infrastructureMapping.getServiceTemplateId() != null) {
          builder.withServiceTemplateId(infrastructureMapping.getServiceTemplateId());
        } else {
          Service service = owners.obtainService();
          ServiceTemplate serviceTemplate = owners.obtainServiceTemplate();
          builder.withServiceTemplateId(serviceTemplate.getUuid());
        }

        if (infrastructureMapping.getDeploymentType() != null) {
          builder.withDeploymentType(infrastructureMapping.getDeploymentType());
        } else {
          DeploymentType deploymentType = owners.obtainService().getDeploymentType();
          if (deploymentType != null) {
            builder.withDeploymentType(deploymentType.name());
          } else {
            builder.withDeploymentType(random.nextObject(DeploymentType.class).name());
          }
        }

        builder.withComputeProviderType(SettingVariableTypes.AWS.name());

        if (infrastructureMapping.getComputeProviderSettingId() != null) {
          builder.withComputeProviderSettingId(infrastructureMapping.getComputeProviderSettingId());
        } else {
          final SettingAttribute settingAttribute =
              settingGenerator.ensurePredefined(seed, owners, AWS_TEST_CLOUD_PROVIDER);
          builder.withComputeProviderSettingId(settingAttribute.getUuid());
        }

        if (infrastructureMapping.getHostConnectionAttrs() != null) {
          builder.withHostConnectionAttrs(infrastructureMapping.getHostConnectionAttrs());
        } else {
          final SettingAttribute devKeySettingAttribute =
              settingGenerator.ensurePredefined(seed, owners, DEV_TEST_CONNECTOR);
          builder.withHostConnectionAttrs(devKeySettingAttribute.getUuid());
        }

        if (infrastructureMapping.getProvisionerId() != null) {
          builder.withProvisionerId(infrastructureMapping.getProvisionerId());
        }

        newInfrastructureMapping = builder.build();
        break;
      case AWS_ECS:
        EcsInfrastructureMapping ecsInfrastructureMapping = (EcsInfrastructureMapping) infrastructureMapping;
        existing = exists(ecsInfrastructureMapping);
        if (existing != null) {
          return existing;
        }

        newInfrastructureMapping = ecsInfrastructureMapping;
        break;
      case AZURE_INFRA:
        AzureInfrastructureMapping azureInfrastructureMapping = (AzureInfrastructureMapping) infrastructureMapping;
        final AzureInfrastructureMapping.Builder azureInfraMappingBuilder =
            Builder.anAzureInfrastructureMapping().withInfraMappingType(infrastructureMappingType.name());

        if (infrastructureMapping.getAppId() != null) {
          azureInfraMappingBuilder.withAppId(infrastructureMapping.getAppId());
        } else {
          final Application application = owners.obtainApplication();
          azureInfraMappingBuilder.withAppId(application.getUuid());
        }

        if (infrastructureMapping.getEnvId() != null) {
          azureInfraMappingBuilder.withEnvId(infrastructureMapping.getEnvId());
        } else {
          Environment environment = owners.obtainEnvironment();
          if (environment == null) {
            environment = environmentGenerator.ensureRandom(seed, owners);
            owners.add(environment);
          }
          azureInfraMappingBuilder.withEnvId(environment.getUuid());
        }

        if (infrastructureMapping.getName() != null) {
          azureInfraMappingBuilder.withName(infrastructureMapping.getName());
        } else {
          azureInfraMappingBuilder.withName(random.nextObject(String.class));
        }

        InfrastructureMapping existingAzureInfraMapping = exists(azureInfraMappingBuilder.build());
        if (existingAzureInfraMapping != null) {
          return existingAzureInfraMapping;
        }

        azureInfraMappingBuilder.withAutoPopulate(infrastructureMapping.isAutoPopulate());
        azureInfraMappingBuilder.withComputeProviderType(SettingVariableTypes.AZURE.name());

        if (infrastructureMapping.getComputeProviderSettingId() != null) {
          azureInfraMappingBuilder.withComputeProviderSettingId(infrastructureMapping.getComputeProviderSettingId());
        } else {
          final SettingAttribute settingAttribute =
              settingGenerator.ensurePredefined(seed, owners, AZURE_TEST_CLOUD_PROVIDER);
          azureInfraMappingBuilder.withComputeProviderSettingId(settingAttribute.getUuid());
        }

        if (infrastructureMapping.getAccountId() != null) {
          azureInfraMappingBuilder.withAccountId(infrastructureMapping.getAccountId());
        } else {
          final Account account = owners.obtainAccount();
          azureInfraMappingBuilder.withAccountId(account.getUuid());
        }

        if (infrastructureMapping.getServiceId() != null) {
          azureInfraMappingBuilder.withServiceId(infrastructureMapping.getServiceId());
        } else {
          Service service = owners.obtainService();
          if (service == null) {
            service = serviceGenerator.ensureRandom(seed, owners);
            owners.add(service);
          }
          azureInfraMappingBuilder.withServiceId(service.getUuid());
        }

        if (infrastructureMapping.getServiceTemplateId() != null) {
          azureInfraMappingBuilder.withServiceTemplateId(infrastructureMapping.getServiceTemplateId());
        } else {
          Service service = owners.obtainService();
          ServiceTemplate serviceTemplate = owners.obtainServiceTemplate();
          azureInfraMappingBuilder.withServiceTemplateId(serviceTemplate.getUuid());
        }

        if (azureInfrastructureMapping.getDeploymentType() != null) {
          azureInfraMappingBuilder.withDeploymentType(azureInfrastructureMapping.getDeploymentType());
        } else {
          DeploymentType deploymentType = owners.obtainService().getDeploymentType();
          if (deploymentType != null) {
            azureInfraMappingBuilder.withDeploymentType(deploymentType.name());
          } else {
            azureInfraMappingBuilder.withDeploymentType(random.nextObject(DeploymentType.class).name());
          }
        }

        if (azureInfrastructureMapping.getSubscriptionId() != null) {
          azureInfraMappingBuilder.withSubscriptionId(azureInfrastructureMapping.getSubscriptionId());
        } else {
          azureInfraMappingBuilder.withSubscriptionId(AZURE_SUBSCRIPTION_ID);
        }

        if (azureInfrastructureMapping.getResourceGroup() != null) {
          azureInfraMappingBuilder.withResourceGroup(azureInfrastructureMapping.getResourceGroup());
        } else {
          azureInfraMappingBuilder.withResourceGroup(AZURE_RESOURCE_GROUP);
        }

        if (azureInfrastructureMapping.getWinRmConnectionAttributes() != null) {
          azureInfraMappingBuilder.withWinRmConnectionAttributes(
              azureInfrastructureMapping.getWinRmConnectionAttributes());
        } else {
          final SettingAttribute winRmSettingAttribute =
              settingGenerator.ensurePredefined(seed, owners, WINRM_TEST_CONNECTOR);
          azureInfraMappingBuilder.withWinRmConnectionAttributes(winRmSettingAttribute.getUuid());
        }

        if (azureInfrastructureMapping.getTags() != null) {
          azureInfraMappingBuilder.withTags(azureInfrastructureMapping.getTags());
        }

        azureInfraMappingBuilder.withUsePublicDns(azureInfrastructureMapping.isUsePublicDns());
        newInfrastructureMapping = azureInfraMappingBuilder.build();
        break;

      case PHYSICAL_DATA_CENTER_WINRM:
        PhysicalInfrastructureMappingWinRm physicalInfrastructureMappingWinRm =
            (PhysicalInfrastructureMappingWinRm) infrastructureMapping;
        final PhysicalInfrastructureMappingWinRm.Builder phyWinRmbuilder =
            PhysicalInfrastructureMappingWinRm.Builder.aPhysicalInfrastructureMappingWinRm().withInfraMappingType(
                infrastructureMappingType.name());

        if (infrastructureMapping.getAppId() != null) {
          phyWinRmbuilder.withAppId(infrastructureMapping.getAppId());
        } else {
          final Application application = owners.obtainApplication();
          phyWinRmbuilder.withAppId(application.getUuid());
        }

        if (infrastructureMapping.getEnvId() != null) {
          phyWinRmbuilder.withEnvId(infrastructureMapping.getEnvId());
        } else {
          Environment environment = owners.obtainEnvironment();
          if (environment == null) {
            environment = environmentGenerator.ensureRandom(seed, owners);
            owners.add(environment);
          }
          phyWinRmbuilder.withEnvId(environment.getUuid());
        }

        if (infrastructureMapping.getName() != null) {
          phyWinRmbuilder.withName(infrastructureMapping.getName());
        } else {
          phyWinRmbuilder.withName(random.nextObject(String.class));
        }

        InfrastructureMapping existingInfraMapping = exists(phyWinRmbuilder.build());
        if (existingInfraMapping != null) {
          return existingInfraMapping;
        }

        phyWinRmbuilder.withAutoPopulate(infrastructureMapping.isAutoPopulate());

        if (infrastructureMapping.getAccountId() != null) {
          phyWinRmbuilder.withAccountId(infrastructureMapping.getAccountId());
        } else {
          final Account account = owners.obtainAccount();
          phyWinRmbuilder.withAccountId(account.getUuid());
        }

        if (infrastructureMapping.getServiceId() != null) {
          phyWinRmbuilder.withServiceId(infrastructureMapping.getServiceId());
        } else {
          Service service = owners.obtainService();
          if (service == null) {
            service = serviceGenerator.ensureRandom(seed, owners);
            owners.add(service);
          }
          phyWinRmbuilder.withServiceId(service.getUuid());
        }

        if (infrastructureMapping.getServiceTemplateId() != null) {
          phyWinRmbuilder.withServiceTemplateId(infrastructureMapping.getServiceTemplateId());
        } else {
          Service service = owners.obtainService();
          ServiceTemplate serviceTemplate = owners.obtainServiceTemplate();
          phyWinRmbuilder.withServiceTemplateId(serviceTemplate.getUuid());
        }

        if (infrastructureMapping.getDeploymentType() != null) {
          phyWinRmbuilder.withDeploymentType(infrastructureMapping.getDeploymentType());
        } else {
          DeploymentType deploymentType = owners.obtainService().getDeploymentType();
          if (deploymentType != null) {
            phyWinRmbuilder.withDeploymentType(deploymentType.name());
          } else {
            phyWinRmbuilder.withDeploymentType(random.nextObject(DeploymentType.class).name());
          }
        }

        phyWinRmbuilder.withComputeProviderType(SettingVariableTypes.PHYSICAL_DATA_CENTER.name());

        if (infrastructureMapping.getComputeProviderSettingId() != null) {
          phyWinRmbuilder.withComputeProviderSettingId(infrastructureMapping.getComputeProviderSettingId());
        } else {
          final SettingAttribute physicalInfraSettingAttr =
              settingGenerator.ensurePredefined(seed, owners, PHYSICAL_DATA_CENTER);
          phyWinRmbuilder.withComputeProviderSettingId(physicalInfraSettingAttr.getUuid());
        }

        if (physicalInfrastructureMappingWinRm.getWinRmConnectionAttributes() != null) {
          phyWinRmbuilder.withWinRmConnectionAttributes(
              physicalInfrastructureMappingWinRm.getWinRmConnectionAttributes());
        } else {
          final SettingAttribute winRmSettingAttribute =
              settingGenerator.ensurePredefined(seed, owners, WINRM_TEST_CONNECTOR);
          phyWinRmbuilder.withWinRmConnectionAttributes(winRmSettingAttribute.getUuid());
        }

        if (physicalInfrastructureMappingWinRm.getHostNames() != null) {
          phyWinRmbuilder.withHostNames(physicalInfrastructureMappingWinRm.getHostNames());
        } else {
          phyWinRmbuilder.withHostNames(asList(AZURE_DEPLOY_HOST));
        }

        newInfrastructureMapping = phyWinRmbuilder.build();
        break;

      default:
        unhandled(infrastructureMappingType);
        throw new UnsupportedOperationException();
    }

    try {
      return infrastructureMappingService.save(newInfrastructureMapping);
    } catch (WingsException de) {
      if (de.getCause() instanceof DuplicateKeyException) {
        InfrastructureMapping exists = exists(newInfrastructureMapping);
        if (exists != null) {
          return exists;
        }
      }
      throw de;
    }
  }
}
