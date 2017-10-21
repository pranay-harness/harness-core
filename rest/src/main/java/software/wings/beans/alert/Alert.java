package software.wings.beans.alert;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Indexed;
import software.wings.alerts.AlertCategory;
import software.wings.alerts.AlertSeverity;
import software.wings.alerts.AlertStatus;
import software.wings.alerts.AlertType;
import software.wings.beans.Base;

/**
 * Created by brett on 10/18/17
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity(value = "alerts")
public class Alert extends Base {
  @Indexed private String accountId;
  @Indexed private AlertType type;
  @Indexed private AlertStatus status;
  private String title;
  private AlertCategory category;
  private AlertSeverity severity;
  private AlertData alertData;
  private long closedAt;

  public static final class AlertBuilder {
    private Alert alert;

    private AlertBuilder() {
      alert = new Alert();
    }

    public static AlertBuilder anAlert() {
      return new AlertBuilder();
    }

    public AlertBuilder withAccountId(String accountId) {
      alert.setAccountId(accountId);
      return this;
    }

    public AlertBuilder withType(AlertType type) {
      alert.setType(type);
      return this;
    }

    public AlertBuilder withStatus(AlertStatus status) {
      alert.setStatus(status);
      return this;
    }

    public AlertBuilder withTitle(String title) {
      alert.setTitle(title);
      return this;
    }

    public AlertBuilder withCategory(AlertCategory category) {
      alert.setCategory(category);
      return this;
    }

    public AlertBuilder withSeverity(AlertSeverity severity) {
      alert.setSeverity(severity);
      return this;
    }

    public AlertBuilder withAlertData(AlertData alertData) {
      alert.setAlertData(alertData);
      return this;
    }

    public AlertBuilder withClosedAt(long closedAt) {
      alert.setClosedAt(closedAt);
      return this;
    }

    public AlertBuilder withAppId(String appId) {
      alert.setAppId(appId);
      return this;
    }

    public AlertBuilder withCreatedAt(long createdAt) {
      alert.setCreatedAt(createdAt);
      return this;
    }

    public AlertBuilder but() {
      return anAlert()
          .withAccountId(alert.getAccountId())
          .withType(alert.getType())
          .withStatus(alert.getStatus())
          .withTitle(alert.getTitle())
          .withCategory(alert.getCategory())
          .withSeverity(alert.getSeverity())
          .withAlertData(alert.getAlertData())
          .withClosedAt(alert.getClosedAt())
          .withAppId(alert.getAppId())
          .withCreatedAt(alert.getCreatedAt());
    }

    public Alert build() {
      return alert;
    }
  }
}
