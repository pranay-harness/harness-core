package software.wings.generator;

import static io.harness.govern.Switch.unhandled;
import static java.util.Arrays.asList;
import static software.wings.api.DeploymentType.SSH;
import static software.wings.beans.InfrastructureMappingBlueprint.CloudProviderType.AWS;
import static software.wings.beans.Service.builder;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.github.benas.randombeans.api.EnhancedRandom;
import software.wings.beans.Account;
import software.wings.beans.Application;
import software.wings.beans.InfrastructureMappingBlueprint;
import software.wings.beans.InfrastructureProvisioner;
import software.wings.beans.InfrastructureProvisionerType;
import software.wings.beans.NameValuePair;
import software.wings.beans.Service;
import software.wings.beans.SettingAttribute;
import software.wings.beans.TerraformInfrastructureProvisioner;
import software.wings.beans.TerraformInfrastructureProvisioner.TerraformInfrastructureProvisionerBuilder;
import software.wings.dl.WingsPersistence;
import software.wings.generator.OwnerManager.Owners;
import software.wings.generator.SecretGenerator.SecretName;
import software.wings.generator.SettingGenerator.Settings;
import software.wings.service.intfc.InfrastructureProvisionerService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.utils.ArtifactType;

@Singleton
public class InfrastructureProvisionerGenerator {
  @Inject OwnerManager ownerManager;
  @Inject private ApplicationGenerator applicationGenerator;
  @Inject private ServiceGenerator serviceGenerator;
  @Inject private SettingGenerator settingGenerator;
  @Inject SecretGenerator secretGenerator;

  @Inject private ServiceResourceService serviceResourceService;
  @Inject private InfrastructureProvisionerService infrastructureProvisionerService;
  @Inject private SecretManager secretManager;

  @Inject WingsPersistence wingsPersistence;

  public enum InfrastructureProvisioners { TERRAFORM_TEST }

  public InfrastructureProvisioner ensurePredefined(
      Randomizer.Seed seed, Owners owners, InfrastructureProvisioners predefined) {
    switch (predefined) {
      case TERRAFORM_TEST:
        return ensureTerraformTest(seed, owners);
      default:
        unhandled(predefined);
    }
    return null;
  }
  private InfrastructureProvisioner ensureTerraformTest(Randomizer.Seed seed, Owners owners) {
    final Service archive =
        serviceGenerator.ensureService(seed, owners, builder().name("Archive").artifactType(ArtifactType.WAR).build());

    final Service factory =
        serviceGenerator.ensureService(seed, owners, builder().name("Factory").artifactType(ArtifactType.WAR).build());

    final Service warehouse = serviceGenerator.ensureService(
        seed, owners, builder().name("Warehouse").artifactType(ArtifactType.WAR).build());

    final SettingAttribute gitSourceSettingAttribute =
        settingGenerator.ensurePredefined(seed, Settings.TERRAFORM_TEST_GIT_REPO);

    final Account account = owners.obtainAccount();

    final TerraformInfrastructureProvisioner terraformInfrastructureProvisioner =
        TerraformInfrastructureProvisioner.builder()
            .name("Harness Terraform Test")
            .sourceRepoSettingId(gitSourceSettingAttribute.getUuid())
            .variables(asList(NameValuePair.builder()
                                  .name("access_key")
                                  .value(secretGenerator.decryptToString(new SecretName("aws_playground_access_key")))
                                  .valueType("TEXT")
                                  .build(),
                NameValuePair.builder()
                    .name("secret_key")
                    .value(secretGenerator.decryptToString(new SecretName("aws_playground_secret_key")))
                    .valueType("TEXT")
                    .build()))
            .mappingBlueprints(asList(
                InfrastructureMappingBlueprint.builder()
                    .serviceId(archive.getUuid())
                    .deploymentType(SSH)
                    .cloudProviderType(AWS)
                    .properties(asList(NameValuePair.builder().name("region").value("${terraform.region}").build(),
                        NameValuePair.builder().name("securityGroups").value("${terraform.security_group}").build(),
                        NameValuePair.builder().name("tags").value("${terraform.archive_tags}").build()))
                    .build(),
                InfrastructureMappingBlueprint.builder()
                    .serviceId(factory.getUuid())
                    .deploymentType(SSH)
                    .cloudProviderType(AWS)
                    .properties(asList(NameValuePair.builder().name("region").value("${terraform.region}").build(),
                        NameValuePair.builder().name("securityGroups").value("${terraform.security_group}").build(),
                        NameValuePair.builder().name("tags").value("${terraform.factory_tags}").build()))
                    .build(),
                InfrastructureMappingBlueprint.builder()
                    .serviceId(warehouse.getUuid())
                    .deploymentType(SSH)
                    .cloudProviderType(AWS)
                    .properties(asList(NameValuePair.builder().name("region").value("${terraform.region}").build(),
                        NameValuePair.builder().name("securityGroups").value("${terraform.security_group}").build(),
                        NameValuePair.builder().name("tags").value("${terraform.warehouse_tags}").build()))
                    .build()))
            .build();

    return ensureInfrastructureProvisioner(seed, owners, terraformInfrastructureProvisioner);
  }

  public InfrastructureProvisioner ensureRandom(Randomizer.Seed seed) {
    return ensureRandom(seed, ownerManager.create());
  }

  public InfrastructureProvisioner ensureRandom(Randomizer.Seed seed, Owners owners) {
    EnhancedRandom random = Randomizer.instance(seed);
    InfrastructureProvisioners predefined = random.nextObject(InfrastructureProvisioners.class);
    return ensurePredefined(seed, owners, predefined);
  }

  public InfrastructureProvisioner exists(InfrastructureProvisioner infrastructureProvisioner) {
    return wingsPersistence.createQuery(InfrastructureProvisioner.class)
        .filter(InfrastructureProvisioner.APP_ID_KEY, infrastructureProvisioner.getAppId())
        .filter(InfrastructureProvisioner.NAME_KEY, infrastructureProvisioner.getName())
        .get();
  }

  public InfrastructureProvisioner ensureInfrastructureProvisioner(
      Randomizer.Seed seed, Owners owners, InfrastructureProvisioner infrastructureProvisioner) {
    EnhancedRandom random = Randomizer.instance(seed);

    InfrastructureProvisionerType infrastructureProvisionerType;

    if (infrastructureProvisioner != null && infrastructureProvisioner.getInfrastructureProvisionerType() != null) {
      infrastructureProvisionerType =
          InfrastructureProvisionerType.valueOf(infrastructureProvisioner.getInfrastructureProvisionerType());
    } else {
      infrastructureProvisionerType = random.nextObject(InfrastructureProvisionerType.class);
    }

    InfrastructureProvisioner newInfrastructureProvisioner = null;
    switch (infrastructureProvisionerType) {
      case TERRAFORM: {
        final TerraformInfrastructureProvisioner terraformInfrastructureProvisioner =
            (TerraformInfrastructureProvisioner) infrastructureProvisioner;
        final TerraformInfrastructureProvisionerBuilder builder = TerraformInfrastructureProvisioner.builder();

        if (infrastructureProvisioner != null && infrastructureProvisioner.getAppId() != null) {
          builder.appId(infrastructureProvisioner.getAppId());
        } else {
          Application application = owners.obtainApplication();
          if (application == null) {
            application = applicationGenerator.ensureRandom(seed, owners);
          }
          builder.appId(application.getUuid());
        }

        if (infrastructureProvisioner != null && infrastructureProvisioner.getName() != null) {
          builder.name(infrastructureProvisioner.getName());
        } else {
          throw new UnsupportedOperationException();
        }

        InfrastructureProvisioner existing = exists(builder.build());
        if (existing != null) {
          return existing;
        }

        if (infrastructureProvisioner != null && infrastructureProvisioner.getMappingBlueprints() != null) {
          builder.mappingBlueprints(infrastructureProvisioner.getMappingBlueprints());
        } else {
          throw new UnsupportedOperationException();
        }

        if (terraformInfrastructureProvisioner != null
            && terraformInfrastructureProvisioner.getSourceRepoSettingId() != null) {
          builder.sourceRepoSettingId(terraformInfrastructureProvisioner.getSourceRepoSettingId());
        } else {
          final SettingAttribute settingAttribute =
              settingGenerator.ensurePredefined(seed, Settings.TERRAFORM_TEST_GIT_REPO);
          builder.sourceRepoSettingId(settingAttribute.getUuid());
        }

        if (terraformInfrastructureProvisioner != null && terraformInfrastructureProvisioner.getVariables() != null) {
          builder.variables(terraformInfrastructureProvisioner.getVariables());
        }

        newInfrastructureProvisioner = builder.build();
        break;
      }
      default: {
        unhandled(infrastructureProvisionerType);
        throw new UnsupportedOperationException();
      }
    }
    return infrastructureProvisionerService.save(newInfrastructureProvisioner);
  }
}