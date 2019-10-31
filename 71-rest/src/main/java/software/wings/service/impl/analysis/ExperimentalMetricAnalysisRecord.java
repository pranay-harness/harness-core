package software.wings.service.impl.analysis;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.annotation.HarnessEntity;
import io.harness.annotation.IgnoreUnusedIndex;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldNameConstants;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Indexes;
import org.mongodb.morphia.utils.IndexType;

/**
 * ExperimentalMetricAnalysisRecord is the payload send after ML Analysis of Experimental Task.
 *
 * Created by Pranjal on 08/14/2018
 */
@Indexes({
  @Index(fields =
      { @Field("workflowExecutionId")
        , @Field("stateExecutionId"), @Field("analysisMinute"), @Field("groupName") },
      options = @IndexOptions(unique = true, name = "MetricAnalysisUniqueIdx"))
  ,
      @Index(fields = {
        @Field("analysisMinute"), @Field("mismatched"), @Field(value = "createdAt", type = IndexType.DESC)
      }, options = @IndexOptions(name = "ExperimentalMetricListIdx")), @Index(fields = {
        @Field("analysisMinute"), @Field("stateExecutionId")
      }, options = @IndexOptions(name = "analysisMinStateExecutionIdIndex"))
})
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
