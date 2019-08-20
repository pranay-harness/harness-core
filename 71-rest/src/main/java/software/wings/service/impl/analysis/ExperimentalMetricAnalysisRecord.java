package software.wings.service.impl.analysis;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
import org.mongodb.morphia.annotations.Indexes;

/**
 * ExperimentalMetricAnalysisRecord is the payload send after ML Analysis of Experimental Task.
 *
 * Created by Pranjal on 08/14/2018
 */
@Entity(value = "experimentalTimeSeriesAnalysisRecords", noClassnameStored = true)
@Indexes(@Index(fields =
    { @Field("workflowExecutionId")
      , @Field("stateExecutionId"), @Field("analysisMinute"), @Field("groupName") },
    options = @IndexOptions(unique = true, name = "MetricAnalysisUniqueIdx")))
@Data
@Builder
@EqualsAndHashCode(callSuper = false)
@JsonIgnoreProperties(ignoreUnknown = true)
@FieldNameConstants(innerTypeName = "ExperimentalMetricAnalysisRecordKeys")
@IgnoreUnusedIndex
public class ExperimentalMetricAnalysisRecord extends MetricAnalysisRecord {
  private String envId;
  @Builder.Default private boolean mismatched = true;
  @Builder.Default private ExperimentStatus experimentStatus = ExperimentStatus.UNDETERMINED;
  @NotEmpty private String experimentName;
}
