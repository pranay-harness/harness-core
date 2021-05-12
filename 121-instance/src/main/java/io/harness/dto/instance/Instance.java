package io.harness.dto.instance;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.EnvironmentType;
import io.harness.dto.InstanceType;
import io.harness.dto.instanceinfo.InstanceInfo;
import io.harness.mongo.index.CompoundMongoIndex;
import io.harness.mongo.index.MongoIndex;

import software.wings.beans.infrastructure.instance.key.PodInstanceKey;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.common.collect.ImmutableList;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@FieldNameConstants(innerTypeName = "InstanceKeys")
@Entity(value = "instances", noClassnameStored = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@Document("instances")
@OwnedBy(HarnessTeam.DX)
public class Instance {
  public static List<MongoIndex> mongoIndexes() {
    // TODO add more indexes
    return ImmutableList.<MongoIndex>builder()
        .add(CompoundMongoIndex.builder()
                 .name("unique_identification")
                 .unique(true)
                 .field(InstanceKeys.accountIdentifier)
                 .field(InstanceKeys.orgIdentifier)
                 .field(InstanceKeys.projectIdentifier)
                 .build())
        .build();
  }

  private String accountIdentifier;
  private String orgIdentifier;
  private String projectIdentifier;
  @NotEmpty private InstanceType instanceType;
  private PodInstanceKey podInstanceKey;

  private String envId;
  private String envName;
  private EnvironmentType envType;

  private String serviceId;
  private String serviceName;

  private String infrastructureMappingId;
  private String infraMappingType;

  private String connectorId;
  private String lastArtifactId;
  private String lastArtifactName;
  private String lastArtifactBuildNum;

  private String lastDeployedById;
  private String lastDeployedByName;
  private long lastDeployedAt;

  private String lastPipelineExecutionId;
  private String lastPipelineExecutionName;

  private InstanceInfo instanceInfo;

  private boolean isDeleted;
  private long deletedAt;
  @CreatedDate Long createdAt;
  @LastModifiedDate Long lastModifiedAt;

  private boolean needRetry;
}
