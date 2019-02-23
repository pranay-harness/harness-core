package software.wings.beans.template;

import static java.util.Arrays.asList;

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

import java.util.ArrayList;
import java.util.List;

@Entity("templateGalleries")
@HarnessExportableEntity
@Indexes(
    @Index(fields = { @Field("name")
                      , @Field("accountId") }, options = @IndexOptions(name = "yaml", unique = true)))
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class TemplateGallery extends Base {
  public static final String ACCOUNT_NAME_KEY = "accountName";
  public static final String NAME_KEY = "name";
  @NotEmpty @EntityName(groups = {Create.class, Update.class}) @NaturalKey private String name;
  @NotEmpty @NaturalKey private String accountId;
  private String description;
  private String referencedGalleryId;
  private boolean global;
  @SchemaIgnore @Indexed private List<String> keywords;

  @Builder
  public TemplateGallery(String uuid, String appId, EmbeddedUser createdBy, long createdAt, EmbeddedUser lastUpdatedBy,
      long lastUpdatedAt, List<String> keywords, String entityYamlPath, String name, String accountId,
      String description, String referencedGalleryId, boolean global) {
    super(uuid, appId, createdBy, createdAt, lastUpdatedBy, lastUpdatedAt, entityYamlPath);
    this.name = name;
    this.accountId = accountId;
    this.description = description;
    this.referencedGalleryId = referencedGalleryId;
    this.global = global;
    this.keywords = keywords;
  }

  @Override
  public List<Object> generateKeywords() {
    List<Object> keywords = new ArrayList<>();
    keywords.addAll(asList(name, description));
    keywords.addAll(super.generateKeywords());
    return keywords;
  }
}
