package software.wings.helpers.ext.pcf.request;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.beans.PcfConfig;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
@TargetModule(HarnessModule._950_DELEGATE_TASKS_BEANS)
@OwnedBy(CDP)
public class PcfInfraMappingDataRequest extends PcfCommandRequest {
  private PcfConfig pcfConfig;
  private String host;
  private String domain;
  private String path;
  private Integer port;
  private boolean useRandomPort;
  private boolean tcpRoute;
  private String applicationNamePrefix;
  private ActionType actionType;
  public enum ActionType { RUNNING_COUNT, FETCH_ORG, FETCH_SPACE, FETCH_ROUTE }

  @Builder
  public PcfInfraMappingDataRequest(String accountId, String appId, String commandName, String activityId,
      PcfCommandType pcfCommandType, String organization, String space, PcfConfig pcfConfig, String workflowExecutionId,
      Integer timeoutIntervalInMin, String host, String domain, String path, Integer port, boolean useRandomPort,
      boolean tcpRoute, String applicationNamePrefix, ActionType actionType, boolean useCLIForPcfAppCreation,
      boolean limitPcfThreads, boolean ignorePcfConnectionContextCache, boolean useCfCLI7) {
    super(accountId, appId, commandName, activityId, pcfCommandType, organization, space, pcfConfig,
        workflowExecutionId, timeoutIntervalInMin, useCLIForPcfAppCreation, false, false, limitPcfThreads,
        ignorePcfConnectionContextCache, useCfCLI7);
    this.pcfConfig = pcfConfig;
    this.host = host;
    this.domain = domain;
    this.path = path;
    this.port = port;
    this.useRandomPort = useRandomPort;
    this.tcpRoute = tcpRoute;
    this.applicationNamePrefix = applicationNamePrefix;
    this.actionType = actionType;
  }
}
