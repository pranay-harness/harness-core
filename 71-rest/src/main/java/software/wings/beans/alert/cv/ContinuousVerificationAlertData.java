package software.wings.beans.alert.cv;

import static java.util.Objects.requireNonNull;

import lombok.Value;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.alert.AlertData;

import java.time.Instant;
import java.util.Objects;
import javax.annotation.Nonnull;

@Value
public class ContinuousVerificationAlertData implements AlertData {
  private static final Logger log = LoggerFactory.getLogger(ContinuousVerificationAlertData.class);

  @Nonnull private CVParams cvParams;
  private String message;
  private Instant alertTs = Instant.now();

  public ContinuousVerificationAlertData(CVParams cvParams, String message) {
    this.cvParams = requireNonNull(cvParams);
    this.message = message;
  }

  @Override
  public boolean matches(AlertData alertData) {
    ContinuousVerificationAlertData cvAlertData = (ContinuousVerificationAlertData) alertData;
    return Objects.equals(cvParams, cvAlertData.cvParams);
  }

  @Override
  public String buildTitle() {
    if (StringUtils.isEmpty(message)) {
      log.error("message field is empty, can't build title for alert. "
          + "If you think this alerts needs no message, then just remove this log");
    }
    return message;
  }
}
