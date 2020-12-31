package io.harness.cvng.core.entities;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.annotation.HarnessEntity;
import io.harness.cvng.beans.CVMonitoringCategory;
import io.harness.cvng.beans.DataSourceType;
import io.harness.cvng.beans.MetricPackDTO;
import io.harness.cvng.beans.MetricPackDTO.MetricDefinitionDTO;
import io.harness.cvng.beans.TimeSeriesMetricType;
import io.harness.data.validator.Trimmed;
import io.harness.mongo.index.CompoundMongoIndex;
import io.harness.mongo.index.MongoIndex;
import io.harness.persistence.AccountAccess;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UuidAware;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@Entity(value = "metricPacks", noClassnameStored = true)
@FieldNameConstants(innerTypeName = "MetricPackKeys")
@HarnessEntity(exportable = true)
public class MetricPack implements PersistentEntity, UuidAware, CreatedAtAware, UpdatedAtAware, AccountAccess {
  public static List<MongoIndex> mongoIndexes() {
    return ImmutableList.<MongoIndex>builder()
        .add(CompoundMongoIndex.builder()
                 .name("idx")
                 .unique(true)
                 .field(MetricPackKeys.accountId)
                 .field(MetricPackKeys.orgIdentifier)
                 .field(MetricPackKeys.projectIdentifier)
                 .field(MetricPackKeys.dataSourceType)
                 .field(MetricPackKeys.identifier)
                 .build())
        .build();
  }

  @Id private String uuid;
  private long createdAt;
  private long lastUpdatedAt;
  private String accountId;
  @NotEmpty private String orgIdentifier;
  @NotEmpty private String projectIdentifier;
  @NotNull private DataSourceType dataSourceType;
  @Trimmed @NotEmpty private String identifier;
  @NotNull private CVMonitoringCategory category;
  @NotEmpty private Set<MetricDefinition> metrics;
  private String dataCollectionDsl;
  @JsonIgnore
  public String getDataCollectionDsl() {
    return dataCollectionDsl;
  }

  public void addToMetrics(MetricDefinition metricDefinition) {
    if (this.metrics == null) {
      this.metrics = new HashSet<>();
    }
    this.metrics.add(metricDefinition);
  }

  public Set<MetricDefinition> getMetrics() {
    if (this.metrics == null) {
      return Collections.emptySet();
    }
    return metrics;
  }

  public MetricPackDTO toDTO() {
    return MetricPackDTO.builder()
        .uuid(getUuid())
        .accountId(getAccountId())
        .orgIdentifier(getOrgIdentifier())
        .projectIdentifier(getProjectIdentifier())
        .dataSourceType(getDataSourceType())
        .identifier(getIdentifier())
        .category(getCategory())
        .metrics(getMetrics().stream().map(MetricDefinition::toDTO).collect(Collectors.toSet()))
        .build();
  }

  @Data
  @Builder
  @EqualsAndHashCode(of = {"name"})
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class MetricDefinition {
    @Trimmed @NotEmpty private String name;
    @NotNull private TimeSeriesMetricType type;
    private String path;
    private String validationPath;
    private boolean included;
    @Builder.Default private List<TimeSeriesThreshold> thresholds = new ArrayList<>();
    @JsonIgnore
    public String getPath() {
      return path;
    }

    @JsonIgnore
    public String getValidationPath() {
      return validationPath;
    }

    public MetricDefinitionDTO toDTO() {
      return MetricDefinitionDTO.builder()
          .name(name)
          .path(path)
          .type(type)
          .validationPath(validationPath)
          .included(included)
          .thresholds(isEmpty(thresholds)
                  ? new ArrayList<>()
                  : thresholds.stream().map(TimeSeriesThreshold::toDTO).collect(Collectors.toList()))
          .build();
    }
  }
}
