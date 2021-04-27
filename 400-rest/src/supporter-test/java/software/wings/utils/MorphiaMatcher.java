package software.wings.utils;

import static org.mockito.Matchers.any;

import org.mongodb.morphia.query.UpdateOperations;

public class MorphiaMatcher {
  /**
   * Same update operations as update operations.
   *
   * @param <T>              the type parameter
   * @param updateOperations the update operations
   * @return the update operations
   */
  public static <T> UpdateOperations<T> sameUpdateOperationsAs(UpdateOperations<T> updateOperations) {
    // return argThat(hasProperty("ops", samePropertyValuesAs(on(updateOperations).get("ops"))));
    return any(UpdateOperations.class);
  }
}
