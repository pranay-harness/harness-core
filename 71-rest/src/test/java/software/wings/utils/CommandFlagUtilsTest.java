package software.wings.utils;

import static io.harness.k8s.model.HelmVersion.V2;
import static io.harness.k8s.model.HelmVersion.V3;
import static io.harness.rule.OwnerRule.ARVIND;

import static software.wings.beans.HelmCommandFlagConstants.HelmSubCommand.FETCH;
import static software.wings.beans.HelmCommandFlagConstants.HelmSubCommand.PULL;
import static software.wings.beans.HelmCommandFlagConstants.HelmSubCommand.TEMPLATE;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import software.wings.helpers.ext.helm.HelmCommandTemplateFactory.HelmCliCommandType;

import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

public class CommandFlagUtilsTest extends CategoryTest {
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

  @Test
  @Owner(developers = ARVIND)
  @Category(UnitTests.class)
  public void testGetHelmSubCommand() {
    assertThat(CommandFlagUtils.getHelmSubCommand(V3, HelmCliCommandType.RENDER_CHART.toString())).isEqualTo(TEMPLATE);
    assertThat(CommandFlagUtils.getHelmSubCommand(V2, HelmCliCommandType.RENDER_CHART.toString())).isEqualTo(TEMPLATE);
    assertThat(CommandFlagUtils.getHelmSubCommand(V3, HelmCliCommandType.FETCH.toString())).isEqualTo(PULL);
    assertThat(CommandFlagUtils.getHelmSubCommand(V2, HelmCliCommandType.FETCH.toString())).isEqualTo(FETCH);
    assertThat(CommandFlagUtils.getHelmSubCommand(V2, HelmCliCommandType.INIT.toString())).isNull();
  }
}