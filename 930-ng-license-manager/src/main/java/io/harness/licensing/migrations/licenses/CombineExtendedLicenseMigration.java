package io.harness.licensing.migrations.licenses;

import io.harness.ModuleType;
import io.harness.licensing.entities.modules.ModuleLicense;
import io.harness.migration.NGMigration;

import com.google.inject.Inject;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

@Slf4j
public class CombineExtendedLicenseMigration implements NGMigration {
  @Inject MongoTemplate mongoTemplate;
  private ModuleType[] migrationTypes = new ModuleType[] {ModuleType.CD, ModuleType.CI, ModuleType.CF, ModuleType.CE};

  @Override
  public void migrate() {
    log.info("Starting the extended license combination migration");
    Query query = new Query();
    List<String> accountIds = mongoTemplate.findDistinct(query, "accountIdentifier", ModuleLicense.class, String.class);
    for (String accountId : accountIds) {
      log.info("Migration for license in account {} started", accountId);
      for (ModuleType moduleType : migrationTypes) {
        Query licenseQuery = new Query();
        licenseQuery.addCriteria(
            new Criteria().andOperator(Criteria.where(ModuleLicense.ModuleLicenseKeys.accountIdentifier).is(accountId),
                Criteria.where(ModuleLicense.ModuleLicenseKeys.moduleType).is(moduleType)));
        List<ModuleLicense> moduleLicenses = mongoTemplate.find(licenseQuery, ModuleLicense.class);

        if (moduleLicenses.size() > 1) {
          Optional<ModuleLicense> validLicenseOptional = moduleLicenses.stream().reduce(
              (current, compare) -> current.getExpiryTime() > compare.getExpiryTime() ? current : compare);

          // mark trial extended on the last license
          ModuleLicense validLicense = validLicenseOptional.get();
          validLicense.setExtendedTrial(true);
          mongoTemplate.save(validLicense);

          // remove the rest licenses
          for (ModuleLicense toBeRemoved : moduleLicenses) {
            if (toBeRemoved.getId().equals(validLicense.getId())) {
              continue;
            }

            mongoTemplate.remove(toBeRemoved);
          }
          log.info("Operated on license combine for account {} and moduleType {}", accountId, moduleType);
        }
      }
    }
    log.info("Finished the trial extend license combination migration");
  }
}
