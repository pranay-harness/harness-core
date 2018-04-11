package software.wings.beans.alert;

import lombok.Builder;
import lombok.Data;

/**
 * Created by rsingh on 4/10/18.
 */
@Data
@Builder
public class NewRelicMetricNameCollectionAlert implements AlertData {
  private String configId;
  private String message;

  @Override
  public boolean matches(AlertData alertData) {
    return configId.equals(((NewRelicMetricNameCollectionAlert) alertData).configId);
  }

  @Override
  public String buildTitle() {
    return message;
  }
}
