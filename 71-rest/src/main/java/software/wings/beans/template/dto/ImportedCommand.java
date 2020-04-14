package software.wings.beans.template.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class ImportedCommand {
  private String commandStoreName;
  private String commandStoreId;
  private String commandId;
  private String templateId;
  private String name;
  private String description;
  private String createdAt;
  private String createdBy;
  private List<ImportedCommandVersion> importedCommandVersionList;
  private String highestVersion;
}
