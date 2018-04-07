package software.wings.generator;

import static io.harness.govern.Switch.unhandled;
import static java.util.Arrays.asList;
import static software.wings.beans.AwsInfrastructureMapping.Builder.anAwsInfrastructureMapping;
import static software.wings.beans.InfrastructureMappingType.AWS_SSH;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.github.benas.randombeans.EnhancedRandomBuilder;
import io.github.benas.randombeans.api.EnhancedRandom;
import software.wings.api.DeploymentType;
import software.wings.beans.AwsInfrastructureMapping;
import software.wings.beans.AwsInstanceFilter;
import software.wings.beans.AwsInstanceFilter.Tag;
import software.wings.beans.Environment;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.InfrastructureMappingType;
import software.wings.beans.Service;
import software.wings.beans.ServiceTemplate;
import software.wings.beans.SettingAttribute;
import software.wings.generator.EnvironmentGenerator.Environments;
import software.wings.generator.ServiceGenerator.Services;
import software.wings.generator.SettingGenerator.Settings;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.ServiceTemplateService;
import software.wings.settings.SettingValue.SettingVariableTypes;

import java.util.List;

@Singleton
public class InfrastructureMappingGenerator {
  @Inject private EnvironmentGenerator environmentGenerator;
  @Inject private ServiceGenerator serviceGenerator;
  @Inject private SettingGenerator settingGenerator;

  @Inject private AppService appService;
  @Inject private InfrastructureMappingService infrastructureMappingService;
  @Inject private ServiceTemplateService serviceTemplateService;

  public enum InfrastructureMappings {
    AWS_SSH_TEST,
  }

  public InfrastructureMapping ensurePredefined(long seed, InfrastructureMappings predefined) {
    switch (predefined) {
      case AWS_SSH_TEST:
        return ensureAwsSshTest(seed);
      default:
        unhandled(predefined);
    }

    return null;
  }

  private InfrastructureMapping ensureAwsSshTest(long seed) {
    final Environment environment = environmentGenerator.ensurePredefined(seed, Environments.GENERIC_TEST);

    Service service = serviceGenerator.ensurePredefined(seed, Services.GENERIC_TEST);
    ServiceTemplate serviceTemplate =
        serviceTemplateService.get(service.getAppId(), service.getUuid(), environment.getUuid());

    final String accountId = appService.getAccountIdByAppId(environment.getAppId());

    final List<Tag> tags = asList(Tag.builder().key("Purpose").value("test").build(),
        Tag.builder().key("User").value(System.getProperty("user.name")).build());

    final SettingAttribute awsTestSettingAttribute =
        settingGenerator.ensurePredefined(seed, Settings.AWS_TEST_CLOUD_PROVIDER);
    final SettingAttribute devKeySettingAttribute =
        settingGenerator.ensurePredefined(seed, Settings.DEV_TEST_CONNECTOR);

    return ensureInfrastructureMapping(seed,
        anAwsInfrastructureMapping()
            .withName("Aws non prod - ssh workflow test")
            .withAutoPopulate(false)
            .withInfraMappingType(AWS_SSH.name())
            .withAccountId(accountId)
            .withAppId(environment.getAppId())
            .withServiceTemplateId(serviceTemplate.getUuid())
            .withEnvId(environment.getUuid())
            .withDeploymentType(DeploymentType.SSH.name())
            .withComputeProviderType(SettingVariableTypes.AWS.name())
            .withComputeProviderSettingId(awsTestSettingAttribute.getUuid())
            .withHostConnectionAttrs(devKeySettingAttribute.getUuid())
            .withUsePublicDns(true)
            .withRegion("us-east-1")
            .withAwsInstanceFilter(AwsInstanceFilter.builder().tags(tags).build())
            .build());
  }

  public InfrastructureMapping ensureRandom(long seed) {
    EnhancedRandom random =
        EnhancedRandomBuilder.aNewEnhancedRandomBuilder().seed(seed).scanClasspathForConcreteTypes(true).build();

    InfrastructureMappings predefined = random.nextObject(InfrastructureMappings.class);

    return ensurePredefined(seed, predefined);
  }

  public InfrastructureMapping ensureInfrastructureMapping(long seed, InfrastructureMapping infrastructureMapping) {
    EnhancedRandom random =
        EnhancedRandomBuilder.aNewEnhancedRandomBuilder().seed(seed).scanClasspathForConcreteTypes(true).build();

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

        if (awsInfrastructureMapping != null && awsInfrastructureMapping.getRegion() != null) {
          builder.withRegion(awsInfrastructureMapping.getRegion());
        } else {
          throw new UnsupportedOperationException();
        }

        if (awsInfrastructureMapping != null && awsInfrastructureMapping.getAwsInstanceFilter() != null) {
          builder.withAwsInstanceFilter(awsInfrastructureMapping.getAwsInstanceFilter());
        } else {
          throw new UnsupportedOperationException();
        }

        builder.withUsePublicDns(awsInfrastructureMapping.isUsePublicDns());

        if (infrastructureMapping != null && infrastructureMapping.getName() != null) {
          builder.withName(infrastructureMapping.getName());
        } else {
          throw new UnsupportedOperationException();
        }

        builder.withAutoPopulate(infrastructureMapping.isAutoPopulate());

        if (infrastructureMapping != null && infrastructureMapping.getAccountId() != null) {
          builder.withAccountId(infrastructureMapping.getAccountId());
        } else {
          throw new UnsupportedOperationException();
        }

        if (infrastructureMapping != null && infrastructureMapping.getAppId() != null) {
          builder.withAppId(infrastructureMapping.getAppId());
        } else {
          throw new UnsupportedOperationException();
        }

        if (infrastructureMapping != null && infrastructureMapping.getEnvId() != null) {
          builder.withEnvId(infrastructureMapping.getEnvId());
        } else {
          throw new UnsupportedOperationException();
        }

        if (infrastructureMapping != null && infrastructureMapping.getServiceTemplateId() != null) {
          builder.withServiceTemplateId(infrastructureMapping.getServiceTemplateId());
        } else {
          throw new UnsupportedOperationException();
        }

        if (infrastructureMapping != null && infrastructureMapping.getDeploymentType() != null) {
          builder.withDeploymentType(infrastructureMapping.getDeploymentType());
        } else {
          throw new UnsupportedOperationException();
        }

        if (infrastructureMapping != null && infrastructureMapping.getComputeProviderType() != null) {
          builder.withComputeProviderType(infrastructureMapping.getComputeProviderType());
        } else {
          throw new UnsupportedOperationException();
        }

        if (infrastructureMapping != null && infrastructureMapping.getComputeProviderSettingId() != null) {
          builder.withComputeProviderSettingId(infrastructureMapping.getComputeProviderSettingId());
        } else {
          throw new UnsupportedOperationException();
        }

        if (infrastructureMapping != null && infrastructureMapping.getHostConnectionAttrs() != null) {
          builder.withHostConnectionAttrs(infrastructureMapping.getHostConnectionAttrs());
        } else {
          throw new UnsupportedOperationException();
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
