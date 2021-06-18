package io.harness.accesscontrol.principals.users.migration;

import static io.harness.accesscontrol.principals.PrincipalType.USER;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.accesscontrol.principals.users.User;
import io.harness.accesscontrol.principals.users.UserService;
import io.harness.accesscontrol.roleassignments.RoleAssignment;
import io.harness.accesscontrol.roleassignments.RoleAssignmentFilter;
import io.harness.accesscontrol.roleassignments.RoleAssignmentService;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.lock.AcquiredLock;
import io.harness.lock.PersistentLocker;
import io.harness.ng.beans.PageRequest;
import io.harness.ng.beans.PageResponse;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
@OwnedBy(HarnessTeam.PL)
@Singleton
public class UserBootstrapMigrationJob implements Runnable {
  private final UserService userService;
  private final RoleAssignmentService roleAssignmentService;
  private final PersistentLocker persistentLocker;
  @Named("mongoTemplate") private final MongoTemplate mongoTemplate;

  private static final String ACCESS_CONTROL_USER_BOOTSTRAP_MIGRATION_LOCK =
      "ACCESS_CONTROL_USER_BOOTSTRAP_MIGRATION_LOCK";

  @Override
  public void run() {
    try (AcquiredLock<?> acquiredLock = acquireLock()) {
      if (acquiredLock == null || fetchUserBootstrapMigrationState().isPresent()) {
        return;
      }
      UserBootstrapMigrationState userBootstrapMigrationState = saveRunningState();
      boolean successfullyMigrated;
      try {
        migrate();
        successfullyMigrated = true;
      } catch (Exception e) {
        log.error("User bootstrap migration failed due to error", e);
        successfullyMigrated = false;
      }
      if (successfullyMigrated) {
        userBootstrapMigrationState = updateStateToCompleted(userBootstrapMigrationState);
      } else {
        deleteUserMigrationState(userBootstrapMigrationState);
      }
    }
  }

  private void migrate() {
    int pageSize = 100;
    int pageIndex = 0;
    long totalPages;
    Set<User> userSet = new HashSet<>();
    do {
      PageResponse<RoleAssignment> roleAssignmentsPage =
          roleAssignmentService.list(PageRequest.builder().pageIndex(pageIndex).pageSize(pageSize).build(),
              RoleAssignmentFilter.builder()
                  .scopeFilter("/")
                  .includeChildScopes(true)
                  .principalTypeFilter(Sets.newHashSet(USER))
                  .build());
      pageIndex++;
      totalPages = roleAssignmentsPage.getTotalPages();

      List<RoleAssignment> roleAssignmentList = roleAssignmentsPage.getContent();
      if (isNotEmpty(roleAssignmentList)) {
        for (RoleAssignment roleAssignment : roleAssignmentList) {
          User user = User.builder()
                          .scopeIdentifier(roleAssignment.getScopeIdentifier())
                          .identifier(roleAssignment.getPrincipalIdentifier())
                          .build();
          userSet.add(user);
        }
      }

      if (userSet.size() >= 50) {
        userService.saveAll(new ArrayList<>(userSet));
        userSet = new HashSet<>();
      }
    } while (pageIndex < totalPages);

    userService.saveAll(new ArrayList<>(userSet));
  }

  private AcquiredLock<?> acquireLock() {
    AcquiredLock<?> lock = null;
    do {
      try {
        TimeUnit.MILLISECONDS.sleep(30000);
        log.info("Trying to acquire ACCESS_CONTROL_USER_BOOTSTRAP_MIGRATION lock with 5 seconds timeout...");
        lock = persistentLocker.tryToAcquireInfiniteLockWithPeriodicRefresh(
            ACCESS_CONTROL_USER_BOOTSTRAP_MIGRATION_LOCK, Duration.ofSeconds(5));
      } catch (InterruptedException e) {
        log.info("Interrupted while trying to get ACCESS_CONTROL_USER_BOOTSTRAP_MIGRATION lock. Exiting");
        Thread.currentThread().interrupt();
        return null;
      } catch (Exception ex) {
        log.info("Unable to get ACCESS_CONTROL_USER_BOOTSTRAP_MIGRATION lock, going to sleep for 30 seconds...");
      }
    } while (lock == null);
    return lock;
  }

  private Optional<UserBootstrapMigrationState> fetchUserBootstrapMigrationState() {
    return Optional.ofNullable(mongoTemplate.findOne(new Query(), UserBootstrapMigrationState.class));
  }

  private UserBootstrapMigrationState saveRunningState() {
    return mongoTemplate.save(UserBootstrapMigrationState.builder().status("Running").build());
  }

  private UserBootstrapMigrationState updateStateToCompleted(UserBootstrapMigrationState userBootstrapMigrationState) {
    return mongoTemplate.save(UserBootstrapMigrationState.builder()
                                  .status("Completed")
                                  .id(userBootstrapMigrationState.getId())
                                  .createdAt(userBootstrapMigrationState.getCreatedAt())
                                  .lastModifiedAt(userBootstrapMigrationState.getLastModifiedAt())
                                  .version(userBootstrapMigrationState.getVersion())
                                  .build());
  }

  private void deleteUserMigrationState(UserBootstrapMigrationState userBootstrapMigrationState) {
    mongoTemplate.remove(userBootstrapMigrationState);
  }
}
