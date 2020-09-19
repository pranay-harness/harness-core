package io.harness.beans;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonView;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.annotation.HarnessEntity;
import io.harness.annotation.StoreIn;
import io.harness.data.validator.EntityIdentifier;
import io.harness.mongo.index.FdIndex;
import io.harness.ng.core.ProjectAccess;
import io.harness.persistence.AccountAccess;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UuidAware;
import io.harness.validation.Update;
import io.harness.yaml.core.auxiliary.intfc.StageElementWrapper;
import io.harness.yaml.core.intfc.Pipeline;
import io.harness.yaml.core.nonyaml.WithNonYamlInfo;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;
import software.wings.jersey.JsonViews;

import java.util.List;
import javax.validation.constraints.NotNull;

@Data
@Builder
@FieldNameConstants(innerTypeName = "Pipeline")
@Entity(value = "cipipeline", noClassnameStored = true)
@HarnessEntity(exportable = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@StoreIn("harnessci")
@Document("cipipeline")
public class CIPipeline implements Pipeline, WithNonYamlInfo<CIPipelineEntityInfo>, PersistentEntity, UuidAware,
                                   AccountAccess, ProjectAccess {
  @EntityIdentifier private String identifier;
  private String name;
  private String description;
  private List<String> tags;
  private List<StageElementWrapper> stages;

  @FdIndex private String accountId;
  private String projectId;
  private String organizationId;
  @Version private Long version;

  @Id @org.springframework.data.annotation.Id @NotNull(groups = {Update.class}) @SchemaIgnore private String uuid;

  @Getter @JsonView(JsonViews.Public.class) CIPipelineEntityInfo nonYamlInfo;
}
