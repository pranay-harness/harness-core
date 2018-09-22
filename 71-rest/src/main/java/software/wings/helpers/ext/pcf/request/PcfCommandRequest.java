package software.wings.helpers.ext.pcf.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.beans.PcfConfig;

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

  public enum PcfCommandType { SETUP, RESIZE, ROLLBACK, UPDATE_ROUTE, DATAFETCH, VALIDATE, APP_DETAILS }
}
