package software.wings.beans;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import io.harness.data.validator.Trimmed;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.mongodb.morphia.annotations.Transient;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GitFileConfig {
  @Trimmed private String connectorId;
  @Trimmed private String commitId;
  @Trimmed private String branch;
  @Trimmed private String filePath;
  private boolean useBranch;
  @Transient @JsonInclude(Include.NON_EMPTY) private String connectorName;
}