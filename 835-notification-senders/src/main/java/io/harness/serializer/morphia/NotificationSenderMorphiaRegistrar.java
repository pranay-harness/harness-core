package io.harness.serializer.morphia;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.morphia.MorphiaRegistrar;
import io.harness.morphia.MorphiaRegistrarHelperPut;
import io.harness.notification.entities.Notification;
import io.harness.notification.entities.NotificationSetting;
import io.harness.notification.entities.NotificationTemplate;
import io.harness.notifications.NotificationReceiverInfo;

import java.util.Set;

@OwnedBy(PL)
public class NotificationSenderMorphiaRegistrar implements MorphiaRegistrar {
  @Override
  public void registerClasses(Set<Class> set) {
    set.add(Notification.class);
    set.add(NotificationTemplate.class);
    set.add(NotificationSetting.class);
    set.add(NotificationReceiverInfo.class);
  }

  @Override
  public void registerImplementationClasses(MorphiaRegistrarHelperPut h, MorphiaRegistrarHelperPut w) {}
}
