/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.waiter;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.GEORGE;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.WaitEngineTestBase;
import io.harness.category.element.UnitTests;
import io.harness.queue.QueuePublisher;
import io.harness.rule.Owner;

import com.google.inject.Inject;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class NotifyQueuePublisherRegistryTest extends WaitEngineTestBase {
  @Inject private NotifyQueuePublisherRegister notifyQueuePublisherRegister;
  @Inject QueuePublisher<NotifyEvent> publisher;

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void testRegister() {
    String uuid = generateUuid();
    assertThat(notifyQueuePublisherRegister.obtain(uuid)).isNull();

    notifyQueuePublisherRegister.register(uuid, publisher::send);

    assertThat(notifyQueuePublisherRegister.obtain(uuid)).isNotNull();
  }
}
