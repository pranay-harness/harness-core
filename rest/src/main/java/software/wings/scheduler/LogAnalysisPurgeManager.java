package software.wings.scheduler;

import com.google.inject.Singleton;

import software.wings.service.intfc.analysis.AnalysisService;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by rsingh on 8/18/17.
 */
@Singleton
public class LogAnalysisPurgeManager {
  private final AnalysisService analysisService;

  public LogAnalysisPurgeManager(AnalysisService analysisService) {
    this.analysisService = analysisService;
  }

  public void startArchival() {
    final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
    scheduledExecutorService.scheduleAtFixedRate(
        () -> { analysisService.purgeLogs(); }, 15, TimeUnit.DAYS.toMinutes(1), TimeUnit.MINUTES);
  }
}
