package migrations.all;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static org.apache.commons.lang3.StringUtils.isBlank;

import com.google.inject.Inject;

import io.harness.persistence.HIterator;
import lombok.extern.slf4j.Slf4j;
import migrations.Migration;
import software.wings.beans.Service;
import software.wings.beans.appmanifest.AppManifestKind;
import software.wings.beans.appmanifest.ApplicationManifest;
import software.wings.beans.appmanifest.ManifestFile;
import software.wings.beans.container.PcfServiceSpecification;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.ApplicationManifestService;
import software.wings.service.intfc.ServiceResourceService;

import java.util.List;

@Slf4j
public class PcfServiceSpecificationToManifestFileMigration implements Migration {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private ServiceResourceService serviceResourceService;
  @Inject private ApplicationManifestService applicationManifestService;

  @Override
  public void migrate() {
    logger.info("Retrieving PCF Services");

    try (HIterator<Service> services =
             new HIterator<>(wingsPersistence.createQuery(Service.class).filter("artifactType", "PCF").fetch())) {
      while (services.hasNext()) {
        Service service = services.next();
        // for pcfV2 services, applicationManifestFile will automatically be created.
        if (service.isPcfV2()) {
          continue;
        }

        boolean needsMigration;
        ApplicationManifest applicationManifest = applicationManifestService.getByServiceId(
            service.getAppId(), service.getUuid(), AppManifestKind.K8S_MANIFEST);
        if (applicationManifest == null) {
          needsMigration = true;
        } else {
          List<ManifestFile> manifestFiles = applicationManifestService.getManifestFilesByAppManifestId(
              applicationManifest.getAppId(), applicationManifest.getUuid());
          needsMigration = isEmpty(manifestFiles);
        }

        if (!needsMigration) {
          continue;
        }

        PcfServiceSpecification pcfServiceSpecification =
            serviceResourceService.getPcfServiceSpecification(service.getAppId(), service.getUuid());
        if (pcfServiceSpecification == null || isBlank(pcfServiceSpecification.getManifestYaml())) {
          StringBuilder errorMsg =
              new StringBuilder("Unexpected, for Older PCF service ")
                  .append(pcfServiceSpecification == null ? "PcfServiceSpecification was not present"
                                                          : "manifestYaml in PcfServiceSpecification was empty. ")
                  .append(" Resetting manifestYaml");
          logger.warn(errorMsg.toString());
          serviceResourceService.createDefaultPcfV2Manifests(service);
        } else {
          serviceResourceService.upsertPCFSpecInManifestFile(pcfServiceSpecification);
        }
      }
    }
  }
}
