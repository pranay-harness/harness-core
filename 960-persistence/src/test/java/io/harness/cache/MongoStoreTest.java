/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cache;

import static io.harness.rule.OwnerRule.GEORGE;

import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.PersistenceTestBase;
import io.harness.cache.MongoStoreTestBase.TestNominalEntity;
import io.harness.cache.MongoStoreTestBase.TestOrdinalEntity;
import io.harness.category.element.UnitTests;
import io.harness.persistence.HPersistence;
import io.harness.rule.Owner;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.turbo.TurboFilter;
import ch.qos.logback.core.spi.FilterReply;
import com.google.inject.Inject;
import de.bwaldvogel.mongo.wire.MongoWireProtocolHandler;
import java.util.Collections;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;

public class MongoStoreTest extends PersistenceTestBase {
  @Inject HPersistence hPersistence;
  @Inject MongoStore mongoStore;

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void testNominalUpdateGet() {
    TestNominalEntity foo = mongoStore.<TestNominalEntity>get(
        0, TestNominalEntity.algorithmId, TestNominalEntity.STRUCTURE_HASH, "key", Collections.emptyList());
    assertThat(foo).isNull();

    TestNominalEntity bar = TestNominalEntity.builder().contextHash(0).key("key").value("value").build();
    mongoStore.upsert(bar, ofSeconds(10));

    foo = mongoStore.<TestNominalEntity>get(
        0, TestNominalEntity.algorithmId, TestNominalEntity.STRUCTURE_HASH, "key", Collections.emptyList());
    assertThat(foo).isNotNull();
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
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

    TestOrdinalEntity foo = mongoStore.<TestOrdinalEntity>get(
        0, TestOrdinalEntity.algorithmId, TestOrdinalEntity.STRUCTURE_HASH, "key", Collections.emptyList());
    assertThat(foo).isNull();

    TestOrdinalEntity bar = TestOrdinalEntity.builder().contextOrder(0).key("key").value("value").build();
    mongoStore.upsert(bar, ofSeconds(10));

    foo = mongoStore.<TestOrdinalEntity>get(
        0, TestOrdinalEntity.algorithmId, TestOrdinalEntity.STRUCTURE_HASH, "key", Collections.emptyList());
    assertThat(foo).isNotNull();

    TestOrdinalEntity baz = TestOrdinalEntity.builder().contextOrder(1).key("key").value("value").build();
    mongoStore.upsert(baz, ofSeconds(10));

    foo = mongoStore.<TestOrdinalEntity>get(
        TestOrdinalEntity.algorithmId, TestOrdinalEntity.STRUCTURE_HASH, "key", Collections.emptyList());
    assertThat(foo).isNotNull();
    assertThat(foo.contextOrder).isEqualTo(1);

    mongoStore.upsert(bar, ofSeconds(10));

    foo = mongoStore.<TestOrdinalEntity>get(
        TestOrdinalEntity.algorithmId, TestOrdinalEntity.STRUCTURE_HASH, "key", Collections.emptyList());
    assertThat(foo).isNotNull();
    assertThat(foo.contextOrder).isEqualTo(1);
  }
}
