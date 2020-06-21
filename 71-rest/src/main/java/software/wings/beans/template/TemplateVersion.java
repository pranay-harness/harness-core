package software.wings.beans.template;

import io.harness.annotation.HarnessEntity;
import io.harness.mongo.index.Field;
import io.harness.mongo.index.Index;
import io.harness.mongo.index.IndexOptions;
import io.harness.mongo.index.IndexType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import software.wings.beans.Base;
import software.wings.beans.template.TemplateVersion.TemplateVersionKeys;

@FieldNameConstants(innerTypeName = "TemplateVersionKeys")

@Index(name = "yaml", fields = { @Field("templateUuid")
                                 , @Field("version") }, options = @IndexOptions(unique = true))
@Index(name = "account_template_version",
    fields =
    {
      @Field(value = TemplateVersionKeys.accountId)
      , @Field(value = TemplateVersionKeys.templateUuid),
          @Field(value = TemplateVersionKeys.version, type = IndexType.DESC)
    })
@Index(name = "account_imported_template_version",
    fields =
    {
      @Field(value = TemplateVersionKeys.accountId)
      , @Field(value = TemplateVersionKeys.templateUuid), @Field(value = TemplateVersionKeys.importedTemplateVersion)
    })
// TODO(abhinav): May have to look at ordering for importedTemplateVersion later.
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Entity(value = "templateVersions", noClassnameStored = true)
@HarnessEntity(exportable = true)
public class TemplateVersion extends Base {
  public static final long INITIAL_VERSION = 1;
  public static final String TEMPLATE_UUID_KEY = "templateUuid";
  private String changeType;
  private String templateUuid;
  private String templateName;
  private String templateType;

  private Long version;
  private String versionDetails;
  private String importedTemplateVersion;
  @NotEmpty private String accountId;
  private String galleryId;

  public enum ChangeType { CREATED, UPDATED, IMPORTED }
}
