package software.wings.infra;

import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.beans.EmbeddedUser;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.CreatedByAware;
import io.harness.persistence.NameAccess;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UpdatedByAware;
import io.harness.persistence.UuidAware;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldNameConstants;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexes;
import software.wings.api.CloudProviderType;
import software.wings.api.DeploymentType;

import java.util.List;
import javax.validation.constraints.NotNull;

@Entity(value = "infrastructureDefinitions", noClassnameStored = true)
@Indexes(@Index(options = @IndexOptions(name = "infraDefinitionIdx", unique = true),
    fields = { @Field("appId")
               , @Field("envId"), @Field("name") }))
@Data
@Builder
@EqualsAndHashCode(callSuper = false)
@FieldNameConstants(innerTypeName = "InfrastructureDefinitionKeys")
public class InfrastructureDefinition
    implements PersistentEntity, UuidAware, NameAccess, CreatedAtAware, CreatedByAware, UpdatedAtAware, UpdatedByAware {
  @Id private String uuid;
  @SchemaIgnore private EmbeddedUser createdBy;
  @SchemaIgnore private long createdAt;
  @NotEmpty private String name;
  @SchemaIgnore private EmbeddedUser lastUpdatedBy;
  @SchemaIgnore @NotNull private long lastUpdatedAt;
  @NotNull protected String appId;
  private String provisionerId;
  @NotNull private CloudProviderType cloudProviderType;
  @NotNull private DeploymentType deploymentType;
  @NotNull private CloudProviderInfrastructure infrastructure;
  private List<String> scopedToServices;
  @NotNull private String envId;
}
