package software.wings.api.instancedetails;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.WingsBaseTest;

import java.util.ArrayList;

public class InstanceInfoVariablesTest extends WingsBaseTest {
  @Test
  @Owner(developers = OwnerRule.YOGESH)
  @Category(UnitTests.class)
  public void defaultValueOfTrafficShift() {
    assertThat(InstanceInfoVariables.builder().build().getNewInstanceTrafficPercent()).isNull();
  }
  @Test
  @Owner(developers = OwnerRule.YOGESH)
  @Category(UnitTests.class)
  public void isDeployStateInfo() {
    assertThat(InstanceInfoVariables.builder()
                   .instanceDetails(new ArrayList<>())
                   .instanceElements(new ArrayList<>())
                   .build()
                   .isDeployStateInfo())
        .isTrue();
    assertThat(InstanceInfoVariables.builder()
                   .instanceDetails(new ArrayList<>())
                   .newInstanceTrafficPercent(5)
                   .build()
                   .isDeployStateInfo())
        .isFalse();
  }
}