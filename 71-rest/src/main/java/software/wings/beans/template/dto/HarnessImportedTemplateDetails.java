package software.wings.beans.template.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class HarnessImportedTemplateDetails implements ImportedTemplateDetails {
  // TODO: refactor to remove imported from all variables.
  private String importedCommandVersion;
  private String importedCommandId;
  private String importedCommandStoreId;
}
