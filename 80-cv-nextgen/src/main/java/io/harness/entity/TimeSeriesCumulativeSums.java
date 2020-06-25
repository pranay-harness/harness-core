package io.harness.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.annotation.HarnessEntity;
import io.harness.mongo.index.FdIndex;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldNameConstants(innerTypeName = "TimeSeriesCumulativeSumsKeys")
@EqualsAndHashCode(callSuper = false, exclude = {"transactionMetricSums", "compressedMetricSums"})
@JsonIgnoreProperties(ignoreUnknown = true)
@Entity(value = "serviceGuardTimeseriesCumulativeSums", noClassnameStored = true)
@HarnessEntity(exportable = false)
public class TimeSeriesCumulativeSums {
  @NotEmpty @FdIndex private String cvConfigId;
  @NotEmpty @FdIndex private int analysisMinute;

  private Map<String, Map<String, TransactionMetricSums>> transactionMetricSums;
}
