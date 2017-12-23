package software.wings.scheduler;

import com.google.inject.Singleton;

import org.mongodb.morphia.query.Query;
import org.reflections.Reflections;
import software.wings.beans.Base;
import software.wings.dl.WingsPersistence;
import software.wings.security.annotations.Archive;

import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by rsingh on 7/13/17.
 */
@Singleton
public class ArchivalManager {
  private final WingsPersistence wingsPersistence;
  private final Set<Class<?>> archivedClasses;

  public ArchivalManager(WingsPersistence wingsPersistence) {
    this.wingsPersistence = wingsPersistence;

    final Reflections reflections = new Reflections("software.wings");
    this.archivedClasses = reflections.getTypesAnnotatedWith(Archive.class);
  }

  public void startArchival() {
    final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
    scheduledExecutorService.scheduleAtFixedRate(() -> {
      for (Class<?> clazz : archivedClasses) {
        if (isExtendedFromBase(clazz)) {
          continue;
        }

        Archive annotation = clazz.getAnnotation(Archive.class);
        if (annotation.retentionMills() <= 0) {
          continue;
        }
        Query deletionQuery = wingsPersistence.createQuery(clazz)
                                  .field("createdAt")
                                  .lessThan(System.currentTimeMillis() - annotation.retentionMills());
        this.wingsPersistence.delete(deletionQuery);
      }
    }, 1, 1, TimeUnit.MINUTES);
  }

  private boolean isExtendedFromBase(Class<?> clazz) {
    while (clazz.getSuperclass() != null && clazz.getSuperclass() != Base.class) {
      clazz = clazz.getSuperclass();
    }

    return clazz == Base.class;
  }
}
