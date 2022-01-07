/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.generator;

import io.harness.exception.DuplicateFieldException;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;

import com.mongodb.DuplicateKeyException;

public class GeneratorUtils {
  public interface PersistenceIn<T> {
    T save();
  }
  public interface PersistenceOut<T> {
    T obtain();
  }

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
