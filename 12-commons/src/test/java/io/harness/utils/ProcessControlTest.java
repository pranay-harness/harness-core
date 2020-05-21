package io.harness.utils;

import static io.harness.rule.OwnerRule.AVMOHAN;
import static io.harness.rule.OwnerRule.GEORGE;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.joor.Reflect;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.time.Duration;

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
}
