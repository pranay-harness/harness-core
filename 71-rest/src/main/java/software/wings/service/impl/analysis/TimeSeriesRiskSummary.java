package software.wings.service.impl.analysis;

import static io.harness.data.encoding.EncodingUtils.compressString;
import static io.harness.data.encoding.EncodingUtils.deCompressString;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import io.harness.exception.WingsException;
import lombok.AllArgsConstructor;
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
import software.wings.utils.JsonUtils;

import java.io.IOException;
import java.util.Map;

/**
 * Class representing an entity of cumulative sums and risk for each window of analysis.
 * Created by Praveen.
 */

@Entity(value = "timeSeriesRiskSummary", noClassnameStored = true)
@Indexes(
    @Index(fields = { @Field("cvConfigId")
                      , @Field("analysisMinute") }, options = @IndexOptions(name = "minuteIndex")))
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@Builder
public class TimeSeriesRiskSummary extends Base {
  @NotEmpty @Indexed private String cvConfigId;
  @NotEmpty @Indexed private int analysisMinute;
  @Transient Map<String, Map<String, Integer>> txnMetricRisk;
  @Transient Map<String, Map<String, Integer>> txnMetricLongTermPattern;
  private transient Map<String, Map<String, TimeSeriesRiskData>> txnMetricRiskData;
  @JsonIgnore private byte[] compressedMetricRisk;
  @JsonIgnore private byte[] compressedLongTermPattern;
  @JsonIgnore private byte[] compressedRiskData;

  public void compressMaps() {
    if (isEmpty(txnMetricRisk) && isEmpty(txnMetricLongTermPattern) && isEmpty(txnMetricRiskData)) {
      return;
    }
    try {
      if (isNotEmpty(txnMetricRisk)) {
        setCompressedMetricRisk(compressString(JsonUtils.asJson(txnMetricRisk)));
      }
      setTxnMetricRisk(null);

      if (isNotEmpty(txnMetricLongTermPattern)) {
        setCompressedLongTermPattern(compressString(JsonUtils.asJson(txnMetricLongTermPattern)));
      }
      setTxnMetricLongTermPattern(null);

      if (isNotEmpty(txnMetricRiskData)) {
        setCompressedRiskData(compressString(JsonUtils.asJson(txnMetricRiskData)));
      }
      setTxnMetricRiskData(null);
    } catch (IOException e) {
      throw new WingsException(e);
    }
  }

  public void decompressMaps() {
    if (isEmpty(compressedMetricRisk) && isEmpty(compressedLongTermPattern) && isEmpty(compressedRiskData)) {
      return;
    }
    try {
      if (isNotEmpty(compressedMetricRisk)) {
        String decompressedMetricRisk = deCompressString(compressedMetricRisk);
        setTxnMetricRisk(
            JsonUtils.asObject(decompressedMetricRisk, new TypeReference<Map<String, Map<String, Integer>>>() {}));
        setCompressedMetricRisk(null);
      }

      if (isNotEmpty(compressedLongTermPattern)) {
        String decompressedLongTermPattern = deCompressString(compressedLongTermPattern);
        setTxnMetricLongTermPattern(
            JsonUtils.asObject(decompressedLongTermPattern, new TypeReference<Map<String, Map<String, Integer>>>() {}));
        setCompressedLongTermPattern(null);
      }

      if (isNotEmpty(compressedRiskData)) {
        String decompressedRiskData = deCompressString(compressedRiskData);
        setTxnMetricRiskData(JsonUtils.asObject(
            decompressedRiskData, new TypeReference<Map<String, Map<String, TimeSeriesRiskData>>>() {}));
        setCompressedRiskData(null);
      }
    } catch (IOException e) {
      throw new WingsException(e);
    }
  }
}
