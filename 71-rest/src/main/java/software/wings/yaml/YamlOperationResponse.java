package software.wings.yaml;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class YamlOperationResponse {
  public enum Status { FAILED, SUCCESS }
  private Status responseStatus;
  private String errorMessage;
  private List<FileOperationStatus> filesStatus;
}
