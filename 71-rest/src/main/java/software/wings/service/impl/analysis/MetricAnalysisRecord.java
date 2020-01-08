package software.wings.service.impl.analysis;

import static io.harness.data.encoding.EncodingUtils.compressString;
import static io.harness.data.encoding.EncodingUtils.deCompressString;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static software.wings.common.VerificationConstants.ML_RECORDS_TTL_MONTHS;
import static software.wings.service.impl.newrelic.NewRelicMetricDataRecord.DEFAULT_GROUP_NAME;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.type.TypeReference;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.exception.WingsException;
import io.harness.serializer.JsonUtils;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;
import org.hibernate.validator.constraints.NotEmpty;
import org.jetbrains.annotations.NotNull;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Transient;
import software.wings.beans.Base;
import software.wings.sm.StateType;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Common Class extended by TimeSeries and Experimental Analysis Record.
 * Created by Pranjal on 08/16/2018
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = false,
    of = {"stateType", "workflowExecutionId", "stateExecutionId", "analysisMinute", "groupName", "baseLineExecutionId",
        "cvConfigId", "tag"})
@FieldNameConstants(innerTypeName = "MetricAnalysisRecordKeys")
public class MetricAnalysisRecord extends Base implements Comparable<MetricAnalysisRecord> {
  @NotEmpty private StateType stateType;

  @NotEmpty private String workflowExecutionId;

  @NotEmpty private String stateExecutionId;

  // no. of minutes of analysis
  @NotEmpty private int analysisMinute;

  private String groupName = DEFAULT_GROUP_NAME;

  private String baseLineExecutionId;

  private Map<String, TimeSeriesMLTxnSummary> transactions;

  private Map<String, Double> overallMetricScores;

  private Map<String, Map<String, Double>> keyTransactionMetricScores;

  private byte[] transactionsCompressedJson;

  @Transient private Map<String, Map<String, List<TimeSeriesMLHostSummary>>> anomalies;

  @Transient private Map<String, Map<String, Map<String, Double>>> transactionMetricSums;

  private String message;

  private String cvConfigId;

  private int aggregatedRisk = -1;

  private String tag;

  private boolean shouldFailFast;

  private String failFastErrorMsg;

  @JsonIgnore
  @SchemaIgnore
  @Indexed(options = @IndexOptions(expireAfterSeconds = 0))
  private Date validUntil = Date.from(OffsetDateTime.now().plusMonths(ML_RECORDS_TTL_MONTHS).toInstant());

  public void compressTransactions() {
    if (isEmpty(transactions)) {
      return;
    }
    try {
      setTransactionsCompressedJson(compressString(JsonUtils.asJson(transactions)));
      setTransactions(null);
    } catch (IOException e) {
      throw new WingsException(e);
    }
  }

  public void decompressTransactions() {
    if (isEmpty(transactionsCompressedJson)) {
      return;
    }
    try {
      String decompressedTransactionsJson = deCompressString(getTransactionsCompressedJson());
      setTransactions(JsonUtils.asObject(
          decompressedTransactionsJson, new TypeReference<Map<String, TimeSeriesMLTxnSummary>>() {}));
      setTransactionsCompressedJson(null);
    } catch (IOException e) {
      throw new WingsException(e);
    }
  }

  @Override
  public int compareTo(@NotNull MetricAnalysisRecord o) {
    return this.analysisMinute - o.analysisMinute;
  }
}
