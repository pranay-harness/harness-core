package software.wings.beans.template;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.harness.annotation.HarnessEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexes;
import software.wings.beans.Base;
import software.wings.beans.Variable;

import java.util.List;
import javax.validation.constraints.NotNull;

@JsonInclude(NON_NULL)
@Indexes({
  @Index(fields = { @Field("accountId")
                    , @Field("templateId"), @Field("version") },
      options = @IndexOptions(name = "yaml", unique = true))
  ,
      @Index(fields = { @Field("templateObject.referencedTemplateList.templateReference.templateUuid") },
          options = @IndexOptions(name = "referencedTemplates"))
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Entity(value = "versionedTemplate", noClassnameStored = true)
@HarnessEntity(exportable = true)
public class VersionedTemplate extends Base {
  public static final String TEMPLATE_ID_KEY = "templateId";
  public static final String VERSION_KEY = "version";

  private String templateId;
  private Long version;
  private String importedTemplateVersion;
  private String accountId;
  private String galleryId;
  @NotNull private BaseTemplate templateObject;
  private List<Variable> variables;
}
