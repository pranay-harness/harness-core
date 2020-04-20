package software.wings.service.impl.analysis;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.annotation.HarnessEntity;
import io.harness.beans.EmbeddedUser;
import io.harness.persistence.AccountAccess;
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
import software.wings.beans.Base;
import software.wings.sm.StateType;

import java.time.OffsetDateTime;
import java.util.Date;
import java.util.Map;

/**
 * Created by rsingh on 08/30/17.
 */
@Indexes({
  @Index(fields = {
    @Field("stateType"), @Field("stateExecutionId")
  }, options = @IndexOptions(unique = true, name = "uniqueIdx"))
})
@Data
@EqualsAndHashCode(callSuper = false)
@Entity(value = "timeSeriesMetricGroup", noClassnameStored = true)
@HarnessEntity(exportable = false)
@FieldNameConstants(innerTypeName = "TimeSeriesMetricGroupKeys")
public class TimeSeriesMetricGroup extends Base implements AccountAccess {
  @NotEmpty private StateType stateType;

  @NotEmpty @Indexed private String stateExecutionId;

  @NotEmpty private Map<String, TimeSeriesMlAnalysisGroupInfo> groups;

  @Indexed private String accountId;

  @JsonIgnore
  @SchemaIgnore
  @Indexed(options = @IndexOptions(expireAfterSeconds = 0))
  private Date validUntil = Date.from(OffsetDateTime.now().plusWeeks(1).toInstant());

  @Builder
  public TimeSeriesMetricGroup(String uuid, String appId, EmbeddedUser createdBy, long createdAt,
      EmbeddedUser lastUpdatedBy, long lastUpdatedAt, String entityYamlPath, StateType stateType,
      String stateExecutionId, Map<String, TimeSeriesMlAnalysisGroupInfo> groups, String accountId) {
    super(uuid, appId, createdBy, createdAt, lastUpdatedBy, lastUpdatedAt, entityYamlPath);
    this.stateType = stateType;
    this.stateExecutionId = stateExecutionId;
    this.groups = groups;
    this.accountId = accountId;
  }

  @Data
  @Builder
  @EqualsAndHashCode(exclude = {"dependencyPath", "mlAnalysisType"})
  public static class TimeSeriesMlAnalysisGroupInfo {
    private String groupName;
    private String dependencyPath;
    private TimeSeriesMlAnalysisType mlAnalysisType;
  }
}
