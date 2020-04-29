package io.harness.mongo;

import com.mongodb.MongoCommandException;
import io.harness.mongo.MorphiaMove.MorphiaMoveKeys;
import io.harness.morphia.MorphiaRegistrar;
import io.harness.persistence.HPersistence;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.AdvancedDatastore;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;

import java.util.HashMap;
import java.util.Map;

@UtilityClass
@Slf4j
public class ClassRefactoringManager {
  public static Map<String, String> movements(Map<String, Class> morphiaInterfaceImplementers) {
    Map<String, String> movements = new HashMap<>();
    for (Map.Entry<String, Class> entry : morphiaInterfaceImplementers.entrySet()) {
      if (entry.getValue() == MorphiaRegistrar.NotFoundClass.class) {
        continue;
      }
      String target = entry.getValue().getName();
      if (entry.getKey().equals(target)) {
        continue;
      }

      movements.put(entry.getKey(), target);
    }
    return movements;
  }

  public static void updateMovedClasses(
      AdvancedDatastore primaryDatastore, Map<String, Class> morphiaInterfaceImplementers) {
    try {
      Map<String, String> movements = movements(morphiaInterfaceImplementers);
      movements.forEach((source, target) -> {
        Query<MorphiaMove> query =
            primaryDatastore.createQuery(MorphiaMove.class).filter(MorphiaMoveKeys.target, target);
        UpdateOperations<MorphiaMove> updateOperations =
            primaryDatastore.createUpdateOperations(MorphiaMove.class).addToSet(MorphiaMoveKeys.sources, source);
        primaryDatastore.findAndModify(query, updateOperations, HPersistence.upsertReturnNewOptions);
      });
    } catch (MongoCommandException exception) {
      if (exception.getErrorCode() == 13) {
        logger.warn("The user has read only access.");
        return;
      }
      throw exception;
    }
  }
}
