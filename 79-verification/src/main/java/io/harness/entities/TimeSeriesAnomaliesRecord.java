package io.harness.entities;

import static io.harness.data.encoding.EncodingUtils.compressString;
import static io.harness.data.encoding.EncodingUtils.deCompressString;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import io.harness.annotation.HarnessEntity;
import io.harness.exception.EncryptDecryptException;
import io.harness.persistence.AccountAccess;
import io.harness.serializer.JsonUtils;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Indexed;
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
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@FieldNameConstants(innerTypeName = "TimeSeriesAnomaliesRecordKeys")
@Entity(value = "timeSeriesAnomaliesRecords", noClassnameStored = true)
@HarnessEntity(exportable = false)
public class TimeSeriesAnomaliesRecord extends Base implements AccountAccess {
  @NotEmpty @Indexed private String cvConfigId;
  @Transient private Map<String, Map<String, List<TimeSeriesMLHostSummary>>> anomalies;
  @JsonIgnore private byte[] compressedAnomalies;
  @Indexed private String accountId;
  private String tag;

  public void compressAnomalies() {
    if (isEmpty(anomalies)) {
      return;
    }
    try {
      setCompressedAnomalies(compressString(JsonUtils.asJson(anomalies)));
      setAnomalies(null);
    } catch (IOException e) {
      throw new EncryptDecryptException(e.getMessage());
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
      throw new EncryptDecryptException(e.getMessage());
    }
  }
}
