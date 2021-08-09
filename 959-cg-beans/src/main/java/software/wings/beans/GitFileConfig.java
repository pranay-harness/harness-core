package software.wings.beans;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.data.validator.Trimmed;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import java.util.List;
import javax.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Transient;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldNameConstants(innerTypeName = "GitFileConfigKeys")
@OwnedBy(CDP)
public class GitFileConfig {
  @Trimmed private String connectorId;
  @Trimmed private String commitId;
  @Trimmed private String branch;
  @Trimmed private String filePath;
  @Trimmed @Nullable private String repoName;
  private List<String> filePathList;
  @Trimmed @Nullable private String serviceSpecFilePath;
  @Trimmed @Nullable private String taskSpecFilePath;
  private boolean useBranch;
  private boolean useInlineServiceDefinition;
  @Transient @JsonInclude(Include.NON_EMPTY) private String connectorName;
}
