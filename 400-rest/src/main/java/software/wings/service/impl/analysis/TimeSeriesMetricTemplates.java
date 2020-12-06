package software.wings.service.impl.analysis;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.annotation.HarnessEntity;
import io.harness.mongo.index.FdIndex;
import io.harness.mongo.index.FdTtlIndex;
import io.harness.mongo.index.Field;
import io.harness.mongo.index.NgUniqueIndex;
import io.harness.persistence.AccountAccess;

import software.wings.beans.Base;
import software.wings.metrics.TimeSeriesMetricDefinition;
import software.wings.sm.StateType;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.reinert.jjschema.SchemaIgnore;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldNameConstants;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;

/**
 * Created by rsingh on 08/30/17.
 */

@NgUniqueIndex(name = "unique_Idx", fields = { @Field("stateExecutionId")
                                               , @Field("cvConfigId") })
@Data
@EqualsAndHashCode(callSuper = false)
@FieldNameConstants(innerTypeName = "TimeSeriesMetricTemplatesKeys")
@Entity(value = "timeSeriesMetricTemplates", noClassnameStored = true)
@HarnessEntity(exportable = true)
public class TimeSeriesMetricTemplates extends Base implements AccountAccess {
  @NotEmpty private StateType stateType;

  @NotEmpty private String stateExecutionId;

  @FdIndex private String cvConfigId;

  @FdIndex private String accountId;

  @NotEmpty private Map<String, TimeSeriesMetricDefinition> metricTemplates;

  @JsonIgnore @SchemaIgnore @FdTtlIndex private Date validUntil;

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
