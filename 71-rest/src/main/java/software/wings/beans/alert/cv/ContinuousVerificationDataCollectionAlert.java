package software.wings.beans.alert.cv;

import lombok.Builder;
import lombok.Data;
import software.wings.beans.alert.AlertData;
import software.wings.verification.CVConfiguration;

/**
 * Created by rsingh on 11/13/17.
 */
@Data
@Builder
public class ContinuousVerificationDataCollectionAlert implements AlertData {
  private CVConfiguration cvConfiguration;
  private String message;

  @Override
  public boolean matches(AlertData alertData) {
    return cvConfiguration.getUuid().equals(
        ((ContinuousVerificationDataCollectionAlert) alertData).cvConfiguration.getUuid());
  }

  @Override
  public String buildTitle() {
    return message;
  }
}
