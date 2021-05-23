package io.harness.metrics.service.impl;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.metrics.MetricConstants.ENV_LABEL;
import static io.harness.metrics.MetricConstants.METRIC_LABEL_PREFIX;

import io.harness.metrics.HarnessMetricRegistry;
import io.harness.metrics.beans.MetricConfiguration;
import io.harness.metrics.beans.MetricGroup;
import io.harness.metrics.service.api.MetricDefinitionInitializer;
import io.harness.metrics.service.api.MetricService;
import io.harness.serializer.YamlUtils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import com.google.inject.Inject;
import io.opencensus.common.Duration;
import io.opencensus.common.Scope;
import io.opencensus.exporter.stats.stackdriver.StackdriverStatsConfiguration;
import io.opencensus.exporter.stats.stackdriver.StackdriverStatsExporter;
import io.opencensus.stats.Measure.MeasureDouble;
import io.opencensus.stats.Stats;
import io.opencensus.stats.StatsRecorder;
import io.opencensus.stats.View;
import io.opencensus.stats.ViewManager;
import io.opencensus.tags.TagContext;
import io.opencensus.tags.TagContextBuilder;
import io.opencensus.tags.TagKey;
import io.opencensus.tags.TagValue;
import io.opencensus.tags.Tagger;
import io.opencensus.tags.Tags;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.ThreadContext;
import org.reflections.Reflections;
import org.reflections.scanners.ResourcesScanner;

@Slf4j
public class MetricServiceImpl implements MetricService {
  @Inject private HarnessMetricRegistry harnessMetricRegistry;
  private static boolean WILL_PUBLISH_METRICS;

  private static Map<String, MetricGroup> metricGroupMap = new HashMap<>();
  private static List<MetricConfiguration> metricConfigDefinitions = new ArrayList<>();

  private static final Tagger tagger = Tags.getTagger();
  private static final StatsRecorder statsRecorder = Stats.getStatsRecorder();

  private static void recordTaggedStat(Map<TagKey, String> tags, MeasureDouble md, Double d) {
    TagContextBuilder contextBuilder = tagger.emptyBuilder();
    tags.forEach((tag, val) -> contextBuilder.put(tag, TagValue.create(val)));
    TagContext tctx = contextBuilder.build();

    try (Scope ss = tagger.withTagContext(tctx)) {
      statsRecorder.newMeasureMap().put(md, d).record(tctx);
      log.info("Recorded metric to stackdriver");
    }
  }

  private void fetchAndInitMetricDefinitions(List<MetricDefinitionInitializer> metricDefinitionInitializers) {
    Reflections reflections = new Reflections("metrics.metricDefinitions", new ResourcesScanner());
    Set<String> metricDefinitionFileNames = reflections.getResources(Pattern.compile(".*\\.yaml"));
    metricDefinitionInitializers.forEach(metricDefinitionInitializer -> {
      metricConfigDefinitions.addAll(metricDefinitionInitializer.getMetricConfiguration());
    });
    metricDefinitionFileNames.forEach(metricDefinition -> {
      try {
        String path = "/" + metricDefinition;
        final String yaml = Resources.toString(MetricServiceImpl.class.getResource(path), Charsets.UTF_8);
        YamlUtils yamlUtils = new YamlUtils();
        final MetricConfiguration metricConfiguration =
            yamlUtils.read(yaml, new TypeReference<MetricConfiguration>() {});
        metricConfigDefinitions.add(metricConfiguration);
      } catch (IOException e) {
        throw new IllegalStateException("Error reading metric definition files", e);
      } catch (Exception ex) {
        throw new RuntimeException("Exception occured while reading metric definition files", ex);
      }
    });
    reflections = new Reflections("metrics.metricGroups", new ResourcesScanner());
    Set<String> metricGroupFileNames = reflections.getResources(Pattern.compile(".*\\.yaml"));
    metricGroupFileNames.forEach(name -> {
      try {
        String path = "/" + name;
        final String yaml = Resources.toString(MetricServiceImpl.class.getResource(path), Charsets.UTF_8);
        YamlUtils yamlUtils = new YamlUtils();
        final MetricGroup metricGroup = yamlUtils.read(yaml, new TypeReference<MetricGroup>() {});
        metricGroupMap.put(metricGroup.getIdentifier(), metricGroup);
      } catch (IOException e) {
        throw new IllegalStateException("Error reading metric group file", e);
      }
    });
  }
  @Override
  public void initializeMetrics() {
    initializeMetrics(new ArrayList<>());
  }
  @Override
  public void initializeMetrics(List<MetricDefinitionInitializer> metricDefinitionInitializes) {
    if (isNotEmpty(System.getenv("GOOGLE_APPLICATION_CREDENTIALS"))) {
      WILL_PUBLISH_METRICS = true;
    }

    if (!WILL_PUBLISH_METRICS) {
      log.warn("Google credentials have not been set. No metrics will be published.");
      return;
    }
    fetchAndInitMetricDefinitions(metricDefinitionInitializes);
    metricConfigDefinitions.forEach(metricConfigDefinition -> {
      List<String> labels = metricGroupMap.get(metricConfigDefinition.getMetricGroup()) == null
          ? new ArrayList<>()
          : metricGroupMap.get(metricConfigDefinition.getMetricGroup()).getLabels();
      labels.add(ENV_LABEL);
      metricConfigDefinition.getMetrics().forEach(metric -> {
        List<TagKey> tagKeys = new ArrayList<>();
        labels.forEach(label -> tagKeys.add(TagKey.create(label)));

        View view = metric.getView(tagKeys);
        ViewManager vmgr = Stats.getViewManager();
        vmgr.registerView(view);
      });
    });
    try {
      StackdriverStatsConfiguration configuration =
          StackdriverStatsConfiguration.builder()
              .setExportInterval(Duration.fromMillis(TimeUnit.MINUTES.toMillis(1)))
              .setDeadline(Duration.fromMillis(TimeUnit.MINUTES.toMillis(5)))
              .build();

      StackdriverStatsExporter.createAndRegister(configuration);
    } catch (Exception ex) {
      log.error("Exception while trying to register stackdriver metrics exporter", ex);
    }
  }

  @Override
  public void recordMetric(String metricName, double value) {
    try {
      if (!WILL_PUBLISH_METRICS) {
        log.error("Credentials to APM not set. We will not be able to publish metrics");
        return;
      }

      MetricConfiguration metricConfiguration = null;
      for (MetricConfiguration configDefinition : metricConfigDefinitions) {
        if (configDefinition.getMetrics()
                .stream()
                .map(MetricConfiguration.Metric::getMetricName)
                .collect(Collectors.toList())
                .contains(metricName)) {
          metricConfiguration = configDefinition;
          break;
        }
      }
      if (metricConfiguration == null) {
        throw new IllegalStateException("Unknown metric name while trying to record metrics :" + metricName);
      }
      MetricConfiguration.Metric cvngMetric = metricConfiguration.getMetrics()
                                                  .stream()
                                                  .filter(metric -> metric.getMetricName().equals(metricName))
                                                  .findFirst()
                                                  .get();

      MetricGroup group = metricGroupMap.get(metricConfiguration.getMetricGroup());
      List<String> labelNames =
          group == null || group.getLabels() == null ? Arrays.asList(ENV_LABEL) : group.getLabels();
      List<String> labelVals = group == null ? new ArrayList<>() : getLabelValues(labelNames);
      Map<TagKey, String> tagsMap = new HashMap<>();
      for (int index = 0; index < labelVals.size(); index++) {
        tagsMap.put(TagKey.create(labelNames.get(index)), labelVals.get(index));
      }

      recordTaggedStat(tagsMap, cvngMetric.getMeasure(), value);
    } catch (Exception ex) {
      log.error("Exception occurred while registering a metric", ex);
    }
  }

  private List<String> getLabelValues(List<String> labelNames) {
    Map<String, String> context = ThreadContext.getContext();

    String env = System.getenv("ENV");
    if (isEmpty(env)) {
      env = "localhost";
    }

    List<String> labelValues = new ArrayList<>();
    labelNames.forEach(label -> {
      String tagKey = METRIC_LABEL_PREFIX + label;
      String tagVal = context.containsKey(tagKey) ? context.get(tagKey) : null;
      if (tagVal != null) {
        labelValues.add(tagVal);
      }
    });

    labelValues.add(env);

    if (labelNames.size() != labelValues.size()) {
      throw new IllegalStateException(
          "Some labels were not found from the object while trying to record metric. Label Names: " + labelNames
          + " and labels: " + labelValues);
    }
    return labelValues;
  }
}
