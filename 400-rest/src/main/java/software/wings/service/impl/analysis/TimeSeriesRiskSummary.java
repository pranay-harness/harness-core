package software.wings.service.impl.analysis;

import static io.harness.data.encoding.EncodingUtils.compressString;
import static io.harness.data.encoding.EncodingUtils.deCompressString;
import static io.harness.data.structure.HasPredicate.hasNone;
import static io.harness.data.structure.HasPredicate.hasSome;

import io.harness.annotation.HarnessEntity;
import io.harness.exception.WingsException;
import io.harness.mongo.index.CdIndex;
import io.harness.mongo.index.FdIndex;
import io.harness.mongo.index.Field;
import io.harness.persistence.AccountAccess;
import io.harness.serializer.JsonUtils;

import software.wings.beans.Base;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import java.io.IOException;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Transient;

/**
 * Class representing an entity of cumulative sums and risk for each window of analysis.
 * Created by Praveen.
 */
@CdIndex(name = "minute_idx", fields = { @Field("cvConfigId")
                                         , @Field("analysisMinute"), @Field("tag") })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@FieldNameConstants(innerTypeName = "TimeSeriesRiskSummaryKeys")
@Entity(value = "timeSeriesRiskSummary", noClassnameStored = true)
@HarnessEntity(exportable = false)
public class TimeSeriesRiskSummary extends Base implements AccountAccess {
  @NotEmpty private String cvConfigId;
  @NotEmpty private int analysisMinute;
  @Transient Map<String, Map<String, Integer>> txnMetricRisk;
  @Transient Map<String, Map<String, Integer>> txnMetricLongTermPattern;
  private transient Map<String, Map<String, TimeSeriesRiskData>> txnMetricRiskData;
  @JsonIgnore private byte[] compressedMetricRisk;
  @JsonIgnore private byte[] compressedLongTermPattern;
  @JsonIgnore private byte[] compressedRiskData;
  @FdIndex private String accountId;

  private String tag;

  public void compressMaps() {
    if (hasNone(txnMetricRisk) && hasNone(txnMetricLongTermPattern) && hasNone(txnMetricRiskData)) {
      return;
    }
    try {
      if (hasSome(txnMetricRisk)) {
        setCompressedMetricRisk(compressString(JsonUtils.asJson(txnMetricRisk)));
      }
      setTxnMetricRisk(null);

      if (hasSome(txnMetricLongTermPattern)) {
        setCompressedLongTermPattern(compressString(JsonUtils.asJson(txnMetricLongTermPattern)));
      }
      setTxnMetricLongTermPattern(null);

      if (hasSome(txnMetricRiskData)) {
        setCompressedRiskData(compressString(JsonUtils.asJson(txnMetricRiskData)));
      }
      setTxnMetricRiskData(null);
    } catch (IOException e) {
      throw new WingsException(e);
    }
  }

  public void decompressMaps() {
    if (hasNone(compressedMetricRisk) && hasNone(compressedLongTermPattern) && hasNone(compressedRiskData)) {
      return;
    }
    try {
      if (hasSome(compressedMetricRisk)) {
        String decompressedMetricRisk = deCompressString(compressedMetricRisk);
        setTxnMetricRisk(
            JsonUtils.asObject(decompressedMetricRisk, new TypeReference<Map<String, Map<String, Integer>>>() {}));
        setCompressedMetricRisk(null);
      }

      if (hasSome(compressedLongTermPattern)) {
        String decompressedLongTermPattern = deCompressString(compressedLongTermPattern);
        setTxnMetricLongTermPattern(
            JsonUtils.asObject(decompressedLongTermPattern, new TypeReference<Map<String, Map<String, Integer>>>() {}));
        setCompressedLongTermPattern(null);
      }

      if (hasSome(compressedRiskData)) {
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
