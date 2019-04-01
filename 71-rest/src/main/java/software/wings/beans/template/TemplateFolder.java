package software.wings.beans.template;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static java.util.Arrays.asList;
import static software.wings.beans.Application.GLOBAL_APP_ID;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonValue;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.annotation.HarnessExportableEntity;
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
import software.wings.beans.KeywordsAware;

import java.util.ArrayList;
import java.util.List;

@Data
@Entity(value = "templateFolders", noClassnameStored = true)
@HarnessExportableEntity
@NoArgsConstructor
@JsonInclude(NON_NULL)
@EqualsAndHashCode(callSuper = false)
@Indexes(@Index(options = @IndexOptions(name = "duplicateKey", unique = true),
    fields = { @Field("accountId")
               , @Field("name"), @Field("pathId"), @Field("appId") }))
public class TemplateFolder extends Base implements KeywordsAware {
  public static final String GALLERY_ID_KEY = "galleryId";
  public static final String KEYWORDS_KEY = "keywords";
  public static final String NAME_KEY = "name";
  public static final String PARENT_ID_KEY = "parentId";
  public static final String PATH_ID_KEY = "pathId";
  public static final String PATH_KEY = "path";

  @NotEmpty private String accountId;
  @NotEmpty @EntityName(groups = {Create.class, Update.class}) String name;
  private String description;
  private String parentId;
  private transient String nodeType = NodeType.FILE.name();
  private String galleryId;
  private transient long templatesCount;
  private String pathId;
  private transient List<TemplateFolder> children = new ArrayList<>();

  @SchemaIgnore @Indexed private List<String> keywords;

  public enum NodeType {
    FOLDER("folder"),
    FILE("file");

    private String displayName;

    NodeType(String displayName) {
      this.displayName = displayName;
    }
    @JsonValue
    public String getDisplayName() {
      return displayName;
    }
  }

  @Builder
  public TemplateFolder(String uuid, String appId, EmbeddedUser createdBy, long createdAt, EmbeddedUser lastUpdatedBy,
      long lastUpdatedAt, List<String> keywords, String entityYamlPath, String accountId, String name,
      String description, String parentId, String nodeType, String galleryId, String pathId,
      List<TemplateFolder> children) {
    super(uuid, appId, createdBy, createdAt, lastUpdatedBy, lastUpdatedAt, entityYamlPath);
    this.accountId = accountId;
    this.name = name;
    this.description = description;
    this.parentId = parentId;
    this.nodeType = nodeType;
    this.galleryId = galleryId;
    this.pathId = pathId;
    this.children = children;
    this.keywords = keywords;
  }

  public TemplateFolder cloneInternal() {
    return TemplateFolder.builder().name(name).description(description).appId(GLOBAL_APP_ID).build();
  }

  public void addChild(TemplateFolder child) {
    if (isEmpty(children)) {
      children = new ArrayList<>();
    }
    children.add(child);
  }

  @Override
  public List<String> generateKeywords() {
    List<String> keywords = KeywordsAware.super.generateKeywords();
    keywords.addAll(asList(name, description));
    return keywords;
  }
}
