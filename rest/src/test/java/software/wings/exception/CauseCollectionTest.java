package software.wings.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class CauseCollectionTest {
  @Test
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

  @Test
  public void causeCollectionLimit() {
    CauseCollection collection = new CauseCollection().addCause(new Exception(new Exception()));

    for (int i = 0; i < 20; i++) {
      collection.addCause(new Exception(new Exception(new Exception(new Exception()))));
    }

    int count = 0;
    Throwable ex = collection.getCause();
    while (ex != null) {
      ex = ex.getCause();
      count++;
    }
    assertThat(count).isEqualTo(50);
  }

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
