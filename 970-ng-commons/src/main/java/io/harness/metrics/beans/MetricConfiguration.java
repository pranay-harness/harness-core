package io.harness.metrics.beans;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.opencensus.stats.Aggregation;
import io.opencensus.stats.Measure;
import io.opencensus.stats.Measure.MeasureDouble;
import io.opencensus.stats.Measure.MeasureLong;
import io.opencensus.stats.View;
import io.opencensus.tags.TagKey;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
// TODO: Give a better name
public class MetricConfiguration {
  String name;
  String identifier;
  String metricGroup;
  List<Metric> metrics;

  @Data
  @Builder
  public static class Metric {
    String metricName;
    String metricDefinition;
    String type;
    String unit;
    Measure measure;
    View view;

    public View getView(List<TagKey> tagKeys) {
      Measure measure = MeasureDouble.create(this.getMetricName(), this.getMetricDefinition(), this.getUnit());
      this.setMeasure(measure);
      if (isEmpty(type)) {
        return View.create(View.Name.create(this.getMetricName()), this.getMetricDefinition(), measure,
            Aggregation.LastValue.create(), tagKeys);
      }

      switch (type) {
        case "LastValue":
          return View.create(View.Name.create(this.getMetricName()), this.getMetricDefinition(), measure,
              Aggregation.LastValue.create(), tagKeys);
        case "Count":
          measure = MeasureLong.create(this.getMetricName(), this.getMetricDefinition(), this.getUnit());
          this.setMeasure(measure);
          return View.create(View.Name.create(this.getMetricName()), this.getMetricDefinition(), measure,
              Aggregation.Count.create(), tagKeys);
        case "Mean":
          return View.create(View.Name.create(this.getMetricName()), this.getMetricDefinition(), measure,
              Aggregation.Sum.create(), tagKeys);

        default:
          throw new IllegalStateException("Unsupported metric type");
      }
    }
  }
}