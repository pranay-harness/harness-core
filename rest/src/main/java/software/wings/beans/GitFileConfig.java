package software.wings.beans;

import io.harness.data.validator.Trimmed;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GitFileConfig {
  @Trimmed private String connectorId;
  @Trimmed private String commitId;
  @Trimmed private String branch;
  @Trimmed private String filePath;
}