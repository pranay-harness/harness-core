package migrations.all;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.persistence.HQuery.excludeAuthority;
import static software.wings.beans.InfrastructureMapping.SERVICE_ID_KEY;

import com.google.inject.Inject;

import lombok.extern.slf4j.Slf4j;
import migrations.Migration;
import software.wings.api.DeploymentType;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.Service;
import software.wings.beans.Service.ServiceKeys;
import software.wings.beans.appmanifest.AppManifestKind;
import software.wings.beans.appmanifest.ApplicationManifest;
import software.wings.beans.appmanifest.ManifestFile;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.ApplicationManifestService;
import software.wings.service.intfc.ServiceResourceService;

import java.util.List;

@Slf4j
public class ServiceHelmValuesToManifestFileMigration implements Migration {
  private static final String HELM_VALUE_YAML_KEY = "helmValueYaml";
  private static final String DEPLOYMENT_TYPE_KEY = "deploymentType";

  @Inject private WingsPersistence wingsPersistence;
  @Inject private ServiceResourceService serviceResourceService;
  @Inject private ApplicationManifestService applicationManifestService;

  @Override
  public void migrate() {
    logger.info("Running ServiceHelmValuesToManifestFileMigration");

    migrateHelmValuesInServices();

    logger.info("Completed ServiceHelmValuesToManifestFileMigration");
  }

  private void migrateHelmValuesInServices() {
    logger.info("Migrating service helm values");

    List<Service> services = wingsPersistence.createQuery(Service.class, excludeAuthority)
                                 .filter(ServiceKeys.artifactType, "DOCKER")
                                 .field(HELM_VALUE_YAML_KEY)
                                 .exists()
                                 .asList();

    if (isEmpty(services)) {
      logger.info("Completed migrating service helm values. No service with helm values found");
      return;
    }

    logger.info("Found {} services", services.size());
    int migratedServices = 0;

    for (Service service : services) {
      boolean migrationNeeded = false;

      if (DeploymentType.HELM.equals(service.getDeploymentType())
          || (service.getDeploymentType() == null && isHelmDeploymentTypeInfraMappingPresent(service.getUuid()))) {
        migrationNeeded = true;
      }

      if (migrationNeeded) {
        ApplicationManifest applicationManifest =
            applicationManifestService.getByServiceId(service.getAppId(), service.getUuid(), AppManifestKind.VALUES);

        if (applicationManifest == null) {
          ManifestFile manifestFile = ManifestFile.builder().fileContent(service.getHelmValueYaml()).build();
          serviceResourceService.createValuesYaml(service.getAppId(), service.getUuid(), manifestFile);
          migratedServices++;
        }
      }
    }

    logger.info("Migrated {} services", migratedServices);
    logger.info("Completed migrating service helm values");
  }

  private boolean isHelmDeploymentTypeInfraMappingPresent(String serviceId) {
    List<InfrastructureMapping> infrastructureMappings =
        wingsPersistence.createQuery(InfrastructureMapping.class, excludeAuthority)
            .filter(DEPLOYMENT_TYPE_KEY, DeploymentType.HELM.name())
            .filter(SERVICE_ID_KEY, serviceId)
            .asList();

    return isNotEmpty(infrastructureMappings);
  }
}
