package io.harness.mongo;

import static io.harness.govern.IgnoreThrowable.ignoredOnPurpose;
import static io.harness.mongo.IndexManager.Mode.INSPECT;
import static io.harness.mongo.IndexManager.Mode.MANUAL;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import com.mongodb.DBCollection;
import io.harness.mongo.index.migrator.Migrator;
import io.harness.persistence.Store;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.AdvancedDatastore;
import org.mongodb.morphia.Morphia;
import org.mongodb.morphia.mapping.MappedClass;

import java.util.Map;
import javax.annotation.Nullable;

@Singleton
@Slf4j
public class IndexManager {
  public enum Mode { AUTO, MANUAL, INSPECT }

  @Inject Injector injector;
  @Nullable @Inject Map<String, Migrator> migrators;

  public void ensureIndexes(Mode mode, AdvancedDatastore datastore, Morphia morphia, Store store) {
    try {
      IndexManagerSession session = new IndexManagerSession(datastore, migrators, mode == null ? MANUAL : mode);
      if (session.ensureIndexes(morphia, store) && mode == INSPECT) {
        logger.info("the inspection finished");
        throw new IndexManagerInspectException();
      }
    } catch (IndexManagerReadOnlyException exception) {
      ignoredOnPurpose(exception);
      logger.warn("The user has read only access.");
    }
  }

  public static Map<String, IndexCreator> indexCreators(MappedClass mc, DBCollection collection) {
    return IndexManagerSession.indexCreators(mc, collection);
  }
}
