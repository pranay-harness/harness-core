package io.harness.migrations.all;

import static io.harness.persistence.HQuery.excludeValidate;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;
import io.harness.migrations.Migration;

import software.wings.beans.ce.CECloudAccount;
import software.wings.beans.ce.depricated.CECloudAccountOld;
import software.wings.dl.WingsPersistence;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
@TargetModule(Module._390_DB_MIGRATION)
public class CECloudAccountMigration implements Migration {
  @Inject private WingsPersistence wingsPersistence;

  @Override
  public void migrate() {
    try {
      log.info("Starting migration of all CECloudAccounts");

      List<CECloudAccountOld> ceCloudAccountOldList =
          wingsPersistence.createQuery(CECloudAccountOld.class, excludeValidate).asList();
      List<CECloudAccount> ceCloudAccountForMigration = new ArrayList<>();
      ceCloudAccountOldList.stream()
          .filter(ceCloudAccountOld -> null != ceCloudAccountOld.getAwsCrossAccountAttributes())
          .forEach(ceCloudAccountOld
              -> ceCloudAccountForMigration.add(
                  CECloudAccount.builder()
                      .accountId(ceCloudAccountOld.getAccountId())
                      .uuid(ceCloudAccountOld.getUuid())
                      .accountArn(ceCloudAccountOld.getAccountArn())
                      .accountName(ceCloudAccountOld.getAccountName())
                      .infraAccountId(ceCloudAccountOld.getInfraAccountId())
                      .infraMasterAccountId(ceCloudAccountOld.getInfraMasterAccountId())
                      .accountStatus(getAccountStatus(ceCloudAccountOld.getAccountStatus()))
                      .masterAccountSettingId(ceCloudAccountOld.getMasterAccountSettingId())
                      .awsCrossAccountAttributes(ceCloudAccountOld.getAwsCrossAccountAttributes())
                      .createdAt(ceCloudAccountOld.getCreatedAt())
                      .lastUpdatedAt(ceCloudAccountOld.getLastUpdatedAt())
                      .build()));

      if (!ceCloudAccountForMigration.isEmpty()) {
        wingsPersistence.save(ceCloudAccountForMigration);
        log.info("Migrated cloud account size {}", ceCloudAccountForMigration.size());
      }
    } catch (Exception e) {
      log.error("Failure occurred in CECloudAccountsMigration", e);
    }
    log.info("CECloudAccounts has completed");
  }

  private CECloudAccount.AccountStatus getAccountStatus(CECloudAccountOld.AccountStatus accountStatus) {
    switch (accountStatus) {
      case CONNECTED:
        return CECloudAccount.AccountStatus.CONNECTED;
      case NOT_CONNECTED:
        return CECloudAccount.AccountStatus.NOT_CONNECTED;
      default:
        return CECloudAccount.AccountStatus.NOT_VERIFIED;
    }
  }
}
