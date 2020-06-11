package io.harness.cvng.core.services.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.annotation.HarnessEntity;
import io.harness.beans.EmbeddedUser;
import io.harness.cvng.beans.TimeSeriesMetricType;
import io.harness.cvng.beans.TimeSeriesThresholdCriteria;
import io.harness.cvng.models.DataSourceType;
import io.harness.data.validator.Trimmed;
import io.harness.persistence.AccountAccess;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UpdatedByAware;
import io.harness.persistence.UuidAware;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Indexes;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.validation.constraints.NotNull;

@Indexes({
  @Index(fields = {
    @Field("projectIdentifier"), @Field("dataSourceType"), @Field("identifier")
  }, options = @IndexOptions(name = "unique_Idx", unique = true))
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@Entity(value = "metricPacks", noClassnameStored = true)
@FieldNameConstants(innerTypeName = "MetricPackKeys")
@HarnessEntity(exportable = true)
public class MetricPack
    implements PersistentEntity, UuidAware, CreatedAtAware, UpdatedAtAware, AccountAccess, UpdatedByAware {
  @Id private String uuid;
  private long createdAt;
  private long lastUpdatedAt;
  private EmbeddedUser lastUpdatedBy;
  @Indexed private String accountId;
  @NotEmpty private String projectIdentifier;
  @NotNull private DataSourceType dataSourceType;
  @Trimmed @NotEmpty private String identifier;
  @NotEmpty private Set<MetricDefinition> metrics;

  @Data
  @Builder
  @EqualsAndHashCode(of = {"name"})
  public static class MetricDefinition {
    @Trimmed @NotEmpty private String name;
    @NotNull private TimeSeriesMetricType type;
    private String path;
    private String validationPath;
    private boolean included;
    @Default private List<TimeSeriesThresholdCriteria> ignoreHints = new ArrayList<>();
    @Default private List<TimeSeriesThresholdCriteria> failFastHints = new ArrayList<>();

    @JsonIgnore
    public String getPath() {
      return path;
    }

    @JsonIgnore
    public String getValidationPath() {
      return validationPath;
    }

    @JsonIgnore
    public TimeSeriesMetricType getType() {
      return type;
    }
  }
}
