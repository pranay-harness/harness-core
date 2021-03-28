package io.harness.ng.core.user;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.invites.entities.UserMembership;
import io.harness.ng.core.invites.entities.UserProjectMap;
import io.harness.ng.core.user.services.api.NgUserService;
import io.harness.repositories.invites.spring.UserProjectMapRepository;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import io.dropwizard.lifecycle.Managed;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;

// Migrate UserProjectMap collection to UserMembership Collection
@RequiredArgsConstructor(onConstructor = @__({ @Inject }))
@OwnedBy(PL)
public class UserMembershipMigrationService implements Managed {
  private final UserProjectMapRepository userProjectMapRepository;
  private final NgUserService ngUserService;
  private final ExecutorService executorService = Executors.newSingleThreadExecutor(
      new ThreadFactoryBuilder().setNameFormat("usermembership-migration-worker-thread").build());
  private Future userMembershipMigrationJob;

  @Override
  public void start() throws Exception {
    userMembershipMigrationJob = executorService.submit(this::userMembershipMigrationJob);
  }

  @Override
  public void stop() throws Exception {
    if (userMembershipMigrationJob != null) {
      userMembershipMigrationJob.cancel(true);
    }
    executorService.shutdown();
    executorService.awaitTermination(5, TimeUnit.SECONDS);
  }

  private void userMembershipMigrationJob() {
    Optional<UserProjectMap> userProjectMapOptional = userProjectMapRepository.findByMigratedExists(false);
    while (userProjectMapOptional.isPresent()) {
      UserProjectMap userProjectMap = userProjectMapOptional.get();
      UserMembership.Scope scope = UserMembership.Scope.builder()
                                       .accountIdentifier(userProjectMap.getAccountIdentifier())
                                       .orgIdentifier(userProjectMap.getOrgIdentifier())
                                       .projectIdentifier(userProjectMap.getProjectIdentifier())
                                       .build();
      ngUserService.addUserToScope(userProjectMap.getUserId(), scope);
      userProjectMap.setMigrated(true);
      userProjectMapRepository.save(userProjectMap);
      userProjectMapOptional = userProjectMapRepository.findByMigratedExists(false);
    }
  }
}
