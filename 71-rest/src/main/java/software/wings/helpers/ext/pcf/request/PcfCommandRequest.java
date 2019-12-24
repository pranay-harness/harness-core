package software.wings.helpers.ext.pcf.request;

import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.delegate.task.mixin.ProcessExecutorCapabilityGenerator;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.beans.PcfConfig;

import java.util.Arrays;
import java.util.List;

@Data
@AllArgsConstructor
public class PcfCommandRequest implements ExecutionCapabilityDemander {
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

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities() {
    return Arrays.asList(ProcessExecutorCapabilityGenerator.buildProcessExecutorCapability(
        "PCF", Arrays.asList("/bin/sh", "-c", "cf --version")));
  }
}
