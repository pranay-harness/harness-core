package io.harness.cvng.analysis.entities;

import io.harness.annotation.HarnessEntity;
import io.harness.annotation.StoreIn;
import io.harness.cvng.analysis.beans.DeploymentLogAnalysisDTO.Cluster;
import io.harness.cvng.analysis.beans.DeploymentLogAnalysisDTO.ClusterCoordinates;
import io.harness.cvng.analysis.beans.DeploymentLogAnalysisDTO.HostSummary;
import io.harness.cvng.analysis.beans.DeploymentLogAnalysisDTO.ResultSummary;
import io.harness.mongo.index.CompoundMongoIndex;
import io.harness.mongo.index.MongoIndex;
import io.harness.ng.DbAliases;
import io.harness.persistence.AccountAccess;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UuidAware;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.common.collect.ImmutableList;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldNameConstants(innerTypeName = "DeploymentLogAnalysisKeys")
@JsonIgnoreProperties(ignoreUnknown = true)
@Entity(value = "deploymentLogAnalyses", noClassnameStored = true)
@HarnessEntity(exportable = false)
@StoreIn(DbAliases.CVNG)
public final class DeploymentLogAnalysis
    implements PersistentEntity, UuidAware, CreatedAtAware, UpdatedAtAware, AccountAccess {
  public static List<MongoIndex> mongoIndexes() {
    return ImmutableList.<MongoIndex>builder()
        .add(CompoundMongoIndex.builder()
                 .name("query_idx")
                 .field(DeploymentLogAnalysisKeys.verificationTaskId)
                 .field(DeploymentLogAnalysisKeys.startTime)
                 .build())
        .build();
  }

  @Id private String uuid;
  private long createdAt;
  private long lastUpdatedAt;
  private String accountId;
  private String verificationTaskId;
  private Instant startTime;
  private Instant endTime;
  private List<Cluster> clusters;
  List<ClusterCoordinates> clusterCoordinates;
  ResultSummary resultSummary;
  List<HostSummary> hostSummaries;

  public List<Cluster> getClusters() {
    if (this.clusters == null) {
      return Collections.emptyList();
    }
    return clusters;
  }

  public List<HostSummary> getHostSummaries() {
    if (this.hostSummaries == null) {
      return Collections.emptyList();
    }
    return hostSummaries;
  }

  public List<ClusterCoordinates> getClusterCoordinates() {
    if (clusterCoordinates == null) {
      return Collections.emptyList();
    }
    return clusterCoordinates;
  }
}
