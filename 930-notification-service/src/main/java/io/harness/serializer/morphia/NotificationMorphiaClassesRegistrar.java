package io.harness.serializer.morphia;

import io.harness.morphia.MorphiaRegistrar;
import io.harness.morphia.MorphiaRegistrarHelperPut;
import io.harness.ng.MongoNotificationRequest;
import io.harness.notification.entities.Notification;
import io.harness.notification.entities.NotificationTemplate;

import java.util.Set;

public class NotificationMorphiaClassesRegistrar implements MorphiaRegistrar {
  @Override
  public void registerClasses(Set<Class> set) {
    set.add(MongoNotificationRequest.class);
    set.add(Notification.class);
    set.add(NotificationTemplate.class);
  }

  @Override
  public void registerImplementationClasses(MorphiaRegistrarHelperPut h, MorphiaRegistrarHelperPut w) {}
}
