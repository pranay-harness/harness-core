package software.wings.service.impl.analysis;

import static io.harness.data.encoding.EncodingUtils.compressString;
import static io.harness.data.encoding.EncodingUtils.deCompressString;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static software.wings.common.Constants.ML_RECORDS_TTL_MONTHS;
import static software.wings.service.impl.newrelic.NewRelicMetricDataRecord.DEFAULT_GROUP_NAME;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.type.TypeReference;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.exception.WingsException;
import lombok.Builder.Default;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Transient;
import software.wings.beans.Base;
import software.wings.sm.StateType;
import software.wings.utils.JsonUtils;

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
@EqualsAndHashCode(callSuper = false, exclude = {"validUntil"})
public class MetricAnalysisRecord extends Base {
  // Represents type of State
  @NotEmpty @Indexed private StateType stateType;

  // Work flow exec id
  @NotEmpty @Indexed private String workflowExecutionId;

  // State exec id
  @NotEmpty @Indexed private String stateExecutionId;

  // no. of minutes of analysis
  @NotEmpty @Indexed private int analysisMinute;

  @Indexed private String groupName = DEFAULT_GROUP_NAME;

  private String baseLineExecutionId;

  private Map<String, TimeSeriesMLTxnSummary> transactions;

  private byte[] transactionsCompressedJson;

  @Transient private Map<String, Map<String, List<TimeSeriesMLHostSummary>>> anomalies;

  private String message;

  private String cvConfigId;

  @Default private int aggregatedRisk = -1;

  @SchemaIgnore
  @JsonIgnore
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
}
