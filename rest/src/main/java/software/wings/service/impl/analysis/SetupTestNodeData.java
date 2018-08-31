package software.wings.service.impl.analysis;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder.Default;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.wings.api.InstanceElement;

import java.util.concurrent.TimeUnit;

/**
 * Created by rsingh on 8/3/18.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@NoArgsConstructor
public class SetupTestNodeData {
  private String appId;
  private String settingId;
  private String instanceName;
  private InstanceElement instanceElement;
  private String hostExpression;
  private String workflowId;
  @Default private long toTime = System.currentTimeMillis();
  @Default private long fromTime = toTime - TimeUnit.MINUTES.toMillis(15);

  public SetupTestNodeData(String appId, String settingId, String instanceName, InstanceElement instanceElement,
      String hostExpression, String workflowId, long fromTime, long toTime) {
    this.appId = appId;
    this.settingId = settingId;
    this.instanceName = instanceName;
    this.instanceElement = instanceElement;
    this.hostExpression = hostExpression;
    this.workflowId = workflowId;
    this.fromTime = fromTime;
    this.toTime = toTime;
  }
}
