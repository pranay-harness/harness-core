package software.wings.beans.template;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static java.util.Arrays.asList;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.annotation.HarnessEntity;
import io.harness.beans.EmbeddedUser;
import io.harness.data.validator.EntityName;
import io.harness.persistence.NameAccess;
import io.harness.validation.Create;
import io.harness.validation.Update;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.UtilityClass;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Indexes;
import software.wings.beans.Base;
import software.wings.beans.Variable;
import software.wings.beans.entityinterface.KeywordsAware;

import java.util.List;
import java.util.Set;
import javax.validation.constraints.NotNull;

@JsonInclude(NON_NULL)
@Indexes(@Index(options = @IndexOptions(name = "yaml", unique = true),
    fields = { @Field("accountId")
               , @Field("name"), @Field("folderId"), @Field("appId") }))
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
@FieldNameConstants(innerTypeName = "TemplateKeys")
@Entity(value = "templates", noClassnameStored = true)
@HarnessEntity(exportable = true)
public class Template extends Base implements KeywordsAware, NameAccess {
  public static final String FOLDER_ID_KEY = "folderId";
  public static final String FOLDER_PATH_ID_KEY = "folderPathId";
  public static final String GALLERY_ID_KEY = "galleryId";
  public static final String GALLERY_KEY = "gallery";
  public static final String KEYWORDS_KEY = "keywords";
  public static final String NAME_KEY = "name";
  public static final String TYPE_KEY = "type";
  public static final String VERSION_KEY = "version";
  public static final String REFERENCED_TEMPLATE_ID = "referencedTemplateId";
  public static final String APP_ID_KEY = "appId";

  @Indexed @NotNull @EntityName(groups = {Create.class, Update.class}) private String name;
  @NotEmpty private String accountId;
  private String type;
  @Indexed private String folderId;
  Long version;
  private String description;
  private String folderPathId;
  private transient String folderPath;
  private transient String gallery;
  @NotNull private transient BaseTemplate templateObject;
  private transient List<Variable> variables;
  private transient VersionedTemplate versionedTemplate;
  private String galleryId;
  private String referencedTemplateId;
  private Long referencedTemplateVersion;
  private transient String referencedTemplateUri;
  @SchemaIgnore private Set<String> keywords;

  @Builder
  public Template(String uuid, String appId, EmbeddedUser createdBy, long createdAt, EmbeddedUser lastUpdatedBy,
      long lastUpdatedAt, Set<String> keywords, String entityYamlPath, String name, String accountId, String type,
      String folderId, long version, String description, String folderPathId, String folderPath, String gallery,
      BaseTemplate templateObject, List<Variable> variables, VersionedTemplate versionedTemplate, String galleryId,
      String referencedTemplateId, Long referencedTemplateVersion) {
    super(uuid, appId, createdBy, createdAt, lastUpdatedBy, lastUpdatedAt, entityYamlPath);
    this.name = name;
    this.accountId = accountId;
    this.type = type;
    this.folderId = folderId;
    this.version = version;
    this.description = description;
    this.folderPathId = folderPathId;
    this.folderPath = folderPath;
    this.gallery = gallery;
    this.templateObject = templateObject;
    this.variables = variables;
    this.versionedTemplate = versionedTemplate;
    this.galleryId = galleryId;
    this.referencedTemplateId = referencedTemplateId;
    this.referencedTemplateVersion = referencedTemplateVersion;
    this.keywords = keywords;
  }

  @Override
  public Set<String> generateKeywords() {
    Set<String> keywords = KeywordsAware.super.generateKeywords();
    keywords.addAll(asList(name, description, type));
    return keywords;
  }

  @UtilityClass
  public static final class TemplateKeys {
    // Temporary
    public static final String createdAt = "createdAt";
    public static final String uuid = "uuid";
    public static final String name = "name";
    public static final String appId = "appId";
    public static final String accountId = "accountId";
  }
}