package software.wings.service.impl.analysis;

import io.harness.annotation.HarnessEntity;
import io.harness.annotation.IgnoreUnusedIndex;
import io.harness.annotation.StoreIn;
import io.harness.mongo.index.CompoundMongoIndex;
import io.harness.mongo.index.FdIndex;
import io.harness.mongo.index.MongoIndex;
import io.harness.mongo.index.SortCompoundMongoIndex;
import io.harness.ng.DbAliases;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.common.collect.ImmutableList;
import java.util.List;
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
@Data
@Builder
@EqualsAndHashCode(callSuper = false)
@JsonIgnoreProperties(ignoreUnknown = true)
@FieldNameConstants(innerTypeName = "ExperimentalMetricAnalysisRecordKeys")
@IgnoreUnusedIndex
@Entity(value = "experimentalTimeSeriesAnalysisRecords", noClassnameStored = true)
@HarnessEntity(exportable = false)
@StoreIn(DbAliases.CG_MANAGER)
public class ExperimentalMetricAnalysisRecord extends MetricAnalysisRecord {
  public static List<MongoIndex> mongoIndexes() {
    return ImmutableList.<MongoIndex>builder()
        .add(CompoundMongoIndex.builder()
                 .name("MetricAnalysisUniqueIdx")
                 .unique(true)
                 .field(MetricAnalysisRecordKeys.workflowExecutionId)
                 .field(MetricAnalysisRecordKeys.stateExecutionId)
                 .field(MetricAnalysisRecordKeys.analysisMinute)
                 .field(MetricAnalysisRecordKeys.groupName)
                 .build(),
            SortCompoundMongoIndex.builder()
                .name("ExperimentalMetricListIdx")
                .field(MetricAnalysisRecordKeys.analysisMinute)
                .field(ExperimentalMetricAnalysisRecordKeys.mismatched)
                .descSortField(CREATED_AT_KEY)
                .build(),
            CompoundMongoIndex.builder()
                .name("analysisMinStateExecutionIdIndex")
                .field(MetricAnalysisRecordKeys.analysisMinute)
                .field(MetricAnalysisRecordKeys.stateExecutionId)
                .build())
        .build();
  }
  private String envId;
  @Builder.Default @FdIndex private boolean mismatched = true;
  @Builder.Default private ExperimentStatus experimentStatus = ExperimentStatus.UNDETERMINED;
  @NotEmpty private String experimentName;
}
