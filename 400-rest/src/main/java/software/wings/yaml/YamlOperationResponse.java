package software.wings.yaml;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
