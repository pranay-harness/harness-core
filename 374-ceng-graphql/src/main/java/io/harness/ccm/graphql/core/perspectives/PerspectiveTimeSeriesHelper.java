package io.harness.ccm.graphql.core.perspectives;

import io.harness.ccm.graphql.dto.common.DataPoint;
import io.harness.ccm.graphql.dto.common.DataPoint.DataPointBuilder;
import io.harness.ccm.graphql.dto.common.Reference;
import io.harness.ccm.graphql.dto.common.TimeSeriesDataPoints;
import io.harness.ccm.graphql.dto.perspectives.PerspectiveTimeSeriesData;

import com.google.cloud.Timestamp;
import com.google.cloud.bigquery.Field;
import com.google.cloud.bigquery.FieldList;
import com.google.cloud.bigquery.FieldValue;
import com.google.cloud.bigquery.FieldValueList;
import com.google.cloud.bigquery.Schema;
import com.google.cloud.bigquery.TableResult;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PerspectiveTimeSeriesHelper {
  public static final String nullStringValueConstant = "Others";
  public static final String OTHERS = "Others";

  public PerspectiveTimeSeriesData fetch(TableResult result) {
    Schema schema = result.getSchema();
    FieldList fields = schema.getFields();

    Map<Timestamp, List<DataPoint>> timeSeriesDataPointsMap = new LinkedHashMap();
    for (FieldValueList row : result.iterateAll()) {
      DataPointBuilder dataPointBuilder = DataPoint.builder();
      Timestamp startTimeTruncatedTimestamp = null;
      Double value = Double.valueOf(0);
      for (Field field : fields) {
        switch (field.getType().getStandardType()) {
          case TIMESTAMP:
            startTimeTruncatedTimestamp = Timestamp.ofTimeMicroseconds(row.get(field.getName()).getTimestampValue());
            break;
          case STRING:
            String stringValue = fetchStringValue(row, field);
            dataPointBuilder.key(Reference.builder().id(stringValue).name(stringValue).type(field.getName()).build());
            break;
          case FLOAT64:
            value += getNumericValue(row, field);
            break;
          default:
            break;
        }
      }

      dataPointBuilder.value(getRoundedDoubleValue(value));
      List<DataPoint> dataPoints = new ArrayList<>();
      if (timeSeriesDataPointsMap.containsKey(startTimeTruncatedTimestamp)) {
        dataPoints = timeSeriesDataPointsMap.get(startTimeTruncatedTimestamp);
      }
      dataPoints.add(dataPointBuilder.build());
      timeSeriesDataPointsMap.put(startTimeTruncatedTimestamp, dataPoints);
    }

    return PerspectiveTimeSeriesData.builder().stats(convertTimeSeriesPointsMapToList(timeSeriesDataPointsMap)).build();
  }

  private List<TimeSeriesDataPoints> convertTimeSeriesPointsMapToList(
      Map<Timestamp, List<DataPoint>> timeSeriesDataPointsMap) {
    return timeSeriesDataPointsMap.entrySet()
        .stream()
        .map(e
            -> TimeSeriesDataPoints.builder().time(e.getKey().toSqlTimestamp().getTime()).values(e.getValue()).build())
        .collect(Collectors.toList());
  }

  private static String fetchStringValue(FieldValueList row, Field field) {
    Object value = row.get(field.getName()).getValue();
    if (value != null) {
      return value.toString();
    }
    return nullStringValueConstant;
  }

  private double getNumericValue(FieldValueList row, Field field) {
    FieldValue value = row.get(field.getName());
    if (!value.isNull()) {
      return Math.round(value.getNumericValue().doubleValue() * 100D) / 100D;
    }
    return 0;
  }

  private double getRoundedDoubleValue(double value) {
    return Math.round(value * 100D) / 100D;
  }

  public PerspectiveTimeSeriesData postFetch(PerspectiveTimeSeriesData data, Integer limit, boolean includeOthers) {
    Map<String, Double> aggregatedDataPerUniqueId = new HashMap<>();
    data.getStats().forEach(dataPoint -> {
      for (DataPoint entry : dataPoint.getValues()) {
        Reference qlReference = entry.getKey();
        if (qlReference != null && qlReference.getId() != null) {
          String key = qlReference.getId();
          if (aggregatedDataPerUniqueId.containsKey(key)) {
            aggregatedDataPerUniqueId.put(key, entry.getValue().doubleValue() + aggregatedDataPerUniqueId.get(key));
          } else {
            aggregatedDataPerUniqueId.put(key, entry.getValue().doubleValue());
          }
        }
      }
    });
    if (aggregatedDataPerUniqueId.isEmpty()) {
      return data;
    }
    List<String> selectedIdsAfterLimit = getElementIdsAfterLimit(aggregatedDataPerUniqueId, limit);

    return PerspectiveTimeSeriesData.builder()
        .stats(getDataAfterLimit(data, selectedIdsAfterLimit, includeOthers))
        .build();
  }

  private List<TimeSeriesDataPoints> getDataAfterLimit(
      PerspectiveTimeSeriesData data, List<String> selectedIdsAfterLimit, boolean includeOthers) {
    List<TimeSeriesDataPoints> limitProcessedData = new ArrayList<>();
    data.getStats().forEach(dataPoint -> {
      List<DataPoint> limitProcessedValues = new ArrayList<>();
      DataPoint others = DataPoint.builder().key(Reference.builder().id(OTHERS).name(OTHERS).build()).value(0).build();
      for (DataPoint entry : dataPoint.getValues()) {
        String key = entry.getKey().getId();
        if (selectedIdsAfterLimit.contains(key)) {
          limitProcessedValues.add(entry);
        } else {
          others.setValue(others.getValue().doubleValue() + entry.getValue().doubleValue());
        }
      }

      if (others.getValue().doubleValue() > 0 && includeOthers) {
        others.setValue(getRoundedDoubleValue(others.getValue().doubleValue()));
        limitProcessedValues.add(others);
      }

      limitProcessedData.add(
          TimeSeriesDataPoints.builder().time(dataPoint.getTime()).values(limitProcessedValues).build());
    });
    return limitProcessedData;
  }

  private List<String> getElementIdsAfterLimit(Map<String, Double> aggregatedData, Integer limit) {
    List<Map.Entry<String, Double>> list = new ArrayList<>(aggregatedData.entrySet());
    list.sort(Map.Entry.comparingByValue(Comparator.reverseOrder()));
    list = list.stream().limit(limit).collect(Collectors.toList());
    List<String> topNElementIds = new ArrayList<>();
    list.forEach(entry -> topNElementIds.add(entry.getKey()));
    return topNElementIds;
  }
}
