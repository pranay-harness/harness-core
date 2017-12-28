package software.wings.api;

import static software.wings.api.ExecutionDataValue.Builder.anExecutionDataValue;

import com.google.common.base.Functions;
import com.google.common.collect.Lists;

import com.amazonaws.services.cloudwatch.model.Datapoint;
import com.amazonaws.services.cloudwatch.model.Dimension;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import software.wings.sm.StateExecutionData;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by anubhaw on 12/9/16.
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CloudWatchExecutionData extends StateExecutionData {
  private String region;
  private String namespace;
  private String metricName;
  private String percentile;
  private List<Dimension> dimensions = new ArrayList<>();
  private String timeDuration;
  private Datapoint datapoint;
  private String assertionStatement;
  private String assertionStatus;

  @Override
  public Map<String, ExecutionDataValue> getExecutionSummary() {
    Map<String, ExecutionDataValue> executionDetails = super.getExecutionSummary();
    putNotNull(executionDetails, "statistics",
        anExecutionDataValue()
            .withValue(datapoint == null ? null : datapoint.toString())
            .withDisplayName("statistics")
            .build());
    putNotNull(executionDetails, "assertionStatement",
        anExecutionDataValue().withValue(assertionStatement).withDisplayName("Assertion").build());
    putNotNull(executionDetails, "assertionStatus",
        anExecutionDataValue().withValue(assertionStatus).withDisplayName("Assertion Result").build());
    return executionDetails;
  }

  @Override
  public Map<String, ExecutionDataValue> getExecutionDetails() {
    Map<String, ExecutionDataValue> executionDetails = super.getExecutionDetails();
    putNotNull(executionDetails, "region", anExecutionDataValue().withValue(region).withDisplayName("Region").build());
    putNotNull(executionDetails, "namespace",
        anExecutionDataValue().withValue(namespace).withDisplayName("Namespace").build());
    putNotNull(executionDetails, "metricName",
        anExecutionDataValue().withValue(metricName).withDisplayName("metricName").build());
    putNotNull(executionDetails, "percentile",
        anExecutionDataValue().withValue(percentile).withDisplayName("percentile").build());
    putNotNull(executionDetails, "dimensions",
        anExecutionDataValue()
            .withValue(dimensions == null ? null : Lists.transform(dimensions, Functions.toStringFunction()))
            .withDisplayName("dimensions")
            .build());
    putNotNull(executionDetails, "statistics",
        anExecutionDataValue()
            .withValue(datapoint == null ? null : datapoint.toString())
            .withDisplayName("statistics")
            .build());
    putNotNull(executionDetails, "assertion",
        anExecutionDataValue().withValue(assertionStatement).withDisplayName("Assertion").build());
    putNotNull(executionDetails, "assertionStatus",
        anExecutionDataValue().withValue(assertionStatus).withDisplayName("Assertion Result").build());
    return executionDetails;
  }
}
