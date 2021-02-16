package io.harness.cvng.statemachine.jobs;

import io.harness.cvng.core.entities.CVConfig;
import io.harness.cvng.statemachine.services.intfc.OrchestrationService;
import io.harness.mongo.iterator.MongoPersistenceIterator.Handler;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.opencensus.exporter.stats.stackdriver.StackdriverStatsExporter;
import io.opencensus.stats.Aggregation;
import io.opencensus.stats.Aggregation.Distribution;
import io.opencensus.stats.BucketBoundaries;
import io.opencensus.stats.Measure.MeasureDouble;
import io.opencensus.stats.Stats;
import io.opencensus.stats.StatsRecorder;
import io.opencensus.stats.View;
import io.opencensus.stats.View.Name;
import io.opencensus.stats.ViewManager;
import io.opencensus.tags.TagKey;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import lombok.extern.slf4j.Slf4j;
@Singleton
@Slf4j
public class AnalysisOrchestrationJob implements Handler<CVConfig> {
  @Inject private OrchestrationService orchestrationService;
  // The task latency in milliseconds.
  private static final MeasureDouble LATENCY_MS =
      MeasureDouble.create("latency_test", "The task latency in milliseconds", "ms");
  private static final StatsRecorder statsRecorder = Stats.getStatsRecorder();

  @Override
  public void handle(CVConfig entity) {
    // try {
    //   // Defining the distribution aggregations
    //   Aggregation latencyDistribution =
    //           Aggregation.Count.create();
    //
    //  //           Distribution.create(
    //  //                   BucketBoundaries.create(
    //  //                           Arrays.asList(
    //  //                                   // Latency in buckets: [>=0ms, >=100ms, >=200ms, >=400ms, >=1s, >=2s, >=4s]
    //  //                                   0.0, 100.0, 200.0, 400.0, 1000.0, 2000.0, 4000.0)));
    //   View view =
    //           View.create(
    //                   Name.create("task_latency_value"),
    //                   "The count of the task latencies",
    //                   LATENCY_MS,
    //                   latencyDistribution,
    //                   Collections.<TagKey>emptyList());
    //
    //
    //
    //   // Create the view manager
    //   ViewManager vmgr = Stats.getViewManager();
    //
    //   // Then finally register the views
    //   vmgr.registerView(view);
    //
    //   // Enable OpenCensus exporters to export metrics to Stackdriver Monitoring.
    //   // Exporters use Application Default Credentials to authenticate.
    //   // See https://developers.google.com/identity/protocols/application-default-credentials
    //   // for more details.
    //   // The minimum reporting period for Stackdriver is 1 minute.
    //   StackdriverStatsExporter.createAndRegister();
    //   // Record 100 fake latency values between 0 and 5 seconds.
    //   for (int i = 0; i < 100; i++) {
    //     double ms = Math.random() * 5 * 1000;
    //     System.out.println(String.format("Latency %d: %f", i, ms));
    //     statsRecorder.newMeasureMap().put(LATENCY_MS, ms).record();
    //     Thread.sleep(1000);
    //   }
    //
    //   // Wait for a duration longer than reporting duration (1min) to ensure all stats are exported.
    //   System.out.println("Done recording metrics");
    // } catch (Exception e) {
    //  System.out.println(e);
    // }
    orchestrationService.orchestrate(entity.getUuid());
  }
}
