package software.wings.beans;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Reference;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by anubhaw on 4/13/17.
 */
@Entity(value = "notificationBatch", noClassnameStored = true)
@Data
@NoArgsConstructor
public class NotificationBatch extends Base {
  private String batchId;
  private NotificationRule notificationRule;
  @Reference(idOnly = true, ignoreMissing = true) private List<Notification> notifications = new ArrayList<>();
}
