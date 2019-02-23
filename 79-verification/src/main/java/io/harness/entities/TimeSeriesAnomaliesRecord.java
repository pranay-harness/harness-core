package io.harness.entities;

import static io.harness.data.encoding.EncodingUtils.compressString;
import static io.harness.data.encoding.EncodingUtils.deCompressString;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import io.harness.exception.WingsException;
import io.harness.serializer.JsonUtils;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Indexes;
import org.mongodb.morphia.annotations.Transient;
import software.wings.beans.Base;
import software.wings.service.impl.analysis.TimeSeriesMLHostSummary;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Class representing entity for TimeSeries Analysis Record.
 * Created by sriram_parthasarathy on 9/22/17.
 */
@Entity(value = "timeSeriesAnomaliesRecords", noClassnameStored = true)
@Indexes(@Index(
    fields = { @Field("appId")
               , @Field("cvConfigId") }, options = @IndexOptions(unique = true, name = "uniqueIdx")))
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class TimeSeriesAnomaliesRecord extends Base {
  @NotEmpty @Indexed private String cvConfigId;
  @Transient private Map<String, Map<String, List<TimeSeriesMLHostSummary>>> anomalies;
  @JsonIgnore private byte[] compressedAnomalies;

  @Builder
  public TimeSeriesAnomaliesRecord(
      String cvConfigId, Map<String, Map<String, List<TimeSeriesMLHostSummary>>> anomalies) {
    this.cvConfigId = cvConfigId;
    this.anomalies = anomalies;
  }

  public void compressAnomalies() {
    if (isEmpty(anomalies)) {
      return;
    }
    try {
      setCompressedAnomalies(compressString(JsonUtils.asJson(anomalies)));
      setAnomalies(null);
    } catch (IOException e) {
      throw new WingsException(e);
    }
  }

  public void decompressAnomalies() {
    if (isEmpty(compressedAnomalies)) {
      return;
    }
    try {
      String decompressedTransactionsJson = deCompressString(compressedAnomalies);
      setAnomalies(JsonUtils.asObject(decompressedTransactionsJson,
          new TypeReference<Map<String, Map<String, List<TimeSeriesMLHostSummary>>>>() {}));
      setCompressedAnomalies(null);
    } catch (IOException e) {
      throw new WingsException(e);
    }
  }
}
