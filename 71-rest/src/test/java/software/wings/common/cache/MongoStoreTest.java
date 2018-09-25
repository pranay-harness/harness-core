package software.wings.common.cache;

import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.turbo.TurboFilter;
import ch.qos.logback.core.spi.FilterReply;
import de.bwaldvogel.mongo.wire.MongoWireProtocolHandler;
import io.harness.cache.Distributable;
import io.harness.cache.Nominal;
import io.harness.cache.Ordinal;
import lombok.Builder;
import lombok.Value;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import software.wings.WingsBaseTest;
import software.wings.dl.WingsPersistence;

import java.io.ObjectStreamClass;

public class MongoStoreTest extends WingsBaseTest {
  @Inject WingsPersistence wingsPersistence;
  @Inject MongoStore mongoStore;

  @Value
  @Builder
  static class TestNominalEntity implements Distributable, Nominal {
    public static final long STRUCTURE_HASH = ObjectStreamClass.lookup(TestNominalEntity.class).getSerialVersionUID();
    public static final long algorithmId = 0;

    private long contextHash;

    private String key;
    private String value;

    @Override
    public long structureHash() {
      return STRUCTURE_HASH;
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

  @Value
  @Builder
  static class TestOrdinalEntity implements Distributable, Ordinal {
    public static final long STRUCTURE_HASH = ObjectStreamClass.lookup(TestOrdinalEntity.class).getSerialVersionUID();
    public static final long algorithmId = 0;

    private long contextOrder;

    private String key;
    private String value;

    @Override
    public long structureHash() {
      return STRUCTURE_HASH;
    }

    @Override
    public long algorithmId() {
      return algorithmId;
    }

    @Override
    public long contextOrder() {
      return contextOrder;
    }

    @Override
    public String key() {
      return key;
    }
  }

  @Test
  public void testNominalUpdateGet() {
    TestNominalEntity foo =
        mongoStore.<TestNominalEntity>get(0, TestNominalEntity.algorithmId, TestNominalEntity.STRUCTURE_HASH, "key");
    assertThat(foo).isNull();

    TestNominalEntity bar = TestNominalEntity.builder().contextHash(0).key("key").value("value").build();
    mongoStore.upsert(bar, ofSeconds(10));

    foo = mongoStore.<TestNominalEntity>get(0, TestNominalEntity.algorithmId, TestNominalEntity.STRUCTURE_HASH, "key");
    assertThat(foo).isNotNull();
  }

  @Test
  public void testOrdinalUpdateGet() {
    Logger log = LoggerFactory.getLogger(MongoWireProtocolHandler.class);
    final LoggerContext loggerContext = ((ch.qos.logback.classic.Logger) log).getLoggerContext();
    loggerContext.addTurboFilter(new TurboFilter() {
      @Override
      public FilterReply decide(Marker marker, ch.qos.logback.classic.Logger logger, Level level, String s,
          Object[] objects, Throwable throwable) {
        return FilterReply.DENY;
      }
    });

    TestOrdinalEntity foo =
        mongoStore.<TestOrdinalEntity>get(0, TestOrdinalEntity.algorithmId, TestOrdinalEntity.STRUCTURE_HASH, "key");
    assertThat(foo).isNull();

    TestOrdinalEntity bar = TestOrdinalEntity.builder().contextOrder(0).key("key").value("value").build();
    mongoStore.upsert(bar, ofSeconds(10));

    foo = mongoStore.<TestOrdinalEntity>get(0, TestOrdinalEntity.algorithmId, TestOrdinalEntity.STRUCTURE_HASH, "key");
    assertThat(foo).isNotNull();

    TestOrdinalEntity baz = TestOrdinalEntity.builder().contextOrder(1).key("key").value("value").build();
    mongoStore.upsert(baz, ofSeconds(10));

    foo = mongoStore.<TestOrdinalEntity>get(TestOrdinalEntity.algorithmId, TestOrdinalEntity.STRUCTURE_HASH, "key");
    assertThat(foo).isNotNull();
    assertThat(foo.contextOrder).isEqualTo(1);

    mongoStore.upsert(bar, ofSeconds(10));

    foo = mongoStore.<TestOrdinalEntity>get(TestOrdinalEntity.algorithmId, TestOrdinalEntity.STRUCTURE_HASH, "key");
    assertThat(foo).isNotNull();
    assertThat(foo.contextOrder).isEqualTo(1);
  }
}
