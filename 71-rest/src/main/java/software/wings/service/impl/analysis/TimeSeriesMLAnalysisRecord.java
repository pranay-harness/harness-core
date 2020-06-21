package software.wings.service.impl.analysis;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.annotation.HarnessEntity;
import io.harness.mongo.index.Field;
import io.harness.mongo.index.Index;
import io.harness.mongo.index.IndexType;
import io.harness.mongo.index.Indexes;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.mongodb.morphia.annotations.Entity;

/**
 * Class representing entity for TimeSeries Analysis Record.
 * Created by sriram_parthasarathy on 9/22/17.
 */
@Indexes({
  @Index(fields =
      { @Field("stateExecutionId")
        , @Field("groupName"), @Field(value = "analysisMinute", type = IndexType.DESC) },
      name = "stateExIdx")
  ,
      @Index(fields = { @Field("cvConfigId")
                        , @Field(value = "analysisMinute", type = IndexType.DESC) },
          name = "service_guard_idx"),
      @Index(fields = { @Field("workflowExecutionId")
                        , @Field(value = "appId") }, name = "workflow_exec_appId_index")
})
@Data
@Builder
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@Entity(value = "timeSeriesAnalysisRecords", noClassnameStored = true)
@HarnessEntity(exportable = false)
public class TimeSeriesMLAnalysisRecord extends MetricAnalysisRecord {}
