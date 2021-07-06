package io.harness.migrations.all;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.DelegateProfile;
import io.harness.migrations.Migration;
import io.harness.persistence.HIterator;
import io.harness.persistence.HPersistence;
import io.harness.persistence.HQuery;

import software.wings.service.intfc.DelegateProfileService;
import software.wings.utils.Utils;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.FindAndModifyOptions;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;

@Slf4j
@OwnedBy(HarnessTeam.DEL)
public class DelegateProfileIdentifierMigration implements Migration {
  @Inject private HPersistence persistence;
  @Inject private DelegateProfileService delegateProfileService;

  @Override
  public void migrate() {
    log.info("Starting the migration of the delegate profile identifiers.");
    Query<DelegateProfile> profilesQuery = persistence.createQuery(DelegateProfile.class, HQuery.excludeAuthority);
    try (HIterator<DelegateProfile> delegateProfiles = new HIterator<>(profilesQuery.fetch())) {
      for (DelegateProfile profile : delegateProfiles) {
        log.debug("Migrating delegate profile with uuid: ", profile.getUuid());
        updateDelegateProfile(profile);
      }
    }
    log.info("The migration for the delegates with no delegate group assigned has finished.");
  }

  private void updateDelegateProfile(DelegateProfile profile) {
    try {
      log.info("Updating delegate profile.");
      Query<DelegateProfile> profileQuery =
          persistence.createQuery(DelegateProfile.class)
              .filter(DelegateProfile.DelegateProfileKeys.uuid, profile.getUuid())
              .filter(DelegateProfile.DelegateProfileKeys.accountId, profile.getAccountId());
      UpdateOperations<DelegateProfile> updateOperations =
          persistence.createUpdateOperations(DelegateProfile.class)
              .set(DelegateProfile.DelegateProfileKeys.identifier, Utils.uuidToIdentifier(profile.getUuid()));

      persistence.findAndModify(profileQuery, updateOperations, new FindAndModifyOptions());
      log.info("Delegate profile updated successfully.");
    } catch (Exception ex) {
      log.error("Unexpected error occurred while migrating delegate profile.", ex);
    }
  }
}
