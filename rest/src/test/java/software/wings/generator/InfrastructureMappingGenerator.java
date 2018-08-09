package software.wings.generator;

import static io.harness.govern.Switch.unhandled;
import static java.util.Arrays.asList;
import static software.wings.beans.AwsInfrastructureMapping.Builder.anAwsInfrastructureMapping;
import static software.wings.beans.InfrastructureMappingType.AWS_SSH;
import static software.wings.generator.SettingGenerator.Settings.AWS_TEST_CLOUD_PROVIDER;
import static software.wings.generator.SettingGenerator.Settings.DEV_TEST_CONNECTOR;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.github.benas.randombeans.api.EnhancedRandom;
import software.wings.api.DeploymentType;
import software.wings.beans.Account;
import software.wings.beans.Application;
import software.wings.beans.AwsInfrastructureMapping;
import software.wings.beans.AwsInstanceFilter;
import software.wings.beans.AwsInstanceFilter.Tag;
import software.wings.beans.Environment;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.InfrastructureMappingType;
import software.wings.beans.InfrastructureProvisioner;
import software.wings.beans.Service;
import software.wings.beans.ServiceTemplate;
import software.wings.beans.SettingAttribute;
import software.wings.dl.WingsPersistence;
import software.wings.generator.EnvironmentGenerator.Environments;
import software.wings.generator.InfrastructureProvisionerGenerator.InfrastructureProvisioners;
import software.wings.generator.OwnerManager.Owners;
import software.wings.generator.ServiceGenerator.Services;
import software.wings.service.intfc.AppService;
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
  @Inject private ServiceTemplateService serviceTemplateService;

  @Inject private WingsPersistence wingsPersistence;

  public enum InfrastructureMappings { AWS_SSH_TEST, TERRAFORM_AWS_SSH_TEST }

  public InfrastructureMapping ensurePredefined(
      Randomizer.Seed seed, Owners owners, InfrastructureMappings predefined) {
    switch (predefined) {
      case AWS_SSH_TEST:
        return ensureAwsSshTest(seed, owners);
      case TERRAFORM_AWS_SSH_TEST:
        return ensureTerraformAwsSshTest(seed, owners);
      default:
        unhandled(predefined);
    }
    return null;
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

    if (infrastructureMapping != null && infrastructureMapping.getInfraMappingType() != null) {
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

        if (infrastructureMapping != null && infrastructureMapping.getAppId() != null) {
          builder.withAppId(infrastructureMapping.getAppId());
        } else {
          final Application application = owners.obtainApplication();
          builder.withAppId(application.getUuid());
        }

        if (infrastructureMapping != null && infrastructureMapping.getEnvId() != null) {
          builder.withEnvId(infrastructureMapping.getEnvId());
        } else {
          Environment environment = owners.obtainEnvironment();
          if (environment == null) {
            environment = environmentGenerator.ensureRandom(seed, owners);
            owners.add(environment);
          }
          builder.withEnvId(environment.getUuid());
        }

        if (infrastructureMapping != null && infrastructureMapping.getName() != null) {
          builder.withName(infrastructureMapping.getName());
        } else {
          builder.withName(random.nextObject(String.class));
        }

        InfrastructureMapping existing = exists(builder.build());
        if (existing != null) {
          return existing;
        }

        builder.withAutoPopulate(infrastructureMapping.isAutoPopulate());

        if (infrastructureMapping != null && infrastructureMapping.getAccountId() != null) {
          builder.withAccountId(infrastructureMapping.getAccountId());
        } else {
          final Account account = owners.obtainAccount();
          builder.withAccountId(account.getUuid());
        }

        if (infrastructureMapping != null && infrastructureMapping.getServiceId() != null) {
          builder.withServiceId(infrastructureMapping.getServiceId());
        } else {
          Service service = owners.obtainService();
          if (service == null) {
            service = serviceGenerator.ensureRandom(seed, owners);
            owners.add(service);
          }
          builder.withServiceId(service.getUuid());
        }

        if (awsInfrastructureMapping != null && awsInfrastructureMapping.getRegion() != null) {
          builder.withRegion(awsInfrastructureMapping.getRegion());
        } else {
          builder.withRegion("us-east-1");
        }

        if (awsInfrastructureMapping != null && awsInfrastructureMapping.getAwsInstanceFilter() != null) {
          builder.withAwsInstanceFilter(awsInfrastructureMapping.getAwsInstanceFilter());
        } else {
          // TODO: provide more options
          builder.withProvisionInstances(false);
          builder.withAwsInstanceFilter(AwsInstanceFilter.builder().build());
        }

        builder.withUsePublicDns(awsInfrastructureMapping.isUsePublicDns());

        if (infrastructureMapping != null && infrastructureMapping.getServiceTemplateId() != null) {
          builder.withServiceTemplateId(infrastructureMapping.getServiceTemplateId());
        } else {
          Service service = owners.obtainService();
          ServiceTemplate serviceTemplate = owners.obtainServiceTemplate();
          builder.withServiceTemplateId(serviceTemplate.getUuid());
        }

        if (infrastructureMapping != null && infrastructureMapping.getDeploymentType() != null) {
          builder.withDeploymentType(infrastructureMapping.getDeploymentType());
        } else {
          builder.withDeploymentType(random.nextObject(DeploymentType.class).name());
        }

        if (infrastructureMapping != null && infrastructureMapping.getComputeProviderType() != null) {
          builder.withComputeProviderType(infrastructureMapping.getComputeProviderType());
        } else {
          builder.withComputeProviderType(random.nextObject(SettingVariableTypes.class).name());
        }

        if (infrastructureMapping != null && infrastructureMapping.getComputeProviderSettingId() != null) {
          builder.withComputeProviderSettingId(infrastructureMapping.getComputeProviderSettingId());
        } else {
          final SettingAttribute settingAttribute =
              settingGenerator.ensurePredefined(seed, owners, AWS_TEST_CLOUD_PROVIDER);
          builder.withComputeProviderSettingId(settingAttribute.getUuid());
        }

        if (infrastructureMapping != null && infrastructureMapping.getHostConnectionAttrs() != null) {
          builder.withHostConnectionAttrs(infrastructureMapping.getHostConnectionAttrs());
        } else {
          final SettingAttribute devKeySettingAttribute =
              settingGenerator.ensurePredefined(seed, owners, DEV_TEST_CONNECTOR);
          builder.withHostConnectionAttrs(devKeySettingAttribute.getUuid());
        }

        if (infrastructureMapping != null && infrastructureMapping.getProvisionerId() != null) {
          builder.withProvisionerId(infrastructureMapping.getProvisionerId());
        }

        newInfrastructureMapping = builder.build();
        break;
      default:
        unhandled(infrastructureMappingType);
        throw new UnsupportedOperationException();
    }

    return infrastructureMappingService.save(newInfrastructureMapping);
  }
}
