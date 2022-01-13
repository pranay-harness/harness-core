/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.utils;

import static io.harness.rule.OwnerRule.AVMOHAN;
import static io.harness.rule.OwnerRule.GEORGE;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import java.time.Duration;
import org.joor.Reflect;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class ProcessControlTest extends CategoryTest {
  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void shouldGetMyPId() {
    assertThat(ProcessControl.myProcessId()).isNotEqualTo(-1);
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void shouldKillProcess() throws Exception {
    Process process = new ProcessBuilder().command("cat").start();
    assertThat(process.isAlive()).isTrue();
    String pid = Reflect.on(process).get("pid").toString();
    ProcessControl.ensureKilled(pid, Duration.ofSeconds(5));
    assertThat(process.isAlive()).isFalse();
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void shouldNotThrowForNullPid() throws Exception {
    assertThatCode(() -> ProcessControl.ensureKilled(null, Duration.ofSeconds(5))).doesNotThrowAnyException();
  }
}
