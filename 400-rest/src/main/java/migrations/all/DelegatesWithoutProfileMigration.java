package migrations.all;

import io.harness.delegate.beans.DelegateProfile;
import io.harness.delegate.task.DelegateLogContext;
import io.harness.logging.AutoLogContext;
import io.harness.persistence.HIterator;

import software.wings.beans.Delegate;
import software.wings.beans.Delegate.DelegateKeys;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.DelegateProfileService;
import software.wings.service.intfc.DelegateService;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import migrations.Migration;
import org.mongodb.morphia.FindAndModifyOptions;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;

@Slf4j
public class DelegatesWithoutProfileMigration implements Migration {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private DelegateService delegateService;
  @Inject private DelegateProfileService delegateProfileService;

  @Override
  public void migrate() {
    log.info("Starting migration of delegates without profile.");

    Query<Delegate> delegatesQuery =
        wingsPersistence.createQuery(Delegate.class).field(DelegateKeys.delegateProfileId).doesNotExist();

    try (HIterator<Delegate> delegatesWithoutProfile = new HIterator<>(delegatesQuery.fetch())) {
      for (Delegate delegate : delegatesWithoutProfile) {
        try (AutoLogContext logContext =
                 new DelegateLogContext(delegate.getUuid(), AutoLogContext.OverrideBehavior.OVERRIDE_ERROR)) {
          updateDelegate(delegate);
        }
      }
    }

    log.info("Migration of delegates without profile finished.");
  }

  private void updateDelegate(Delegate delegate) {
    try {
      log.info("Fetching primary delegate profile.");
      DelegateProfile primaryProfile = delegateProfileService.fetchPrimaryProfile(delegate.getAccountId());

      log.info("Updating delegate.");
      Query<Delegate> updateQuery = wingsPersistence.createQuery(Delegate.class)
                                        .filter(DelegateKeys.accountId, delegate.getAccountId())
                                        .field(DelegateKeys.uuid)
                                        .equal(delegate.getUuid())
                                        .field(DelegateKeys.delegateProfileId)
                                        .doesNotExist();

      UpdateOperations<Delegate> updateOperations = wingsPersistence.createUpdateOperations(Delegate.class)
                                                        .set(DelegateKeys.delegateProfileId, primaryProfile.getUuid());

      wingsPersistence.findAndModify(updateQuery, updateOperations, new FindAndModifyOptions());

      log.info("Delegate updated successfully.");
    } catch (Exception ex) {
      log.error("Unexpected error occurred while processing delegate.", ex);
    }
  }
}
