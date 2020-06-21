package io.harness.entities;

import static io.harness.data.encoding.EncodingUtils.compressString;
import static io.harness.data.encoding.EncodingUtils.deCompressString;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import io.harness.annotation.HarnessEntity;
import io.harness.exception.WingsException;
import io.harness.mongo.index.Field;
import io.harness.mongo.index.Index;
import io.harness.mongo.index.IndexOptions;
import io.harness.mongo.index.IndexType;
import io.harness.mongo.index.Indexed;
import io.harness.mongo.index.Indexes;
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
import org.mongodb.morphia.annotations.Transient;
import software.wings.beans.Base;

import java.io.IOException;
import java.util.Map;

/**
 * Class representing an entity of cumulative sums and risk for each window of analysis.
 * Created by Praveen.
 */

@Indexes({
  @Index(name = "uniqueIdx",
      fields = { @Field("appId")
                 , @Field("cvConfigId"), @Field("analysisMinute"), @Field("tag") },
      options = @IndexOptions(unique = true))
  ,

      @Index(fields = {
        @Field("cvConfigId"), @Field(value = "analysisMinute", type = IndexType.DESC), @Field("tag")
      }, name = "service_gd_idx")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldNameConstants(innerTypeName = "TimeSeriesCumulativeSumsKeys")
@EqualsAndHashCode(callSuper = false, exclude = {"transactionMetricSums", "compressedMetricSums"})
@JsonIgnoreProperties(ignoreUnknown = true)
@Entity(value = "timeSeriesCumulativeSums", noClassnameStored = true)
@HarnessEntity(exportable = false)
public class TimeSeriesCumulativeSums extends Base implements AccountAccess {
  @NotEmpty @Indexed private String cvConfigId;
  @NotEmpty @Indexed private int analysisMinute;
  @Transient private Map<String, Map<String, Map<String, Double>>> transactionMetricSums;
  @JsonIgnore private byte[] compressedMetricSums;
  @Indexed private String accountId;

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
