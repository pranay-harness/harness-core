package io.harness.beans;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.beans.YamlDTO;
import io.harness.gitsync.persistance.GitSyncableEntity;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UuidAware;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@Entity(value = "sampleBean1", noClassnameStored = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@Document("sampleBean1")
@FieldNameConstants(innerTypeName = "SampleBean1Keys")
@OwnedBy(HarnessTeam.DX)
public class SampleBean1 implements UuidAware, PersistentEntity, GitSyncableEntity, YamlDTO {
  @Id @org.mongodb.morphia.annotations.Id String uuid;
  String test1;
  String branch;
  String accountIdentifier;
  String projectIdentifier;
  String orgIdentifier;
  String identifier;
  String name;
  String objectIdOfYaml;
  Boolean isFromDefaultBranch;
  String yamlGitConfigId;
}
