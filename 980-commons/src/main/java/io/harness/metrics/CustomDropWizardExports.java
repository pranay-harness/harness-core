/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.metrics;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Snapshot;
import com.codahale.metrics.Timer;
import io.prometheus.client.Collector;
import io.prometheus.client.Collector.Describable;
import io.prometheus.client.Collector.MetricFamilySamples.Sample;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Created by Pranjal on 11/21/2018
 */

public class CustomDropWizardExports extends Collector implements Describable {
  private MetricRegistry registry;
  private static final Logger LOGGER =
      Logger.getLogger(io.prometheus.client.dropwizard.DropwizardExports.class.getName());
  private static final Pattern METRIC_NAME_RE = Pattern.compile("[^a-zA-Z0-9:_]");

  public CustomDropWizardExports(MetricRegistry registry) {
    this.registry = registry;
  }

  List<MetricFamilySamples> fromCounter(String dropwizardName, Counter counter) {
    String name = sanitizeMetricName(dropwizardName);
    Sample sample =
        new Sample(name, new ArrayList(), new ArrayList(), (Long.valueOf(counter.getCount())).doubleValue());
    return Arrays.asList(
        new MetricFamilySamples(name, Type.GAUGE, getHelpMessage(dropwizardName, counter), Arrays.asList(sample)));
  }

  private static String getHelpMessage(String metricName, Metric metric) {
    return String.format(
        "Generated from Dropwizard metric import (metric=%s, type=%s)", metricName, metric.getClass().getName());
  }

  List<MetricFamilySamples> fromGauge(String dropwizardName, Gauge gauge) {
    String name = sanitizeMetricName(dropwizardName);
    Object obj = gauge.getValue();
    double value;
    if (obj instanceof Number) {
      value = ((Number) obj).doubleValue();
    } else {
      if (!(obj instanceof Boolean)) {
        LOGGER.log(Level.FINE,
            String.format("Invalid type for Gauge %s: %s", name, obj == null ? "null" : obj.getClass().getName()));
        return new ArrayList();
      }

      value = (Boolean) obj ? 1.0D : 0.0D;
    }

    Sample sample = new Sample(name, new ArrayList(), new ArrayList(), value);
    return Arrays.asList(
        new MetricFamilySamples(name, Type.GAUGE, getHelpMessage(dropwizardName, gauge), Arrays.asList(sample)));
  }

  List<MetricFamilySamples> fromSnapshotAndCount(
      String dropwizardName, Snapshot snapshot, long count, double factor, String helpMessage) {
    String name = sanitizeMetricName(dropwizardName);
    List<Sample> samples =
        Arrays.asList(new Sample(name, Arrays.asList("quantile"), Arrays.asList("0.5"), snapshot.getMedian() * factor),
            new Sample(name, Arrays.asList("quantile"), Arrays.asList("0.75"), snapshot.get75thPercentile() * factor),
            new Sample(name, Arrays.asList("quantile"), Arrays.asList("0.95"), snapshot.get95thPercentile() * factor),
            new Sample(name, Arrays.asList("quantile"), Arrays.asList("0.98"), snapshot.get98thPercentile() * factor),
            new Sample(name, Arrays.asList("quantile"), Arrays.asList("0.99"), snapshot.get99thPercentile() * factor),
            new Sample(name, Arrays.asList("quantile"), Arrays.asList("0.999"), snapshot.get999thPercentile() * factor),
            new Sample(name + "_count", new ArrayList(), new ArrayList(), (double) count));
    return Arrays.asList(new MetricFamilySamples(name, Type.SUMMARY, helpMessage, samples));
  }

  List<MetricFamilySamples> fromHistogram(String dropwizardName, Histogram histogram) {
    return this.fromSnapshotAndCount(
        dropwizardName, histogram.getSnapshot(), histogram.getCount(), 1.0D, getHelpMessage(dropwizardName, histogram));
  }

  List<MetricFamilySamples> fromTimer(String dropwizardName, Timer timer) {
    return this.fromSnapshotAndCount(dropwizardName, timer.getSnapshot(), timer.getCount(),
        1.0D / (double) TimeUnit.SECONDS.toNanos(1L), getHelpMessage(dropwizardName, timer));
  }

  List<MetricFamilySamples> fromMeter(String dropwizardName, Meter meter) {
    String name = sanitizeMetricName(dropwizardName);

    List<Sample> samples =
        Arrays.asList(new Sample(name, Arrays.asList("rate"), Arrays.asList("m1Rate"), meter.getOneMinuteRate()),
            new Sample(name, Arrays.asList("rate"), Arrays.asList("m5Rate"), meter.getFiveMinuteRate()),
            new Sample(name, Arrays.asList("rate"), Arrays.asList("m15Rate"), meter.getFifteenMinuteRate()),
            new Sample(name, Arrays.asList("rate"), Arrays.asList("mean"), meter.getMeanRate()));

    return Arrays.asList(new MetricFamilySamples(name, Type.SUMMARY, getHelpMessage(dropwizardName, meter), samples));
  }

  public static String sanitizeMetricName(String dropwizardName) {
    String name = METRIC_NAME_RE.matcher(dropwizardName).replaceAll("_");
    if (!name.isEmpty() && Character.isDigit(name.charAt(0))) {
      name = "_" + name;
    }

    return name;
  }

  @Override
  public List<MetricFamilySamples> collect() {
    ArrayList<MetricFamilySamples> mfSamples = new ArrayList();
    Iterator var2 = this.registry.getGauges().entrySet().iterator();

    Entry entry;
    while (var2.hasNext()) {
      entry = (Entry) var2.next();
      mfSamples.addAll(this.fromGauge((String) entry.getKey(), (Gauge) entry.getValue()));
    }

    var2 = this.registry.getCounters().entrySet().iterator();

    while (var2.hasNext()) {
      entry = (Entry) var2.next();
      mfSamples.addAll(this.fromCounter((String) entry.getKey(), (Counter) entry.getValue()));
    }

    var2 = this.registry.getHistograms().entrySet().iterator();

    while (var2.hasNext()) {
      entry = (Entry) var2.next();
      mfSamples.addAll(this.fromHistogram((String) entry.getKey(), (Histogram) entry.getValue()));
    }

    var2 = this.registry.getTimers().entrySet().iterator();

    while (var2.hasNext()) {
      entry = (Entry) var2.next();
      mfSamples.addAll(this.fromTimer((String) entry.getKey(), (Timer) entry.getValue()));
    }

    var2 = this.registry.getMeters().entrySet().iterator();

    while (var2.hasNext()) {
      entry = (Entry) var2.next();
      mfSamples.addAll(this.fromMeter((String) entry.getKey(), (Meter) entry.getValue()));
    }

    return mfSamples;
  }

  @Override
  public List<MetricFamilySamples> describe() {
    return new ArrayList();
  }
}
