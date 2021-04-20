package io.harness.beans;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.beans.YamlDTO;
import io.harness.gitsync.persistance.GitSyncableEntity;
import io.harness.persistence.PersistentEntity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@Entity(value = "sampleBean", noClassnameStored = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@Document("sampleBean")
@FieldNameConstants(innerTypeName = "SampleBeanKeys")
@OwnedBy(HarnessTeam.DX)
public class SampleBean extends GitSyncableEntity implements PersistentEntity, YamlDTO {
  String test1;
  String name;
  String accountIdentifier;
  String projectIdentifier;
  String orgIdentifier;
  String identifier;
}
