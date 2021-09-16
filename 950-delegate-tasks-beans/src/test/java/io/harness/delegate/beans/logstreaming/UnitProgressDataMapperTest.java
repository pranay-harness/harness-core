package io.harness.delegate.beans.logstreaming;

import static io.harness.rule.OwnerRule.ACASIAN;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.logging.CommandExecutionStatus;
import io.harness.logging.UnitProgress;
import io.harness.logging.UnitStatus;
import io.harness.rule.Owner;

import java.time.Instant;
import java.util.Arrays;
import java.util.Map;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(HarnessTeam.CDP)
public class UnitProgressDataMapperTest extends CategoryTest {
  @Test
  @Owner(developers = ACASIAN)
  @Category(UnitTests.class)
  public void testShouldMapToCommandUnitProgress() {
    long startTime = Instant.now().toEpochMilli();
    long endTime = Instant.now().plusSeconds(1).toEpochMilli();
    UnitProgress running = UnitProgress.newBuilder()
                               .setUnitName("Test")
                               .setStatus(UnitStatus.RUNNING)
                               .setStartTime(startTime)
                               .setEndTime(endTime)
                               .build();
    UnitProgress success = UnitProgress.newBuilder()
                               .setUnitName("Foo")
                               .setStatus(UnitStatus.SUCCESS)
                               .setStartTime(startTime)
                               .setEndTime(endTime)
                               .build();
    UnitProgressData unitProgressData =
        UnitProgressData.builder().unitProgresses(Arrays.asList(running, success)).build();

    CommandUnitsProgress commandUnitsProgress = UnitProgressDataMapper.toCommandUnitsProgress(unitProgressData);
    assertThat(commandUnitsProgress).isNotNull();
    assertThat(commandUnitsProgress.getCommandUnitProgressMap()).isNotEmpty();
    assertThat(commandUnitsProgress.getCommandUnitProgressMap().size()).isEqualTo(2);
    Map<String, CommandUnitProgress> entries = commandUnitsProgress.getCommandUnitProgressMap();
    CommandUnitProgress testCommandUnitProgress = entries.get("Test");
    assertThat(testCommandUnitProgress).isNotNull();
    assertThat(testCommandUnitProgress.getStatus()).isEqualTo(CommandExecutionStatus.RUNNING);
    assertThat(testCommandUnitProgress.getStartTime()).isEqualTo(startTime);
    assertThat(testCommandUnitProgress.getEndTime()).isEqualTo(endTime);

    CommandUnitProgress fooCommandUnitProgress = entries.get("Foo");
    assertThat(fooCommandUnitProgress).isNotNull();
    assertThat(fooCommandUnitProgress.getStatus()).isEqualTo(CommandExecutionStatus.SUCCESS);
    assertThat(fooCommandUnitProgress.getStartTime()).isEqualTo(startTime);
    assertThat(fooCommandUnitProgress.getEndTime()).isEqualTo(endTime);
  }

  @Test
  @Owner(developers = ACASIAN)
  @Category(UnitTests.class)
  public void testShouldNotMapIfUnitProgressIsNull() {
    assertThat(UnitProgressDataMapper.toCommandUnitsProgress(null)).isNull();
    assertThat(UnitProgressDataMapper.toCommandUnitsProgress(UnitProgressData.builder().build())).isNull();
  }
}
