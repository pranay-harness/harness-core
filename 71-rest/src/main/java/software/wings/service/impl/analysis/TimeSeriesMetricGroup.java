package software.wings.service.impl.analysis;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.beans.EmbeddedUser;
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
import software.wings.sm.StateType;

import java.time.OffsetDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Created by rsingh on 08/30/17.
 */
@Entity(value = "timeSeriesMetricGroup", noClassnameStored = true)
@Indexes({
  @Index(fields = {
    @Field("stateType"), @Field("stateExecutionId")
  }, options = @IndexOptions(unique = true, name = "uniqueIdx"))
})
@Data
@EqualsAndHashCode(callSuper = false)
public class TimeSeriesMetricGroup extends Base {
  @NotEmpty @Indexed private StateType stateType;

  @NotEmpty @Indexed private String stateExecutionId;

  @NotEmpty private Map<String, TimeSeriesMlAnalysisGroupInfo> groups;

  @SchemaIgnore
  @JsonIgnore
  @Indexed(options = @IndexOptions(expireAfterSeconds = 0))
  private Date validUntil = Date.from(OffsetDateTime.now().plusWeeks(1).toInstant());

  @Builder
  public TimeSeriesMetricGroup(String uuid, String appId, EmbeddedUser createdBy, long createdAt,
      EmbeddedUser lastUpdatedBy, long lastUpdatedAt, List<String> keywords, String entityYamlPath, StateType stateType,
      String stateExecutionId, Map<String, TimeSeriesMlAnalysisGroupInfo> groups) {
    super(uuid, appId, createdBy, createdAt, lastUpdatedBy, lastUpdatedAt, keywords, entityYamlPath);
    this.stateType = stateType;
    this.stateExecutionId = stateExecutionId;
    this.groups = groups;
  }

  @Data
  @EqualsAndHashCode(exclude = {"dependencyPath", "mlAnalysisType"})
  @Builder
  public static class TimeSeriesMlAnalysisGroupInfo {
    private String groupName;
    private String dependencyPath;
    private TimeSeriesMlAnalysisType mlAnalysisType;
  }
}
