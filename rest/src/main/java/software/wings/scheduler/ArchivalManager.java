package software.wings.scheduler;

import org.mongodb.morphia.query.Query;
import org.reflections.Reflections;
import software.wings.beans.Base;
import software.wings.dl.WingsPersistence;
import software.wings.security.annotations.Archive;

import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.inject.Singleton;

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
        if (clazz.getSuperclass() != Base.class) {
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
}
