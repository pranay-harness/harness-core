package software.wings.audit;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonView;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.annotation.HarnessExportableEntity;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UuidAware;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import software.wings.jersey.JsonViews;

import javax.validation.constraints.NotNull;

@Data
@Builder
@Entity(value = "entityAuditRecords", noClassnameStored = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@HarnessExportableEntity

@FieldNameConstants(innerTypeName = "AuditRecordKeys")
public class AuditRecord implements PersistentEntity, UuidAware, CreatedAtAware {
  @Id private String uuid;
  @NotEmpty private String accountId;

  @NotEmpty String auditHeaderId;
  @NotNull EntityAuditRecord entityAuditRecord;
  @JsonView(JsonViews.Internal.class) @SchemaIgnore private long createdAt;
}
