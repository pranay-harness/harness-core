package software.wings.beans.template.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ImportedCommandVersion {
  private String commandStoreId;
  private String commandId;
  private String templateId;
  private String version;
  private String versionDetails;
  private String createdAt;
  private String createdBy;
}
