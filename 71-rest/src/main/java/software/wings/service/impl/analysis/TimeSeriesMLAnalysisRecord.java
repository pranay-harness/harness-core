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
@Indexes(@Index(fields =
    {
      @Field("workflowExecutionId")
      , @Field("stateExecutionId"), @Field("analysisMinute"), @Field("groupName"), @Field("cvConfigId")
    },
    options = @IndexOptions(unique = true, name = "MetricAnalysisUniqueIdx")))
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class TimeSeriesMLAnalysisRecord extends MetricAnalysisRecord {}
