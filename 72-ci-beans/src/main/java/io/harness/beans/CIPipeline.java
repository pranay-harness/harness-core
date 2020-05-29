package io.harness.beans;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonView;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.annotation.HarnessEntity;
import io.harness.annotation.StoreIn;
import io.harness.persistence.AccountAccess;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UuidAware;
import io.harness.validation.Update;
import io.harness.yaml.core.Tag;
import io.harness.yaml.core.intfc.Pipeline;
import io.harness.yaml.core.intfc.Stage;
import io.harness.yaml.core.nonyaml.WithNonYamlInfo;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Indexed;
import software.wings.jersey.JsonViews;

import java.util.List;
import javax.validation.constraints.NotNull;

@Data
@Builder
@Entity(value = "cipipeline", noClassnameStored = true)
@HarnessEntity(exportable = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@StoreIn("harnessci")
public class CIPipeline implements Pipeline, WithNonYamlInfo<CIPipelineEntityInfo>, PersistentEntity, UuidAware,
                                   CreatedAtAware, UpdatedAtAware, AccountAccess {
  private String identifier;
  private String name;
  private String description;
  List<Tag> tags;
  private List<Stage> stages;
  private EmbeddedUser createdBy;
  private long createdAt;

  private long lastUpdatedAt;

  @Indexed private String accountId;

  @Id @NotNull(groups = {Update.class}) @SchemaIgnore private String uuid;

  @Getter @JsonView(JsonViews.Public.class) CIPipelineEntityInfo nonYamlInfo;
}
