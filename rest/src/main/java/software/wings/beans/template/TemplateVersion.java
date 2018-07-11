package software.wings.beans.template;

import lombok.AllArgsConstructor;
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

@Indexes(@Index(
    fields = { @Field("templateUuid")
               , @Field("version") }, options = @IndexOptions(name = "yaml", unique = true)))
@Entity(value = "templateVersions", noClassnameStored = true)
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TemplateVersion extends Base {
  public static final long INITIAL_VERSION = 1;
  public static String TEMPLATE_UUID_KEY = "templateUuid";
  private String changeType;
  private String templateUuid;
  private String templateName;
  private String templateType;
  private Long version;
  @Indexed @NotEmpty private String accountId;

  public enum ChangeType { CREATED, UPDATED }
}
