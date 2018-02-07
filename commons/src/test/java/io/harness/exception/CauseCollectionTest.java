package io.harness.exception;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.FastUnitTests;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class CauseCollectionTest extends CategoryTest {
  @Test
  @Category(FastUnitTests.class)
  public void collectCauseCollection() {
    final CauseCollection collection = new CauseCollection()
                                           .addCause(new Exception("first"))
                                           .addCause(new Exception("second"))
                                           .addCause(new Exception("outer", new Exception("inner")));

    int count = 0;
    Throwable ex = collection.getCause();
    while (ex != null) {
      ex = ex.getCause();
      count++;
    }
    assertThat(count).isEqualTo(4);

    for (int i = 0; i < 10; i++) {
      collection.addCause(collection.getCause());
    }
  }

  @Category(FastUnitTests.class)
  @Test
  public void causeCollectionLimit() {
    CauseCollection collection = new CauseCollection().addCause(new Exception(new Exception()));

    for (int i = 0; i < 3; i++) {
      collection.addCause(new Exception(new Exception(new Exception(new Exception()))));
    }

    int count = 0;
    Throwable ex = collection.getCause();
    while (ex != null) {
      ex = ex.getCause();
      count++;
    }
    assertThat(count).isEqualTo(14);
  }

  @Category(FastUnitTests.class)
  @Test
  public void causeCollectionDeduplication() {
    final Exception exception = new Exception(new Exception(new Exception()));

    CauseCollection collection = new CauseCollection().addCause(exception);
    collection.addCause(exception);
    collection.addCause(exception.getCause());

    int count = 0;
    Throwable ex = collection.getCause();
    while (ex != null) {
      ex = ex.getCause();
      count++;
    }
    assertThat(count).isEqualTo(3);
  }
}
