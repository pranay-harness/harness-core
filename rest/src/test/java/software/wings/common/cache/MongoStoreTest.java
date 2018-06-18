package software.wings.common.cache;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.cache.Distributable;
import lombok.Builder;
import lombok.Value;
import org.junit.Test;
import software.wings.WingsBaseTest;
import software.wings.dl.WingsPersistence;

import java.io.ObjectStreamClass;

public class MongoStoreTest extends WingsBaseTest {
  @Inject WingsPersistence wingsPersistence;

  @Value
  @Builder
  static class TestEntity implements Distributable {
    public static final long structureHash = ObjectStreamClass.lookup(TestEntity.class).getSerialVersionUID();
    public static final long algorithmId = 0;

    private long contextHash;

    private String key;
    private String value;

    @Override
    public long structureHash() {
      return structureHash;
    }

    @Override
    public long algorithmId() {
      return algorithmId;
    }

    @Override
    public long contextHash() {
      return contextHash;
    }

    @Override
    public String key() {
      return key;
    }
  }

  @Test
  public void testUpdateGet() {
    final MongoStore mongoStore = new MongoStore(wingsPersistence.getDatastore());

    TestEntity foo = mongoStore.<TestEntity>get(0, TestEntity.algorithmId, TestEntity.structureHash, "key");
    assertThat(foo).isNull();

    TestEntity bar = TestEntity.builder().contextHash(0).key("key").value("value").build();
    mongoStore.upsert(bar);

    foo = mongoStore.<TestEntity>get(0, TestEntity.algorithmId, TestEntity.structureHash, "key");
    assertThat(foo).isNotNull();
  }
}
