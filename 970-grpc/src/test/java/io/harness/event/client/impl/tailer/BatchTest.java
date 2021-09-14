/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.event.client.impl.tailer;

import static io.harness.rule.OwnerRule.AVMOHAN;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.data.structure.UUIDGenerator;
import io.harness.event.PublishMessage;
import io.harness.rule.Owner;

import org.junit.Test;
import org.junit.experimental.categories.Category;

public class BatchTest extends CategoryTest {
  // size=24 bytes
  public static final PublishMessage TEST_MESSAGE =
      PublishMessage.newBuilder().setMessageId(UUIDGenerator.generateUuid()).build();

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void shouldBeFullOnSizeThreshold() throws Exception {
    Batch batch = new Batch(10, 100);
    batch.add(TEST_MESSAGE);
    assertThat(batch.isFull()).isTrue();
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void shouldBeFullOnCountThreshold() throws Exception {
    Batch batch = new Batch(1024 * 1024, 1);
    batch.add(TEST_MESSAGE);
    assertThat(batch.isFull()).isTrue();
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void shouldThrowIfAddingToFullBatch() throws Exception {
    Batch batch = new Batch(1024 * 1024, 1);
    batch.add(TEST_MESSAGE);
    assertThat(batch.isFull()).isTrue();
    assertThatIllegalStateException().isThrownBy(() -> batch.add(TEST_MESSAGE));
  }
}
