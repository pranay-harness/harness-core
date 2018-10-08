package software.wings.integration.common;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.rule.BypassRuleMixin.Bypass;
import io.harness.threading.Concurrent;
import lombok.Getter;
import lombok.Setter;
import org.junit.Test;
import org.mongodb.morphia.annotations.Entity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.WingsBaseTest;
import software.wings.beans.Base;
import software.wings.dl.WingsPersistence;
import software.wings.rules.Integration;

@Entity(value = "!!!testMongo", noClassnameStored = true)
class MongoEntity extends Base {
  @Getter @Setter private String data;
}

@Integration
public class MongoDBTest extends WingsBaseTest {
  private static final Logger logger = LoggerFactory.getLogger(MongoDBTest.class);

  @Inject WingsPersistence wingsPersistence;

  @Test
  // this test was used to validate mongo setting for dirty reads
  // there is no need it to be run again and again
  @Bypass
  public void dirtyReads() {
    Concurrent.test(10, t -> {
      try {
        final MongoEntity entity = new MongoEntity();
        String data = "";
        for (int i = 0; i < 1000; i++) {
          logger.info("" + i);
          data = data + i;
          entity.setData(data);
          wingsPersistence.save(entity);
          final MongoEntity mongoEntity = wingsPersistence.get(MongoEntity.class, entity.getUuid());
          assertThat(mongoEntity.getData()).isEqualTo(data);
        }
      } catch (RuntimeException e) {
        logger.error("", e);
      }
    });
  }
}
