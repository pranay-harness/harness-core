package software.wings.beans.template;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.harness.annotation.HarnessEntity;
import io.harness.beans.EmbeddedUser;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.CreatedByAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UpdatedByAware;
import io.harness.persistence.UuidAware;
import io.harness.validation.Update;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Indexed;
import software.wings.beans.entityinterface.ApplicationAccess;

import javax.validation.constraints.NotNull;

@JsonInclude(NON_NULL)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
@FieldNameConstants(innerTypeName = "ImportedTemplateKeys")
@Entity(value = "importedTemplates", noClassnameStored = true)
@HarnessEntity(exportable = true)
public class ImportedTemplate implements PersistentEntity, UuidAware, CreatedAtAware, CreatedByAware, UpdatedAtAware,
                                         UpdatedByAware, ApplicationAccess {
  @Id @NotNull(groups = {Update.class}) private String uuid;

  @Indexed @NotNull protected String appId;

  private EmbeddedUser createdBy;

  @Indexed private long createdAt;

  private EmbeddedUser lastUpdatedBy;

  @NotNull private long lastUpdatedAt;

  @NotEmpty private String name;
  @NotEmpty private String commandStoreName;
  @NotEmpty private String commandName;
  @NotEmpty private String commandDisplayName;
  @NotEmpty private String templateId;
  private String description;
  private String imageUrl;
  @NotEmpty private String accountId;

  @Builder(toBuilder = true)
  public ImportedTemplate(String uuid, String appId, EmbeddedUser createdBy, long createdAt, EmbeddedUser lastUpdatedBy,
      long lastUpdatedAt, String name, String commandStoreName, String commandName, String templateId,
      String description, String imageUrl, String accountId) {
    this.uuid = uuid;
    this.appId = appId;
    this.createdBy = createdBy;
    this.createdAt = createdAt;
    this.lastUpdatedBy = lastUpdatedBy;
    this.lastUpdatedAt = lastUpdatedAt;
    this.name = name;
    this.commandStoreName = commandStoreName;
    this.commandName = commandName;
    this.templateId = templateId;
    this.description = description;
    this.imageUrl = imageUrl;
    this.accountId = accountId;
  }
}
