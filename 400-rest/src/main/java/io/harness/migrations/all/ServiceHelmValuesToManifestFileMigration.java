package io.harness.migrations.all;

import static io.harness.data.structure.HasPredicate.hasNone;
import static io.harness.data.structure.HasPredicate.hasSome;
import static io.harness.persistence.HQuery.excludeAuthority;

import static software.wings.beans.InfrastructureMapping.SERVICE_ID_KEY;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
import io.harness.migrations.Migration;

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

import com.google.inject.Inject;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@TargetModule(HarnessModule._390_DB_MIGRATION)
public class ServiceHelmValuesToManifestFileMigration implements Migration {
  private static final String HELM_VALUE_YAML_KEY = "helmValueYaml";
  private static final String DEPLOYMENT_TYPE_KEY = "deploymentType";

  @Inject private WingsPersistence wingsPersistence;
  @Inject private ServiceResourceService serviceResourceService;
  @Inject private ApplicationManifestService applicationManifestService;

  @Override
  public void migrate() {
    log.info("Running ServiceHelmValuesToManifestFileMigration");

    migrateHelmValuesInServices();

    log.info("Completed ServiceHelmValuesToManifestFileMigration");
  }

  private void migrateHelmValuesInServices() {
    log.info("Migrating service helm values");

    List<Service> services = wingsPersistence.createQuery(Service.class, excludeAuthority)
                                 .filter(ServiceKeys.artifactType, "DOCKER")
                                 .field(HELM_VALUE_YAML_KEY)
                                 .exists()
                                 .asList();

    if (hasNone(services)) {
      log.info("Completed migrating service helm values. No service with helm values found");
      return;
    }

    log.info("Found {} services", services.size());
    int migratedServices = 0;

    for (Service service : services) {
      boolean migrationNeeded = false;

      if (DeploymentType.HELM == service.getDeploymentType()
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

    log.info("Migrated {} services", migratedServices);
    log.info("Completed migrating service helm values");
  }

  private boolean isHelmDeploymentTypeInfraMappingPresent(String serviceId) {
    List<InfrastructureMapping> infrastructureMappings =
        wingsPersistence.createQuery(InfrastructureMapping.class, excludeAuthority)
            .filter(DEPLOYMENT_TYPE_KEY, DeploymentType.HELM.name())
            .filter(SERVICE_ID_KEY, serviceId)
            .asList();

    return hasSome(infrastructureMappings);
  }
}
