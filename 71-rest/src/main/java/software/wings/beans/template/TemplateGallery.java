package software.wings.beans.template;

import static java.util.Arrays.asList;

import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.annotation.HarnessEntity;
import io.harness.beans.EmbeddedUser;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexes;
import software.wings.beans.Base;
import software.wings.beans.entityinterface.KeywordsAware;

import java.util.Set;

@Indexes({
  @Index(options = @IndexOptions(name = "account_gallery_key_idx"), fields = {
    @Field("accountId"), @Field("galleryKey")
  })
})
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Entity("templateGalleries")
@HarnessEntity(exportable = true)
public class TemplateGallery extends Base implements KeywordsAware {
  public static final String ACCOUNT_NAME_KEY = "accountName";
  public static final String NAME_KEY = "name";
  public static final String GALLERY_KEY = "galleryKey";

  public static final String IMPORTED_TEMPLATE_GALLERY_NAME = "Saved Templates";

  @NotEmpty private String name;
  @NotEmpty private String accountId;
  private String description;
  private String referencedGalleryId;
  private boolean global;
  @SchemaIgnore private Set<String> keywords;
  private String galleryKey;

  public enum GalleryKey { HARNESS_COMMAND_LIBRARY_GALLERY, ACCOUNT_TEMPLATE_GALLERY }

  @Builder
  public TemplateGallery(String uuid, String appId, EmbeddedUser createdBy, long createdAt, EmbeddedUser lastUpdatedBy,
      long lastUpdatedAt, Set<String> keywords, String entityYamlPath, String name, String accountId,
      String description, String referencedGalleryId, boolean global, String galleryKey) {
    super(uuid, appId, createdBy, createdAt, lastUpdatedBy, lastUpdatedAt, entityYamlPath);
    this.name = name;
    this.accountId = accountId;
    this.description = description;
    this.referencedGalleryId = referencedGalleryId;
    this.global = global;
    this.keywords = keywords;
    this.galleryKey = galleryKey;
  }

  @Override
  public Set<String> generateKeywords() {
    Set<String> keywords = KeywordsAware.super.generateKeywords();
    keywords.addAll(asList(name, description));
    return keywords;
  }
}
