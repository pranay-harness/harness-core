package software.wings.beans.template.dto;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static software.wings.common.TemplateConstants.HARNESS_COMMAND_LIBRARY_GALLERY;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;

import java.util.Set;

@JsonTypeName(HARNESS_COMMAND_LIBRARY_GALLERY)
@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(NON_NULL)
public class HarnessImportedTemplateDetails implements ImportedTemplateDetails {
  private String commandVersion;
  private String commandName;
  private String commandStoreName;
  private transient String repoUrl;
  private transient Set<String> tags;
}
