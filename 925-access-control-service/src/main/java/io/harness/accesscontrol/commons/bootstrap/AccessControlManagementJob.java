/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.accesscontrol.commons.bootstrap;

import io.harness.accesscontrol.permissions.PermissionsManagementJob;
import io.harness.accesscontrol.resources.resourcetypes.ResourceTypeManagementJob;
import io.harness.accesscontrol.roles.RolesManagementJob;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.lock.AcquiredLock;
import io.harness.lock.PersistentLocker;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(HarnessTeam.PL)
@Singleton
@Slf4j
public class AccessControlManagementJob {
  private final ResourceTypeManagementJob resourceTypeManagementJob;
  private final PermissionsManagementJob permissionsManagementJob;
  private final RolesManagementJob rolesManagementJob;
  private final PersistentLocker persistentLocker;
  private static final String ACCESS_CONTROL_CONFIG_MANAGEMENT_LOCK = "ACCESS_CONTROL_CONFIG_MANAGEMENT_LOCK";

  @Inject
  public AccessControlManagementJob(ResourceTypeManagementJob resourceTypeManagementJob,
      PermissionsManagementJob permissionsManagementJob, RolesManagementJob rolesManagementJob,
      PersistentLocker persistentLocker) {
    this.resourceTypeManagementJob = resourceTypeManagementJob;
    this.permissionsManagementJob = permissionsManagementJob;
    this.rolesManagementJob = rolesManagementJob;
    this.persistentLocker = persistentLocker;
  }

  public void run() {
    try (AcquiredLock<?> lock = acquireLock(ACCESS_CONTROL_CONFIG_MANAGEMENT_LOCK, true)) {
      if (lock != null) {
        resourceTypeManagementJob.run();
        permissionsManagementJob.run();
        rolesManagementJob.run();
      }
    } catch (InterruptedException e) {
      log.error(String.format("Interrupted while trying to acquire %s", ACCESS_CONTROL_CONFIG_MANAGEMENT_LOCK), e);
      Thread.currentThread().interrupt();
    } catch (IllegalMonitorStateException e) {
      log.error(String.format("Error while releasing the lock %s", ACCESS_CONTROL_CONFIG_MANAGEMENT_LOCK), e);
    }
  }

  protected AcquiredLock<?> acquireLock(String lockIdentifier, boolean retryIndefinitely) throws InterruptedException {
    AcquiredLock<?> lock = null;
    do {
      try {
        log.info("Trying to acquire {} lock with 5 seconds timeout", lockIdentifier);
        lock = persistentLocker.tryToAcquireInfiniteLockWithPeriodicRefresh(lockIdentifier, Duration.ofSeconds(5));
      } catch (Exception ex) {
        log.warn("Unable to get {} lock, due to the exception. Will retry again", lockIdentifier, ex);
      }
      if (lock == null) {
        TimeUnit.SECONDS.sleep(60);
      }
    } while (lock == null && retryIndefinitely);
    return lock;
  }
}
