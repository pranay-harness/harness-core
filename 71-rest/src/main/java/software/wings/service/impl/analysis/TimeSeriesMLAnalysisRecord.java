package software.wings.service.impl.analysis;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexes;

/**
 * Class representing entity for TimeSeries Analysis Record.
 * Created by sriram_parthasarathy on 9/22/17.
 */
@Entity(value = "timeSeriesAnalysisRecords", noClassnameStored = true)
@Indexes({
  @Index(fields =
      {
        @Field("workflowExecutionId")
        , @Field("stateExecutionId"), @Field("analysisMinute"), @Field("groupName"), @Field("cvConfigId"), @Field("tag")
      },
      options = @IndexOptions(unique = true, name = "MetricAnalysisUniqueIdx"))
  ,
      @Index(fields = {
        @Field("analysisMinute"), @Field("appId"), @Field("cvConfigId")
      }, options = @IndexOptions(name = "service_guard_idx"))
})
@Data
@Builder
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class TimeSeriesMLAnalysisRecord extends MetricAnalysisRecord {}
