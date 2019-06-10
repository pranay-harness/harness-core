package io.harness.migration;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import com.mongodb.DBCollection;
import io.harness.exception.InvalidArgumentsException;
import io.harness.mongo.IndexManager;
import io.harness.mongo.IndexManager.IndexCreator;
import org.apache.commons.lang3.tuple.Pair;
import org.mongodb.morphia.AdvancedDatastore;
import org.mongodb.morphia.Morphia;
import org.mongodb.morphia.mapping.MappedClass;

import java.util.Map;

@Singleton
public class MongoIndexMigrationService {
  @Inject Morphia morphia;
  @Inject @Named("primaryDatastore") private AdvancedDatastore primaryDatastore;

  public void ensureIndex(Class collectionClass, String indexName) {
    final DBCollection collection = primaryDatastore.getCollection(collectionClass);

    final MappedClass mappedClass = morphia.getMapper().getMappedClass(collectionClass);

    Map<String, IndexCreator> creators = IndexManager.indexCreators(mappedClass, collection);

    final IndexCreator indexCreator = creators.get(indexName);
    if (indexCreator == null) {
      throw new InvalidArgumentsException(Pair.of("Index", indexName));
    }
  }
}
