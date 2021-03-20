package io.harness.migrations.all;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
import io.harness.migrations.Migration;

import software.wings.dl.WingsPersistence;
import software.wings.service.impl.ThirdPartyApiCallLog;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@TargetModule(HarnessModule._390_DB_MIGRATION)
public class DeleteOldThirdPartyApiCallsMigration implements Migration {
  @Inject private WingsPersistence wingsPersistence;

  @Override
  public void migrate() {
    try {
      wingsPersistence.delete(wingsPersistence.createQuery(ThirdPartyApiCallLog.class));
    } catch (RuntimeException ex) {
      log.error("clear collection error", ex);
    }
  }
}
