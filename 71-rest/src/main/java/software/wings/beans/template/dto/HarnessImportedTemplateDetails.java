package software.wings.beans.template.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class HarnessImportedTemplateDetails implements ImportedTemplateDetails {
  private String commandVersion;
  private String commandName;
  private String commandStoreName;
}
