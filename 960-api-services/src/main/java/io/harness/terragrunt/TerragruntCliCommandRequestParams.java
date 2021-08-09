package io.harness.terragrunt;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;

import java.io.File;
import java.util.Map;
import lombok.Builder;
import lombok.Data;

@OwnedBy(CDP)
@Data
@Builder
public class TerragruntCliCommandRequestParams {
  private String directory;
  private String backendConfigFilePath;
  private String commandUnitName;
  private Long timeoutInMillis;
  private Map<String, String> envVars;
  private String targetArgs;
  private String varParams;
  private String uiLogs;
  private String workspaceCommand;
  private File tfOutputsFile;
  private ActivityLogOutputStream activityLogOutputStream;
  private PlanLogOutputStream planLogOutputStream;
  private PlanJsonLogOutputStream planJsonLogOutputStream;
  private ErrorLogOutputStream errorLogOutputStream;
}
