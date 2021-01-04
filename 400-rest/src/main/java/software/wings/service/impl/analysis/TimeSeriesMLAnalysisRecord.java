package software.wings.service.impl.analysis;

import io.harness.annotation.HarnessEntity;
import io.harness.mongo.index.CdIndex;
import io.harness.mongo.index.Field;
import io.harness.mongo.index.IndexType;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.mongodb.morphia.annotations.Entity;

/**
 * Class representing entity for TimeSeries Analysis Record.
 * Created by sriram_parthasarathy on 9/22/17.
 */

@CdIndex(name = "stateExIdx",
    fields =
    { @Field("stateExecutionId")
      , @Field("groupName"), @Field(value = "analysisMinute", type = IndexType.DESC) })
@CdIndex(name = "service_guard_idx",
    fields = { @Field("cvConfigId")
               , @Field(value = "analysisMinute", type = IndexType.DESC) })
@CdIndex(name = "workflow_exec_appId_index", fields = { @Field("workflowExecutionId")
                                                        , @Field(value = "appId") })
@Data
@Builder
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@Entity(value = "timeSeriesAnalysisRecords", noClassnameStored = true)
@HarnessEntity(exportable = false)
public class TimeSeriesMLAnalysisRecord extends MetricAnalysisRecord {}
