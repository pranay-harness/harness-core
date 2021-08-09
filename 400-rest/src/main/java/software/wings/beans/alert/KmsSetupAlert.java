package software.wings.beans.alert;

import io.harness.alert.AlertData;

import lombok.Builder;
import lombok.Data;

/**
 * Created by rsingh on 11/13/17.
 */
@Data
@Builder
public class KmsSetupAlert implements AlertData {
  private String kmsId;
  private String message;

  @Override
  public boolean matches(AlertData alertData) {
    return kmsId.equals(((KmsSetupAlert) alertData).kmsId);
  }

  @Override
  public String buildTitle() {
    return message;
  }

  @Override
  public String buildResolutionTitle() {
    return "Incident Resolved (" + message + ")";
  }
}
