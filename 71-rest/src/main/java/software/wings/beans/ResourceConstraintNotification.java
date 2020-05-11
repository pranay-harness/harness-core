package software.wings.beans;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@OwnedBy(CDC)
@EqualsAndHashCode(callSuper = true)
public class ResourceConstraintNotification extends Notification {
  @Getter @Setter private String displayText;

  public ResourceConstraintNotification() {
    super(NotificationType.INFORMATION);
  }
}
