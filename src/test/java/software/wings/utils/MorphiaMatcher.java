package software.wings.utils;

import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.samePropertyValuesAs;
import static org.joor.Reflect.on;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;

import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;

/**
 * Created by peeyushaggarwal on 6/15/16.
 */
public class MorphiaMatcher {
  public static <T> Query<T> sameQueryAs(Query<T> query) {
    return argThat(hasProperty("children", samePropertyValuesAs(on(query).get("children"))));
  }

  public static <T> UpdateOperations<T> sameUpdateOperationsAs(UpdateOperations<T> updateOperations) {
    // return argThat(hasProperty("ops", samePropertyValuesAs(on(updateOperations).get("ops"))));
    return any(UpdateOperations.class);
  }
}
