package io.harness.pms.barriers.service;

import static io.harness.rule.OwnerRule.ALEXEI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.harness.PipelineServiceTestBase;
import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.pms.barriers.beans.BarrierSetupInfo;
import io.harness.rule.Owner;

import com.google.common.io.Resources;
import com.google.inject.Inject;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class PMSBarrierServiceTest extends PipelineServiceTestBase {
  @Inject private PMSBarrierServiceImpl pmsBarrierService;

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldTestGetBarrierSetupInfoList() throws IOException {
    ClassLoader classLoader = getClass().getClassLoader();
    String yamlFile = "barriers.yaml";
    String yaml = Resources.toString(Objects.requireNonNull(classLoader.getResource(yamlFile)), StandardCharsets.UTF_8);

    List<BarrierSetupInfo> barrierSetupInfoList = pmsBarrierService.getBarrierSetupInfoList(yaml);

    assertThat(barrierSetupInfoList.size()).isEqualTo(3);
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldThrowIOExceptionWhenGetBarrierSetupInfoList() {
    String incorrectYaml = "pipeline: stages: stage";
    assertThatThrownBy(() -> pmsBarrierService.getBarrierSetupInfoList(incorrectYaml))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("Error while extracting yaml");
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldThrowInvalidRequestExceptionWhenGetBarrierSetupInfoList() throws IOException {
    ClassLoader classLoader = getClass().getClassLoader();
    String yamlFile = "barriers-incorrect.yaml";
    String yaml = Resources.toString(Objects.requireNonNull(classLoader.getResource(yamlFile)), StandardCharsets.UTF_8);

    assertThatThrownBy(() -> pmsBarrierService.getBarrierSetupInfoList(yaml))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("Barrier Identifier myBarrierId7 was not present in flowControl");
  }
}
