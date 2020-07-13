package io.harness.service;

import static io.harness.rule.OwnerRule.GEORGE;
import static io.harness.threading.Morpheus.sleep;
import static java.time.Duration.ofMillis;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.DelegateServiceTest;
import io.harness.callback.DelegateCallback;
import io.harness.callback.MongoDatabase;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.DelegateCallbackRecord;
import io.harness.persistence.HPersistence;
import io.harness.rule.Owner;
import io.harness.service.intfc.DelegateCallbackRegistry;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class DelegateCallbackRegistryTest extends DelegateServiceTest {
  @Inject HPersistence persistence;
  @Inject DelegateCallbackRegistry delegateCallbackRegistry;

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void testEnsureCallback() {
    DelegateCallback delegateCallback1 =
        DelegateCallback.newBuilder()
            .setMongoDatabase(MongoDatabase.newBuilder().setConnection("dummy data1").build())
            .build();
    DelegateCallback delegateCallback2 =
        DelegateCallback.newBuilder()
            .setMongoDatabase(MongoDatabase.newBuilder().setConnection("dummy data2").build())
            .build();
    String registryId = delegateCallbackRegistry.ensureCallback(delegateCallback1);

    DelegateCallbackRecord delegateCallbackRecord1 = persistence.get(DelegateCallbackRecord.class, registryId);
    assertThat(delegateCallbackRecord1)
        .isNotNull()
        .extracting(DelegateCallbackRecord::getCallbackMetadata)
        .isEqualTo(delegateCallback1.toByteArray());

    sleep(ofMillis(1));
    assertThat(delegateCallbackRegistry.ensureCallback(delegateCallback1)).isEqualTo(registryId);

    DelegateCallbackRecord delegateCallbackRecord2 = persistence.get(DelegateCallbackRecord.class, registryId);
    assertThat(delegateCallbackRecord2)
        .isNotNull()
        .extracting(DelegateCallbackRecord::getCallbackMetadata)
        .isEqualTo(delegateCallback1.toByteArray());

    assertThat(delegateCallbackRecord2.getValidUntil()).isAfter(delegateCallbackRecord1.getValidUntil());

    assertThat(delegateCallbackRegistry.ensureCallback(delegateCallback2)).isNotEqualTo(registryId);
  }
}
