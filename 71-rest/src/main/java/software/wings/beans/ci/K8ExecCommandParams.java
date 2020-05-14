package software.wings.beans.ci;

import lombok.Builder;
import lombok.Data;
import lombok.Value;

import java.util.List;
import javax.validation.constraints.NotNull;

@Data
@Value
@Builder
public class K8ExecCommandParams {
  @NotNull private String podName;
  @NotNull private String namespace;
  @NotNull private String containerName;
  @NotNull private List<String> commands;
  @NotNull private ShellScriptType scriptType;
  @NotNull private String mountPath;
  @NotNull private String relStderrFilePath; // Stderr file path relative to mount path
  @NotNull private String relStdoutFilePath; // Stdout file path relative to mount path
  @NotNull private Integer commandTimeoutSecs;
}