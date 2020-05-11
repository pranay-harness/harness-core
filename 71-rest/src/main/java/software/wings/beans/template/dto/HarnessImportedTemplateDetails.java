package software.wings.beans.template.dto;

import static software.wings.common.TemplateConstants.HARNESS_COMMAND_LIBRARY_GALLERY;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Value;

import java.util.Set;

@JsonTypeName(HARNESS_COMMAND_LIBRARY_GALLERY)
@Value
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class HarnessImportedTemplateDetails implements ImportedTemplateDetails {
  private String commandVersion;
  private String commandName;
  private String commandStoreName;
  private transient String repoUrl;
  private transient Set<String> tags;
}
