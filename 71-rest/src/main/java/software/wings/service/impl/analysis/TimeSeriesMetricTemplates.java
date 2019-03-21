package software.wings.service.impl.analysis;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.reinert.jjschema.SchemaIgnore;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Indexes;
import software.wings.beans.Base;
import software.wings.metrics.TimeSeriesMetricDefinition;
import software.wings.sm.StateType;

import java.time.OffsetDateTime;
import java.util.Date;
import java.util.Map;

/**
 * Created by rsingh on 08/30/17.
 */
@Entity(value = "timeSeriesMetricTemplates", noClassnameStored = true)
@Indexes({
  @Index(fields = {
    @Field("stateType"), @Field("stateExecutionId"), @Field("cvConfigId"), @Field("accountId")
  }, options = @IndexOptions(unique = true, name = "uniqueIdx"))
})
@Data
@EqualsAndHashCode(callSuper = false)
public class TimeSeriesMetricTemplates extends Base {
  @NotEmpty private StateType stateType;

  @NotEmpty @Indexed private String stateExecutionId;

  private String cvConfigId;

  private String accountId;

  @NotEmpty private Map<String, TimeSeriesMetricDefinition> metricTemplates;

  @JsonIgnore @SchemaIgnore @Indexed(options = @IndexOptions(expireAfterSeconds = 0)) private Date validUntil;

  @Builder
  public TimeSeriesMetricTemplates(StateType stateType, String stateExecutionId, String cvConfigId, String accountId,
      Map<String, TimeSeriesMetricDefinition> metricTemplates) {
    this.stateType = stateType;
    this.stateExecutionId = stateExecutionId;
    this.cvConfigId = cvConfigId;
    this.accountId = accountId;
    this.metricTemplates = metricTemplates;
    this.validUntil = isEmpty(stateExecutionId) ? null : Date.from(OffsetDateTime.now().plusMonths(1).toInstant());
  }
}
