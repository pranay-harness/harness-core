package io.harness.entities;

import static io.harness.data.encoding.EncodingUtils.compressString;
import static io.harness.data.encoding.EncodingUtils.deCompressString;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import io.harness.exception.WingsException;
import io.harness.serializer.JsonUtils;
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

import java.io.IOException;
import java.util.Map;

/**
 * Class representing an entity of cumulative sums and risk for each window of analysis.
 * Created by Praveen.
 */

@Entity(value = "timeSeriesCumulativeSums", noClassnameStored = true)
@Indexes(@Index(fields = { @Field("appId")
                           , @Field("cvConfigId"), @Field("analysisMinute"), @Field("tag") },
    options = @IndexOptions(unique = true, name = "uniqueIdx")))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class TimeSeriesCumulativeSums extends Base {
  @NotEmpty @Indexed private String cvConfigId;
  @NotEmpty @Indexed private int analysisMinute;
  @Transient private Map<String, Map<String, Map<String, Double>>> transactionMetricSums;
  @JsonIgnore private byte[] compressedMetricSums;

  private String tag;

  public void compressMetricSums() {
    if (isEmpty(transactionMetricSums)) {
      return;
    }
    try {
      setCompressedMetricSums(compressString(JsonUtils.asJson(transactionMetricSums)));
      setTransactionMetricSums(null);
    } catch (IOException e) {
      throw new WingsException(e);
    }
  }

  public void decompressMetricSums() {
    if (isEmpty(compressedMetricSums)) {
      return;
    }
    try {
      String decompressedTransactionsJson = deCompressString(compressedMetricSums);
      setTransactionMetricSums(JsonUtils.asObject(
          decompressedTransactionsJson, new TypeReference<Map<String, Map<String, Map<String, Double>>>>() {}));
      setCompressedMetricSums(null);
    } catch (IOException e) {
      throw new WingsException(e);
    }
  }
}
