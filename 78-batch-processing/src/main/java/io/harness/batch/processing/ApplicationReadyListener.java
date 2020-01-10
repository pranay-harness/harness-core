package io.harness.batch.processing;

import static com.google.common.base.Verify.verify;
import static io.harness.event.app.EventServiceApplication.EVENTS_STORE;
import static io.harness.persistence.HPersistence.DEFAULT_STORE;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toSet;

import io.harness.annotation.StoreIn;
import io.harness.mongo.HObjectFactory;
import io.harness.mongo.IndexManager;
import io.harness.persistence.HPersistence;
import io.harness.persistence.PersistentEntity;
import io.harness.timescaledb.TimeScaleDBService;
import org.mongodb.morphia.AdvancedDatastore;
import org.mongodb.morphia.Morphia;
import org.reflections.Reflections;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@Profile("!test")
public class ApplicationReadyListener {
  private final TimeScaleDBService timeScaleDBService;
  private final HPersistence hPersistence;

  public ApplicationReadyListener(TimeScaleDBService timeScaleDBService, HPersistence hPersistence) {
    this.timeScaleDBService = timeScaleDBService;
    this.hPersistence = hPersistence;
  }

  @EventListener(ApplicationReadyEvent.class)
  public void onApplicationReady() {
    ensureTimescaleConnectivity();
    ensureIndexForEventsStore();
  }

  private void ensureTimescaleConnectivity() {
    verify(timeScaleDBService.isValid(), "Unable to connect to timescale db");
  }

  private void ensureIndexForEventsStore() {
    Reflections reflections = new Reflections("software.wings", "io.harness");
    Set<Class> classes = reflections.getSubTypesOf(PersistentEntity.class)
                             .stream()
                             .filter(cls
                                 -> ofNullable(cls.getAnnotation(StoreIn.class))
                                        .map(StoreIn::value)
                                        .orElse(DEFAULT_STORE.getName())
                                        .equals(EVENTS_STORE.getName()))
                             .collect(toSet());
    Morphia locMorphia = new Morphia();
    locMorphia.getMapper().getOptions().setObjectFactory(new HObjectFactory());
    AdvancedDatastore datastore = hPersistence.getDatastore(EVENTS_STORE);
    locMorphia.map(classes);
    IndexManager.ensureIndex(datastore, locMorphia);
  }
}
