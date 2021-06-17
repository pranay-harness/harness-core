package io.harness.licensing.migration.tasks;

import static org.springframework.data.mongodb.core.query.Query.query;

import io.harness.licensing.entities.account.AccountLicense;
import io.harness.licensing.entities.modules.CDModuleLicense;
import io.harness.licensing.entities.modules.CFModuleLicense;
import io.harness.licensing.entities.modules.CIModuleLicense;
import io.harness.licensing.entities.modules.ModuleLicense;
import io.harness.licensing.entities.transactions.LicenseTransaction;
import io.harness.licensing.entities.transactions.modules.CDLicenseTransaction;
import io.harness.licensing.entities.transactions.modules.CELicenseTransaction;
import io.harness.licensing.entities.transactions.modules.CFLicenseTransaction;
import io.harness.licensing.entities.transactions.modules.CILicenseTransaction;
import io.harness.licensing.entities.transactions.modules.CVLicenseTransaction;
import io.harness.migration.NGMigration;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCursor;
import java.util.HashMap;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;

@Slf4j
public class ModuleToAccountMigration implements NGMigration {
  @Inject MongoTemplate mongoTemplate;
  @Override
  public void migrate() {
    FindIterable<ModuleLicense> findIterable = mongoTemplate.getCollection("moduleLicenses").find(ModuleLicense.class);
    MongoCursor<ModuleLicense> cursor = findIterable.cursor();
    while (cursor.hasNext()) {
      ModuleLicense moduleLicense = cursor.next();
      AccountLicense accountLicense = mongoTemplate.findOne(
          query(Criteria.where("accountIdentifier").is(moduleLicense.getAccountIdentifier())), AccountLicense.class);
      if (accountLicense == null) {
        accountLicense = AccountLicense.builder()
                             .accountIdentifier(moduleLicense.getAccountIdentifier())
                             .moduleLicenses(new HashMap<>())
                             .licenseCheckIteration(0L)
                             .build();
      }
      try {
        migrateInfoToTransaction(moduleLicense);
      } catch (IllegalArgumentException e) {
        log.error("Failed to migrate moduleLicense" + moduleLicense.getId(), e);
        continue;
      }
      accountLicense.getModuleLicenses().put(moduleLicense.getModuleType(), moduleLicense);
      mongoTemplate.save(accountLicense);
      log.info("Proceed migration on moduleLicense {}", moduleLicense.getId());
    }
  }

  private void migrateInfoToTransaction(ModuleLicense moduleLicense) {
    LicenseTransaction transaction;
    switch (moduleLicense.getModuleType()) {
      case CI:
        CIModuleLicense ciModuleLicense = (CIModuleLicense) moduleLicense;
        ciModuleLicense.setTotalDevelopers(100);
        ciModuleLicense.setMaxDevelopers(-1);
        transaction = CILicenseTransaction.builder().developers(100).build();
        break;
      case CD:
        CDModuleLicense cdModuleLicense = (CDModuleLicense) moduleLicense;
        cdModuleLicense.setTotalWorkloads(100);
        cdModuleLicense.setMaxWorkloads(-1);
        transaction = CDLicenseTransaction.builder().workload(100).build();
        break;
      case CF:
        CFModuleLicense cfModuleLicense = (CFModuleLicense) moduleLicense;
        cfModuleLicense.setTotalFeatureFlagUnits(50);
        cfModuleLicense.setTotalClientMAUs(1000000L);
        cfModuleLicense.setMaxFeatureFlagUnits(-1);
        cfModuleLicense.setMaxClientMAUs(-1L);
        transaction = CFLicenseTransaction.builder().featureFlagUnit(50).clientMAU(1000000L).build();
        break;
      case CE:
        transaction = CELicenseTransaction.builder().build();
        break;
      case CV:
        transaction = CVLicenseTransaction.builder().build();
        break;
      default:
        throw new IllegalArgumentException(
            String.format("Invalid module type %s, need to migrate mannually", moduleLicense.getModuleType()));
    }
    transaction.setUuid(ObjectId.get().toHexString());
    transaction.setEdition(moduleLicense.getEdition());
    transaction.setAccountIdentifier(moduleLicense.getAccountIdentifier());
    transaction.setLicenseType(moduleLicense.getLicenseType());
    transaction.setStatus(moduleLicense.getStatus());
    transaction.setStartTime(moduleLicense.getCreatedAt());
    transaction.setExpiryTime(moduleLicense.getExpiryTime());

    moduleLicense.setTransaction(Lists.newArrayList(transaction));
  }
}
