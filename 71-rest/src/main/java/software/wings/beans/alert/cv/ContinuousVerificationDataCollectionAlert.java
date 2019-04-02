package software.wings.beans.alert.cv;

import lombok.Builder;
import lombok.Data;
import software.wings.beans.alert.AlertData;

/**
 * Created by rsingh on 11/13/17.
 */
@Data
@Builder
public class ContinuousVerificationDataCollectionAlert implements AlertData {
  private String cvConfigId;
  private String message;

  @Override
  public boolean matches(AlertData alertData) {
    return cvConfigId.equals(((ContinuousVerificationDataCollectionAlert) alertData).cvConfigId);
  }

  @Override
  public String buildTitle() {
    return message;
  }
}
