package software.wings.beans.template;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static java.util.Arrays.asList;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonValue;
import com.mongodb.client.model.CollationStrength;
import io.harness.data.validator.EntityName;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Collation;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Indexes;
import software.wings.beans.Base;
import software.wings.beans.EmbeddedUser;
import software.wings.utils.validation.Create;

import java.util.ArrayList;
import java.util.List;

@Data
@Entity(value = "templateFolders", noClassnameStored = true)
@NoArgsConstructor
@JsonInclude(NON_NULL)
@EqualsAndHashCode(callSuper = false)
@Indexes(@Index(options = @IndexOptions(
                    name = "collation", collation = @Collation(locale = "en", strength = CollationStrength.PRIMARY)),
    fields = { @Field("name") }))
public class TemplateFolder extends Base {
  public static final String PATH_KEY = "path";
  public static final String PATH_ID_KEY = "pathId";
  public static final String NAME_KEY = "name";
  public static final String GALLERY_ID_KEY = "galleryId";

  @Indexed @NotEmpty private String accountId;
  @NotEmpty @EntityName(groups = Create.class) String name;
  private String description;
  private String parentId;
  private transient String nodeType = NodeType.FILE.name();
  private String galleryId;
  private transient long templatesCount;
  @Indexed private String pathId;
  private transient List<TemplateFolder> children = new ArrayList<>();

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
    super(uuid, appId, createdBy, createdAt, lastUpdatedBy, lastUpdatedAt, keywords, entityYamlPath);
    this.accountId = accountId;
    this.name = name;
    this.description = description;
    this.parentId = parentId;
    this.nodeType = nodeType;
    this.galleryId = galleryId;
    this.pathId = pathId;
    this.children = children;
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
  public List<Object> generateKeywords() {
    List<Object> keywords = new ArrayList<>();
    keywords.addAll(asList(name, description));
    keywords.addAll(super.generateKeywords());
    return keywords;
  }
}
