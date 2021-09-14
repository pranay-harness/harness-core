/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.licensing.migrations.licenses;

import io.harness.ModuleType;
import io.harness.licensing.beans.modules.types.CDLicenseType;
import io.harness.licensing.entities.modules.CDModuleLicense;
import io.harness.licensing.entities.modules.ModuleLicense.ModuleLicenseKeys;
import io.harness.migration.NGMigration;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.util.CloseableIterator;

@Slf4j
public class ModuleLicenseMigration implements NGMigration {
  @Inject MongoTemplate mongoTemplate;

  @Override
  public void migrate() {
    log.info("Starting the module license migration");
    Query query = new Query(Criteria.where(ModuleLicenseKeys.moduleType).is(ModuleType.CD));
    try (CloseableIterator<CDModuleLicense> iterator = mongoTemplate.stream(query, CDModuleLicense.class)) {
      while (iterator.hasNext()) {
        CDModuleLicense license = iterator.next();
        if (license.getCdLicenseType() == null) {
          license.setCdLicenseType(CDLicenseType.SERVICES);
          try {
            mongoTemplate.save(license);
          } catch (Exception e) {
            log.info("Failed to set CDLicenseType for license {}", license.getId());
          }
          log.info("Successfully set CDLicenseType to SERVICES for licenseId={}", license.getId());
        }
      }
    }
    log.info("Finished the module license migration");
  }
}
