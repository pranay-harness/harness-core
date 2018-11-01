package software.wings.service.impl.analysis;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.wings.api.InstanceElement;

import java.util.concurrent.TimeUnit;
import javax.validation.constraints.NotNull;

/**
 * Created by rsingh on 8/3/18.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@NoArgsConstructor
public class SetupTestNodeData {
  @NotNull private String appId;
  @NotNull private String settingId;
  private String instanceName;
  @JsonProperty private boolean isServiceLevel;
  private InstanceElement instanceElement;
  private String hostExpression;
  private String workflowId;
  private long toTime = System.currentTimeMillis() / TimeUnit.SECONDS.toMillis(1);
  private long fromTime = toTime - TimeUnit.MINUTES.toMillis(15) / TimeUnit.SECONDS.toMillis(1);

  public SetupTestNodeData(String appId, String settingId, String instanceName, boolean isServiceLevel,
      InstanceElement instanceElement, String hostExpression, String workflowId, long fromTime, long toTime) {
    this.appId = appId;
    this.settingId = settingId;
    this.instanceName = instanceName;
    this.isServiceLevel = isServiceLevel;
    this.instanceElement = instanceElement;
    this.hostExpression = hostExpression;
    this.workflowId = workflowId;
    this.fromTime = fromTime;
    this.toTime = toTime;
  }
}
