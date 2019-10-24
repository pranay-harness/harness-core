package software.wings.beans.infrastructure.instance.info;

import lombok.Data;
import lombok.experimental.FieldNameConstants;
import software.wings.beans.infrastructure.instance.InvocationCount;
import software.wings.beans.infrastructure.instance.InvocationCount.InvocationCountKey;

import java.util.Map;

@Data
@FieldNameConstants(innerTypeName = "ServerlessInstanceInfoKeys")
public abstract class ServerlessInstanceInfo {
  private Map<InvocationCountKey, InvocationCount> invocationCountMap;

  public ServerlessInstanceInfo(Map<InvocationCountKey, InvocationCount> invocationCountMap) {
    this.invocationCountMap = invocationCountMap;
  }
}
