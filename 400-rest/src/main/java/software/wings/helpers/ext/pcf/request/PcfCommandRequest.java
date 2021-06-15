package software.wings.helpers.ext.pcf.request;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.pcf.model.CfCliVersion;

import software.wings.beans.PcfConfig;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.hibernate.validator.constraints.NotEmpty;

@Data
@AllArgsConstructor
@TargetModule(HarnessModule._950_DELEGATE_TASKS_BEANS)
@OwnedBy(CDP)
public class PcfCommandRequest {
  private String accountId;
  private String appId;
  private String commandName;
  private String activityId;
  @NotEmpty private PcfCommandType pcfCommandType;
  private String organization;
  private String space;
  private PcfConfig pcfConfig;
  private String workflowExecutionId;
  private Integer timeoutIntervalInMin;
  private boolean useCfCLI;
  private boolean enforceSslValidation;
  private boolean useAppAutoscalar;
  private boolean limitPcfThreads;
  private boolean ignorePcfConnectionContextCache;
  private CfCliVersion cfCliVersion;

  public enum PcfCommandType {
    SETUP,
    RESIZE,
    ROLLBACK,
    UPDATE_ROUTE,
    DATAFETCH,
    VALIDATE,
    APP_DETAILS,
    CREATE_ROUTE,
    RUN_PLUGIN
  }
}
