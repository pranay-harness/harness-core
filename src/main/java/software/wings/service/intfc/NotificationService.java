package software.wings.service.intfc;

import org.hibernate.validator.constraints.NotEmpty;
import ru.vyarus.guice.validator.group.annotation.ValidationGroups;
import software.wings.beans.Notification;
import software.wings.beans.NotificationAction;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.utils.validation.Create;
import software.wings.utils.validation.Update;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

/**
 * Created by anubhaw on 7/22/16.
 */
public interface NotificationService {
  PageResponse<Notification> list(PageRequest<Notification> pageRequest);

  @ValidationGroups(Create.class) Notification save(Notification notification);

  Notification get(@NotEmpty String appId, @NotEmpty String notificationId);

  @ValidationGroups(Update.class) Notification update(@Valid Notification notification);

  Notification act(
      @NotEmpty String appId, @NotEmpty String notificationId, @NotNull NotificationAction notificationAction);
}
