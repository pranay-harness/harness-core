package software.wings.service.impl.analysis;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.annotation.HarnessEntity;
import io.harness.annotation.IgnoreUnusedIndex;
import io.harness.mongo.index.Field;
import io.harness.mongo.index.Index;
import io.harness.mongo.index.IndexOptions;
import io.harness.mongo.index.IndexType;
import io.harness.mongo.index.Indexed;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldNameConstants;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;

/**
 * ExperimentalMetricAnalysisRecord is the payload send after ML Analysis of Experimental Task.
 *
 * Created by Pranjal on 08/14/2018
 */

@Index(name = "MetricAnalysisUniqueIdx",
    fields =
    { @Field("workflowExecutionId")
      , @Field("stateExecutionId"), @Field("analysisMinute"), @Field("groupName") },
    options = @IndexOptions(unique = true))
@Index(name = "ExperimentalMetricListIdx",
    fields = { @Field("analysisMinute")
               , @Field("mismatched"), @Field(value = "createdAt", type = IndexType.DESC) })
@Index(name = "analysisMinStateExecutionIdIndex", fields = { @Field("analysisMinute")
                                                             , @Field("stateExecutionId") })
@Data
@Builder
@EqualsAndHashCode(callSuper = false)
@JsonIgnoreProperties(ignoreUnknown = true)
@FieldNameConstants(innerTypeName = "ExperimentalMetricAnalysisRecordKeys")
@IgnoreUnusedIndex
@Entity(value = "experimentalTimeSeriesAnalysisRecords", noClassnameStored = true)
@HarnessEntity(exportable = false)
public class ExperimentalMetricAnalysisRecord extends MetricAnalysisRecord {
  private String envId;
  @Builder.Default @Indexed private boolean mismatched = true;
  @Builder.Default private ExperimentStatus experimentStatus = ExperimentStatus.UNDETERMINED;
  @NotEmpty private String experimentName;
}
