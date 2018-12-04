package io.harness.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Indexes;
import software.wings.beans.Base;

import java.util.Map;

/**
 * Class representing an entity of cumulative sums and risk for each window of analysis.
 * Created by Praveen.
 */

@Entity(value = "timeSeriesCumulativeSums", noClassnameStored = true)
@Indexes(@Index(fields = { @Field("appId")
                           , @Field("cvConfigId"), @Field("analysisMinute") },
    options = @IndexOptions(unique = true, name = "uniqueIdx")))
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@Builder
public class TimeSeriesCumulativeSums extends Base {
  @NotEmpty @Indexed private String cvConfigId;
  @NotEmpty @Indexed private int analysisMinute;
  private Map<String, Map<String, Map<String, Double>>> transactionMetricSums;
}
