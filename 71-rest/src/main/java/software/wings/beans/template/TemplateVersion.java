package software.wings.beans.template;

import io.harness.annotation.HarnessEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexes;
import org.mongodb.morphia.utils.IndexType;
import software.wings.beans.Base;
import software.wings.beans.template.TemplateVersion.TemplateVersionKeys;

@FieldNameConstants(innerTypeName = "TemplateVersionKeys")
@Indexes({
  @Index(fields = { @Field("templateUuid")
                    , @Field("version") }, options = @IndexOptions(name = "yaml", unique = true))
  , @Index(options = @IndexOptions(name = "account_template_version"), fields = {
    @Field(value = TemplateVersionKeys.accountId)
    , @Field(value = TemplateVersionKeys.templateUuid),
        @Field(value = TemplateVersionKeys.version, type = IndexType.DESC)
  })
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Entity(value = "templateVersions", noClassnameStored = true)
@HarnessEntity(exportable = true)
public class TemplateVersion extends Base {
  public static final long INITIAL_VERSION = 1;
  public static String TEMPLATE_UUID_KEY = "templateUuid";
  private String changeType;
  private String templateUuid;
  private String templateName;
  private String templateType;
  private Long version;
  @NotEmpty private String accountId;
  private String galleryId;

  public enum ChangeType { CREATED, UPDATED }
}
