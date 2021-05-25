package io.harness.entities;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.entities.deploymentinfo.DeploymentInfo;
import io.harness.entities.infrastructureMapping.InfrastructureMapping;
import io.harness.mongo.index.CompoundMongoIndex;
import io.harness.mongo.index.MongoIndex;
import io.harness.persistence.PersistentEntity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.common.collect.ImmutableList;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@FieldNameConstants(innerTypeName = "DeploymentSummaryKeys")
@Entity(value = "deploymentSummary", noClassnameStored = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@Document("deploymentSummary")
@OwnedBy(HarnessTeam.DX)
public class DeploymentSummary implements PersistentEntity {
  public static List<MongoIndex> mongoIndexes() {
    // TODO add more indexes
    return ImmutableList.<MongoIndex>builder()
        .add(CompoundMongoIndex.builder()
                 .name("unique_account_org_project_id")
                 .unique(true)
                 .field(DeploymentSummaryKeys.accountIdentifier)
                 .field(DeploymentSummaryKeys.orgIdentifier)
                 .field(DeploymentSummaryKeys.projectIdentifier)
                 .build())
        .build();
  }

  @Id @org.mongodb.morphia.annotations.Id private String id;
  private String accountIdentifier;
  private String orgIdentifier;
  private String projectIdentifier;
  private String pipelineExecutionId;
  private String pipelineExecutionName;
  private String artifactId;
  private String artifactName;
  private String artifactBuildNum;
  private String deployedById;
  private String deployedByName;
  private String infrastructureMappingId;
  private InfrastructureMapping infrastructureMapping;
  private long deployedAt;
  private DeploymentInfo deploymentInfo;
  @CreatedDate Long createdAt;
  @LastModifiedDate Long lastModifiedAt;
}
