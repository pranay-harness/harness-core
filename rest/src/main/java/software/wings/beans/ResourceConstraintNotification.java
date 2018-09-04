package software.wings.beans;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@EqualsAndHashCode(callSuper = true)
public class ResourceConstraintNotification extends Notification {
  @Getter @Setter private String displayText;

  public ResourceConstraintNotification() {
    super(NotificationType.INFORMATION);
  }
}
