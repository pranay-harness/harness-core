package io.harness.cvng.metrics.services.impl;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.cvng.metrics.beans.CVNGMetricConfiguration;
import io.harness.cvng.metrics.beans.CVNGMetricConfiguration.CVNGMetric;
import io.harness.cvng.metrics.beans.CVNGMetricContext;
import io.harness.cvng.metrics.beans.CVNGMetricGroup;
import io.harness.cvng.metrics.services.api.MetricService;
import io.harness.logging.AutoLogContext;
import io.harness.metrics.HarnessMetricRegistry;
import io.harness.serializer.YamlUtils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.io.Resources;
import com.google.inject.Inject;
import io.opencensus.common.Scope;
import io.opencensus.exporter.stats.stackdriver.StackdriverStatsExporter;
import io.opencensus.stats.Aggregation;
import io.opencensus.stats.Measure;
import io.opencensus.stats.Measure.MeasureDouble;
import io.opencensus.stats.Measure.MeasureLong;
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
import io.prometheus.client.exporter.common.TextFormat;
import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MetricServiceImpl implements MetricService {
  @Inject private HarnessMetricRegistry harnessMetricRegistry;

  private static final String ANALYSIS_METRIC_DESCRIPTION = "metrics for analysis in cvng";
  static final List<String> METRIC_DEFINITION_FILES = Lists.newArrayList("/metrics/statemachine-metrics.yaml");
  static final List<String> METRIC_GROUP_FILES = Arrays.asList("/metrics/verification-config-group.yaml");

  static final Map<String, CVNGMetricGroup> metricGroupMap;
  static final List<CVNGMetricConfiguration> metricConfigDefinitions;

  static {
    metricGroupMap = new HashMap<>();
    metricConfigDefinitions = new ArrayList<>();
    METRIC_DEFINITION_FILES.forEach(path -> {
      try {
        final String yaml = Resources.toString(MetricServiceImpl.class.getResource(path), Charsets.UTF_8);
        YamlUtils yamlUtils = new YamlUtils();
        final CVNGMetricConfiguration metricConfiguration =
            yamlUtils.read(yaml, new TypeReference<CVNGMetricConfiguration>() {});
        metricConfigDefinitions.add(metricConfiguration);
      } catch (IOException e) {
        throw new IllegalStateException("Error reading metric packs", e);
      }
    });

    METRIC_GROUP_FILES.forEach(path -> {
      try {
        final String yaml = Resources.toString(MetricServiceImpl.class.getResource(path), Charsets.UTF_8);
        YamlUtils yamlUtils = new YamlUtils();
        final CVNGMetricGroup metricGroup = yamlUtils.read(yaml, new TypeReference<CVNGMetricGroup>() {});
        metricGroupMap.put(metricGroup.getIdentifier(), metricGroup);
      } catch (IOException e) {
        throw new IllegalStateException("Error reading metric packs", e);
      }
    });
  }

  // The latency in milliseconds
  private static final MeasureDouble M_LATENCY_MS =
      MeasureDouble.create("repl/latency", "The latency in milliseconds per REPL loop", "ms");

  // Counts/groups the lengths of lines read in.
  private static final MeasureLong M_LINE_LENGTHS =
      MeasureLong.create("repl/line_lengths", "The distribution of line lengths", "By");
  public static final TagKey ACCOUNTID_TAG = TagKey.create("accountId");
  private static final TagKey KEY_STATUS = TagKey.create("status");
  private static final TagKey KEY_ERROR = TagKey.create("error");
  private static final Tagger tagger = Tags.getTagger();
  private static final StatsRecorder statsRecorder = Stats.getStatsRecorder();

  private static void recordStat(MeasureLong ml, Long n) {
    statsRecorder.newMeasureMap().put(ml, n);
  }

  private static void recordTaggedStat(TagKey key, String value, MeasureLong ml, Long n) {
    TagContext tctx = tagger.emptyBuilder().put(key, TagValue.create(value)).build();
    try (Scope ss = tagger.withTagContext(tctx)) {
      statsRecorder.newMeasureMap().put(ml, n).record();
    }
  }

  private static void recordTaggedStat(TagKey key, String value, MeasureDouble md, Double d) {
    TagContext tctx = tagger.emptyBuilder().put(key, TagValue.create(value)).build();
    try (Scope ss = tagger.withTagContext(tctx)) {
      statsRecorder.newMeasureMap().put(md, d).record();
    }
  }

  private static void recordTaggedStat(Map<TagKey, String> tags, MeasureDouble md, Double d) {
    TagContextBuilder contextBuilder = tagger.emptyBuilder();
    tags.forEach((tag, val) -> contextBuilder.put(tag, TagValue.create(val)));
    TagContext tctx = contextBuilder.build();

    try (Scope ss = tagger.withTagContext(tctx)) {
      statsRecorder.newMeasureMap().put(md, d).record(tctx);
      log.info("Recorded metric to stackdriver");
    }
  }

  @Override
  public void initializeMetrics() {
    metricConfigDefinitions.forEach(metricConfigDefinition -> {
      List<String> labels = metricGroupMap.get(metricConfigDefinition.getMetricGroup()).getLabels();
      labels.add("ENV");
      metricConfigDefinition.getMetrics().forEach(metric -> {
        // registerGaugeMetric(metric.getMetricName(), labels);

        // register opencensus also.
        List<TagKey> tagKeys = new ArrayList<>();
        labels.forEach(label -> tagKeys.add(TagKey.create(label)));
        MeasureDouble measure =
            MeasureDouble.create(metric.getMetricName(), metric.getMetricDefinition(), metric.getUnit());
        metric.setMeasure(measure);

        View view = View.create(View.Name.create(metric.getMetricName()), metric.getMetricDefinition(), measure,
            Aggregation.Count.create(), tagKeys);
        ViewManager vmgr = Stats.getViewManager();
        vmgr.registerView(view);
      });
    });
    try {
      StackdriverStatsExporter.createAndRegister();
    } catch (Exception ex) {
      log.error("Exception", ex);
    }
  }

  @Override
  public String getRecordedMetricData() throws IOException {
    final StringWriter writer = new StringWriter();
    //    Set<String> metrics = new HashSet<>();
    //    metricConfigDefinitions.forEach(configuration ->
    //      metrics.addAll(configuration.getMetrics()));
    //    try {
    //      TextFormat.write004(writer, harnessMetricRegistry.getMetric(metrics));
    //      writer.flush();
    //    } finally {
    //      writer.close();
    //    }
    return writer.getBuffer().toString();
  }

  @Override
  public void recordMetric(String metricName, double value, CVNGMetricContext config) {
    try {
      CVNGMetricConfiguration metricConfiguration = null;
      for (CVNGMetricConfiguration configDefinition : metricConfigDefinitions) {
        if (configDefinition.getMetrics()
                .stream()
                .map(CVNGMetric::getMetricName)
                .collect(Collectors.toList())
                .contains(metricName)) {
          metricConfiguration = configDefinition;
          break;
        }
      }
      if (metricConfiguration == null) {
        throw new IllegalStateException("Unknown metric name while trying to record metrics :" + metricName);
      }
      CVNGMetric cvngMetric = metricConfiguration.getMetrics()
                                  .stream()
                                  .filter(metric -> metric.getMetricName().equals(metricName))
                                  .findFirst()
                                  .get();
      CVNGMetricGroup group = metricGroupMap.get(metricConfiguration.getMetricGroup());
      List<String> labelVals = getLabelValues(group.getLabels(), config);
      Map<TagKey, String> tagsMap = new HashMap<>();
      for (int index = 0; index < group.getLabels().size(); index++) {
        tagsMap.put(TagKey.create(group.getLabels().get(index)), labelVals.get(index));
      }
      //      String [] labelArr = new String[labelVals.size()];
      //      labelVals.toArray(labelArr);
      //      harnessMetricRegistry.recordGaugeValue(metricName, labelArr, value);

      recordTaggedStat(tagsMap, cvngMetric.getMeasure(), value);
    } catch (Exception ex) {
      log.error("Exception occurred while registering a metric", ex);
    }
  }

  private List<String> getLabelValues(List<String> labelNames, Object o) {
    String env = System.getenv("ENV");
    if (isEmpty(env)) {
      env = "localhost";
    }

    List<String> labelValues = new ArrayList<>();
    labelNames.forEach(label -> {
      for (Method method : o.getClass().getMethods()) {
        if ((method.getName().startsWith("get")) && (method.getName().length() == (label.length() + 3))) {
          if (method.getName().toLowerCase().endsWith(label.toLowerCase())) {
            try {
              labelValues.add((String) method.invoke(o));
            } catch (IllegalAccessException e) {
              log.error("Could not determine method: " + method.getName());
            } catch (InvocationTargetException e) {
              log.error("Could not determine method: " + method.getName());
            }
          }
        }
      }
    });

    labelValues.add(env);

    if (labelNames.size() != labelValues.size()) {
      throw new IllegalStateException("Some labels were not found from the object while trying to record metric");
    }
    return labelValues;
  }

  private void registerGaugeMetric(String metricName, List<String> labels) {
    String[] labelArr = new String[labels.size()];
    labels.toArray(labelArr);
    harnessMetricRegistry.registerGaugeMetric(metricName, labelArr, ANALYSIS_METRIC_DESCRIPTION);
  }
}
