package software.wings.sm;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by rishi on 1/17/17.
 */
public class ParamsNotifyResponseData extends ExecutionStatusData {
  private Map<String, Object> params = new HashMap<>();

  public Map<String, Object> getParams() {
    return params;
  }

  public void setParams(Map<String, Object> params) {
    this.params = params;
  }
}
