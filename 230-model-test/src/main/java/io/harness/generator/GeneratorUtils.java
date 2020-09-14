package io.harness.generator;

import com.mongodb.DuplicateKeyException;
import io.harness.exception.DuplicateFieldException;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;

public class GeneratorUtils {
  public interface PersistenceIn<T> { T save(); }
  public interface PersistenceOut<T> { T obtain(); }

  public static <T> T suppressDuplicateException(PersistenceIn<T> persistenceIn, PersistenceOut<T> persistenceOut) {
    try {
      return persistenceIn.save();
    } catch (WingsException | DuplicateKeyException exception) {
      if (exception instanceof DuplicateKeyException || exception.getCause() instanceof DuplicateKeyException) {
        T entity = persistenceOut.obtain();
        if (entity != null) {
          return entity;
        }
      }

      if (exception instanceof DuplicateFieldException || exception.getCause() instanceof DuplicateFieldException) {
        T entity = persistenceOut.obtain();
        if (entity != null) {
          return entity;
        }
      }

      if (exception instanceof InvalidRequestException || exception.getCause() instanceof InvalidRequestException) {
        T entity = persistenceOut.obtain();
        if (entity != null) {
          return entity;
        }
      }

      throw exception;
    }
  }
}
