package software.wings.yaml;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@OwnedBy(DX)
public class FileOperationStatus {
  public enum Status { FAILED, SUCCESS, SKIPPED }
  private String yamlFilePath;
  private Status status;
  private String errorMssg;
  private String entityId;
}
