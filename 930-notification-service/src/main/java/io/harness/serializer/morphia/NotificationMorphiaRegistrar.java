package io.harness.serializer.morphia;

import io.harness.morphia.MorphiaRegistrar;
import io.harness.morphia.MorphiaRegistrarHelperPut;
import io.harness.notification.entities.Notification;
import io.harness.notification.entities.NotificationSetting;
import io.harness.notification.entities.NotificationTemplate;

import java.util.Set;

public class NotificationMorphiaRegistrar implements MorphiaRegistrar {
  @Override
  public void registerClasses(Set<Class> set) {
    set.add(Notification.class);
    set.add(NotificationTemplate.class);
    set.add(NotificationSetting.class);
  }

  @Override
  public void registerImplementationClasses(MorphiaRegistrarHelperPut h, MorphiaRegistrarHelperPut w) {}
}
