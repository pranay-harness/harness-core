package software.wings.yaml;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileOperationStatus {
  public enum Status { FAILED, SUCCESS, SKIPPED }
  private String yamlFilePath;
  private Status status;
  private String errorMssg;
}
