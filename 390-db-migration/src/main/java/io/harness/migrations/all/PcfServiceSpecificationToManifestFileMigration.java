/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.migrations.all;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import static org.apache.commons.lang3.StringUtils.isBlank;

import io.harness.annotations.dev.OwnedBy;
import io.harness.migrations.Migration;
import io.harness.persistence.HIterator;

import software.wings.beans.Service;
import software.wings.beans.appmanifest.AppManifestKind;
import software.wings.beans.appmanifest.ApplicationManifest;
import software.wings.beans.appmanifest.ManifestFile;
import software.wings.beans.container.PcfServiceSpecification;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.ApplicationManifestService;
import software.wings.service.intfc.ServiceResourceService;

import com.google.inject.Inject;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(CDP)
public class PcfServiceSpecificationToManifestFileMigration implements Migration {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private ServiceResourceService serviceResourceService;
  @Inject private ApplicationManifestService applicationManifestService;

  @Override
  public void migrate() {
    log.info("Retrieving PCF Services");

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
          log.warn(errorMsg.toString());
          serviceResourceService.createDefaultPcfV2Manifests(service);
        } else {
          serviceResourceService.upsertPCFSpecInManifestFile(pcfServiceSpecification);
        }
      }
    }
  }
}
