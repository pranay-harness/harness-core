package software.wings.beans.template;

import static java.util.Arrays.asList;

import io.harness.data.validator.EntityName;
import io.harness.validation.Create;
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
import software.wings.beans.EmbeddedUser;

import java.util.ArrayList;
import java.util.List;

@Entity("templateGalleries")
@Indexes(
    @Index(fields = { @Field("name")
                      , @Field("accountId") }, options = @IndexOptions(name = "yaml", unique = true)))
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
public class TemplateGallery extends Base {
  public static final String ACCOUNT_NAME_KEY = "accountName";
  public static final String NAME_KEY = "name";
  @NotEmpty @EntityName(groups = Create.class) private String name;
  @NotEmpty private String accountId;
  private String description;
  private String referencedGalleryId;
  private boolean global;

  @Builder
  public TemplateGallery(String uuid, String appId, EmbeddedUser createdBy, long createdAt, EmbeddedUser lastUpdatedBy,
      long lastUpdatedAt, List<String> keywords, String entityYamlPath, String name, String accountId,
      String description, String referencedGalleryId, boolean global) {
    super(uuid, appId, createdBy, createdAt, lastUpdatedBy, lastUpdatedAt, keywords, entityYamlPath);
    this.name = name;
    this.accountId = accountId;
    this.description = description;
    this.referencedGalleryId = referencedGalleryId;
    this.global = global;
  }

  @Override
  public List<Object> generateKeywords() {
    List<Object> keywords = new ArrayList<>();
    keywords.addAll(asList(name, description));
    keywords.addAll(super.generateKeywords());
    return keywords;
  }
}
