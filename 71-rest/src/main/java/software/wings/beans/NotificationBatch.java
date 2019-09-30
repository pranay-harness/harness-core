package software.wings.beans;

import io.harness.annotation.HarnessEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Reference;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by anubhaw on 4/13/17.
 */
@Entity(value = "notificationBatch", noClassnameStored = true)
@HarnessEntity(exportable = true)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class NotificationBatch extends Base {
  private String batchId;
  private NotificationRule notificationRule;
  @Reference(idOnly = true, ignoreMissing = true) private List<Notification> notifications = new ArrayList<>();
}
