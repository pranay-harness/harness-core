package software.wings.beans.alert.cv;

import io.harness.alert.AlertData;

import software.wings.verification.CVConfiguration;

import lombok.Builder;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

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
    return StringUtils.equals(cvConfiguration.getUuid(),
        ((ContinuousVerificationDataCollectionAlert) alertData).getCvConfiguration().getUuid());
  }

  @Override
  public String buildTitle() {
    return message;
  }
}
