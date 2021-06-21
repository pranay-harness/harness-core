package io.harness.ng.accesscontrol.migrations;

import io.harness.NGConstants;
import io.harness.lock.AcquiredLock;
import io.harness.lock.PersistentLocker;
import io.harness.ng.accesscontrol.migrations.services.AccessControlMigrationService;
import io.harness.ng.core.entities.Organization;
import io.harness.ng.core.services.OrganizationService;

import com.google.inject.Inject;
import io.dropwizard.lifecycle.Managed;
import java.time.Duration;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

@Slf4j
@AllArgsConstructor(onConstructor = @__({ @Inject }))
public class AccessControlMigrationJob implements Managed {
  private final PersistentLocker persistentLocker;
  private final OrganizationService organizationService;
  private final AccessControlMigrationService migrationService;
  private static final String ACCESS_CONTROL_MIGRATION_LOCK = "ACCESS_CONTROL_MIGRATION_LOCK";

  @Override
  public void start() throws Exception {
    AcquiredLock<?> migrationLock = null;

    while (migrationLock == null) {
      try {
        log.info("Trying to acquire ACCESS_CONTROL_MIGRATION_LOCK lock with 5 seconds timeout...");
        migrationLock = persistentLocker.tryToAcquireInfiniteLockWithPeriodicRefresh(
            ACCESS_CONTROL_MIGRATION_LOCK, Duration.ofSeconds(5));
      } catch (Exception ex) {
        log.info("Unable to get ACCESS_CONTROL_MIGRATION_LOCK lock, going to sleep for 30 seconds...");
      }
      try {
        Thread.sleep(30000);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        return;
      }
    }

    log.info("Acquired ACCESS_CONTROL_MIGRATION_LOCK lock, starting migration now...");

    try {
      Page<String> accountsWithDefaultOrganization =
          organizationService
              .list(Criteria.where(Organization.OrganizationKeys.identifier).is(NGConstants.DEFAULT_ORG_IDENTIFIER),
                  Pageable.unpaged())
              .map(Organization::getAccountIdentifier);

      for (String accountIdentifier : accountsWithDefaultOrganization) {
        migrationService.migrate(accountIdentifier);
      }

    } finally {
      log.info("Migration finished, releasing lock now...");
      migrationLock.release();
    }
  }

  @Override
  public void stop() throws Exception {}
}
