package software.wings.helpers.ext.pcf.request;

import software.wings.beans.PcfConfig;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.hibernate.validator.constraints.NotEmpty;

@Data
@AllArgsConstructor
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
