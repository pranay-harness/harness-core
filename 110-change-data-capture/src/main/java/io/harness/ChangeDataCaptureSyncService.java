package io.harness;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import io.dropwizard.lifecycle.Managed;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(HarnessTeam.CE)
public class ChangeDataCaptureSyncService implements Managed {
  @Inject ChangeDataCaptureJob changeDataCaptureJob;

  private final ExecutorService executorService =
      Executors.newSingleThreadExecutor(new ThreadFactoryBuilder().setNameFormat("search-main-thread").build());
  private Future changeDataCaptureJobFuture;

  @Override
  public void start() throws Exception {
    changeDataCaptureJobFuture = executorService.submit(changeDataCaptureJob);
  }

  @Override
  public void stop() throws Exception {
    changeDataCaptureJobFuture.cancel(true);
    executorService.shutdownNow();
  }
}
