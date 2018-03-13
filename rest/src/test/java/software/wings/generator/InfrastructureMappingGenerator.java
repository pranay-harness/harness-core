package software.wings.generator;

import static io.harness.govern.Switch.unhandled;
import static software.wings.beans.AwsInfrastructureMapping.Builder.anAwsInfrastructureMapping;

import com.google.inject.Inject;

import io.github.benas.randombeans.EnhancedRandomBuilder;
import io.github.benas.randombeans.api.EnhancedRandom;
import software.wings.beans.AwsInfrastructureMapping;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.InfrastructureMappingType;
import software.wings.service.intfc.InfrastructureMappingService;

public class InfrastructureMappingGenerator {
  @Inject InfrastructureMappingService infrastructureMappingService;

  public InfrastructureMapping createInfrastructureMapping(long seed, InfrastructureMapping infrastructureMapping) {
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
