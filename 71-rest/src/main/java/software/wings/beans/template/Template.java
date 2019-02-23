package software.wings.beans.template;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static java.util.Arrays.asList;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.annotation.HarnessExportableEntity;
import io.harness.annotation.NaturalKey;
import io.harness.beans.EmbeddedUser;
import io.harness.data.validator.EntityName;
import io.harness.validation.Create;
import io.harness.validation.Update;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Indexes;
import software.wings.beans.Base;
import software.wings.beans.Variable;

import java.util.ArrayList;
import java.util.List;
import javax.validation.constraints.NotNull;

@JsonInclude(NON_NULL)
@Entity(value = "templates", noClassnameStored = true)
@HarnessExportableEntity
@Indexes(@Index(options = @IndexOptions(name = "yaml", unique = true),
    fields = { @Field("accountId")
               , @Field("name"), @Field("folderId") }))
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class Template extends Base {
  public static final String FOLDER_ID_KEY = "folderId";
  public static final String FOLDER_PATH_ID_KEY = "folderPathId";
  public static final String GALLERY_ID_KEY = "galleryId";
  public static final String GALLERY_KEY = "gallery";
  public static final String KEYWORDS_KEY = "keywords";
  public static final String NAME_KEY = "name";
  public static final String TYPE_KEY = "type";
  public static final String VERSION_KEY = "version";

  @Indexed @NotNull @EntityName(groups = {Create.class, Update.class}) @NaturalKey private String name;
  @NotEmpty @NaturalKey private String accountId;
  private String type;
  @Indexed @NaturalKey private String folderId;
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
  @SchemaIgnore @Indexed private List<String> keywords;

  @Builder
  public Template(String uuid, String appId, EmbeddedUser createdBy, long createdAt, EmbeddedUser lastUpdatedBy,
      long lastUpdatedAt, List<String> keywords, String entityYamlPath, String name, String accountId, String type,
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
  public List<Object> generateKeywords() {
    List<Object> keywords = new ArrayList<>();
    keywords.addAll(asList(name, description, type));
    keywords.addAll(super.generateKeywords());
    return keywords;
  }
}
