package io.harness.accesscontrol.principals.users.migration;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.dropwizard.lifecycle.Managed;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

@OwnedBy(HarnessTeam.PL)
@Singleton
public class UserBootstrapMigrationService implements Managed {
  private final UserBootstrapMigrationJob userBootstrapMigration;
  private final ExecutorService executorService;
  private Future<?> migrationFuture;

  @Inject
  public UserBootstrapMigrationService(UserBootstrapMigrationJob userBootstrapMigration) {
    this.userBootstrapMigration = userBootstrapMigration;
    this.executorService = Executors.newSingleThreadExecutor(
        new ThreadFactoryBuilder().setNameFormat("user-bootstrap-migration-%d").build());
  }

  @Override
  public void start() throws Exception {
    if (migrationFuture == null && !executorService.isShutdown()) {
      migrationFuture = executorService.submit(userBootstrapMigration);
    }
  }

  @Override
  public void stop() throws Exception {
    if (migrationFuture != null) {
      migrationFuture.cancel(true);
    }
    executorService.shutdown();
    executorService.awaitTermination(1, TimeUnit.MINUTES);
  }
}
