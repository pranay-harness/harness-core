package software.wings.beans.template;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

import io.harness.annotation.HarnessEntity;
import io.harness.mongo.index.CdIndex;
import io.harness.mongo.index.Field;
import io.harness.mongo.index.NgUniqueIndex;
import io.harness.persistence.AccountAccess;

import software.wings.beans.Base;
import software.wings.beans.Variable;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.mongodb.morphia.annotations.Entity;

@JsonInclude(NON_NULL)
@NgUniqueIndex(name = "yaml", fields = { @Field("accountId")
                                         , @Field("templateId"), @Field("version") })
@CdIndex(name = "referencedTemplates",
    fields = { @Field("templateObject.referencedTemplateList.templateReference.templateUuid") })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Entity(value = "versionedTemplate", noClassnameStored = true)
@HarnessEntity(exportable = true)
public class VersionedTemplate extends Base implements AccountAccess {
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
