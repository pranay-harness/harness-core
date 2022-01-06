/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.service;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.ALEKSANDAR;
import static io.harness.rule.OwnerRule.GEORGE;
import static io.harness.rule.OwnerRule.MARKO;
import static io.harness.threading.Morpheus.sleep;

import static java.time.Duration.ofMillis;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.harness.DelegateServiceTestBase;
import io.harness.callback.DelegateCallback;
import io.harness.callback.HttpsClientEntrypoint;
import io.harness.callback.MongoDatabase;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.DelegateCallbackRecord;
import io.harness.exception.UnexpectedException;
import io.harness.persistence.HPersistence;
import io.harness.rule.Owner;
import io.harness.service.impl.DelegateCallbackRegistryImpl;
import io.harness.service.impl.MongoDelegateCallbackService;

import com.google.inject.Inject;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class DelegateCallbackRegistryTest extends DelegateServiceTestBase {
  @Inject HPersistence persistence;
  @Inject DelegateCallbackRegistryImpl delegateCallbackRegistry;

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void testBuildDelegateCallbackServiceWithNoOrInvalidDriverId() {
    assertThat(delegateCallbackRegistry.buildDelegateCallbackService(null)).isNull();
    assertThat(delegateCallbackRegistry.buildDelegateCallbackService(generateUuid())).isNull();
  }
  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void testBuildDelegateTaskResultsProviderNoOrInvalidDriverId() {
    assertThat(delegateCallbackRegistry.buildDelegateTaskResultsProvider(null)).isNull();
    assertThat(delegateCallbackRegistry.buildDelegateTaskResultsProvider(generateUuid())).isNull();
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void testBuildDelegateCallbackServiceWithInvalidConnectionString() {
    DelegateCallback delegateCallback =
        DelegateCallback.newBuilder()
            .setMongoDatabase(MongoDatabase.newBuilder().setConnection("connectionString").build())
            .build();
    String driverId = delegateCallbackRegistry.ensureCallback(delegateCallback);

    assertThatThrownBy(() -> delegateCallbackRegistry.buildDelegateCallbackService(driverId))
        .isInstanceOf(UnexpectedException.class);
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void testBuildDelegateCallbackServiceWithValidConnectionString() {
    String database = generateUuid();

    DelegateCallback delegateCallback =
        DelegateCallback.newBuilder()
            .setMongoDatabase(
                MongoDatabase.newBuilder().setConnection("mongodb://username:password@host:27017/" + database).build())
            .build();
    String driverId = delegateCallbackRegistry.ensureCallback(delegateCallback);

    assertThat(delegateCallbackRegistry.buildDelegateCallbackService(driverId)).isNotNull();
    assertThat(delegateCallbackRegistry.buildDelegateCallbackService(driverId))
        .isInstanceOf(MongoDelegateCallbackService.class);
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void testBuildDelegateCallbackServiceWithTypeOtherThanMongo() {
    DelegateCallback delegateCallback =
        DelegateCallback.newBuilder().setHttpsClientEntrypoint(HttpsClientEntrypoint.newBuilder().build()).build();
    String driverId = delegateCallbackRegistry.ensureCallback(delegateCallback);

    assertThat(delegateCallbackRegistry.buildDelegateCallbackService(driverId)).isNull();
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void testObtainDelegateCallbackServiceWithNoOrInvalidDriverId() {
    assertThat(delegateCallbackRegistry.obtainDelegateCallbackService(null)).isNull();
    assertThat(delegateCallbackRegistry.obtainDelegateCallbackService(generateUuid())).isNull();
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void testObtainDelegateTaskResultsProviderWithNoOrInvalidDriverId() {
    assertThat(delegateCallbackRegistry.obtainDelegateTaskResultsProvider(null)).isNull();
    assertThat(delegateCallbackRegistry.obtainDelegateTaskResultsProvider(generateUuid())).isNull();
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void testObtainDelegateCallbackServiceWithExistingDriver() {
    String database = generateUuid();

    DelegateCallback delegateCallback =
        DelegateCallback.newBuilder()
            .setMongoDatabase(
                MongoDatabase.newBuilder().setConnection("mongodb://username:password@host:27017/" + database).build())
            .build();
    String driverId = delegateCallbackRegistry.ensureCallback(delegateCallback);

    assertThat(delegateCallbackRegistry.obtainDelegateCallbackService(driverId)).isNotNull();
    assertThat(delegateCallbackRegistry.obtainDelegateTaskResultsProvider(driverId)).isNotNull();

    assertThat(persistence.delete(DelegateCallbackRecord.class, driverId)).isTrue();
    assertThat(delegateCallbackRegistry.obtainDelegateCallbackService(driverId)).isNotNull();
    assertThat(delegateCallbackRegistry.obtainDelegateTaskResultsProvider(driverId)).isNotNull();
  }

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
