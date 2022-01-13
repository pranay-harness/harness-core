/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.data.structure;

import static io.harness.data.structure.UUIDGenerator.convertFromBase64;
import static io.harness.data.structure.UUIDGenerator.convertToBase64String;
import static io.harness.data.structure.UUIDGenerator.generateTimeBasedUuid;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.GEORGE;

import static java.time.Duration.ofMillis;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import io.harness.threading.Morpheus;

import java.util.UUID;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class UUIDGeneratorTest extends CategoryTest {
  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void teatGenerateUuid() {
    assertThat(generateUuid()).hasSize(22);
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void teatGenerateTimebaseUuid() {
    String base64 = generateTimeBasedUuid();
    UUID uuid = convertFromBase64(base64);
    assertThatCode(() -> uuid.timestamp()).doesNotThrowAnyException();
    Morpheus.sleep(ofMillis(1));
    String base64_2 = generateTimeBasedUuid();
    UUID uuid_2 = convertFromBase64(base64_2);

    assertThat(uuid_2.timestamp()).isGreaterThan(uuid.timestamp());
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void teatConvertFromToBase64() {
    UUID uuid = new UUID(0x1234567890abcdefL, 0xfedcba0987654321L);
    UUID uuid2 = convertFromBase64(convertToBase64String(uuid));

    assertThat(uuid).isEqualTo(uuid2);
  }
}
