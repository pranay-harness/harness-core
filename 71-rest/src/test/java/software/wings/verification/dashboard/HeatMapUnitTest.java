package software.wings.verification.dashboard;

import static io.harness.rule.OwnerRule.SOWMYA;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.Lists;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.WingsBaseTest;

import java.util.ArrayList;
import java.util.Collections;

public class HeatMapUnitTest extends WingsBaseTest {
  private HeatMapUnit heatMapUnit;

  @Before
  public void setUp() {
    heatMapUnit = HeatMapUnit.builder().build();
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testUpdateOverallScore() {
    assertThat(heatMapUnit.getNa()).isEqualTo(1);

    heatMapUnit.updateOverallScore(-1.0);
    assertThat(heatMapUnit.getScoreList()).isEqualTo(new ArrayList<>());
    assertThat(heatMapUnit.getOverallScore()).isEqualTo(-1.0);
    assertThat(heatMapUnit.getNa()).isEqualTo(1);

    heatMapUnit.updateOverallScore(0.0);
    assertThat(heatMapUnit.getScoreList()).isEqualTo(Collections.singletonList(0.0));
    assertThat(heatMapUnit.getOverallScore()).isEqualTo(0.0);
    assertThat(heatMapUnit.getLowRisk()).isEqualTo(1);
    assertThat(heatMapUnit.getNa()).isEqualTo(0);

    heatMapUnit.updateOverallScore(-1.0);
    assertThat(heatMapUnit.getScoreList()).isEqualTo(Collections.singletonList(0.0));
    assertThat(heatMapUnit.getOverallScore()).isEqualTo(0.0);
    assertThat(heatMapUnit.getLowRisk()).isEqualTo(1);
    assertThat(heatMapUnit.getNa()).isEqualTo(0);

    heatMapUnit.updateOverallScore(1.0);
    assertThat(heatMapUnit.getScoreList()).isEqualTo(Lists.newArrayList(0.0, 1.0));
    assertThat(heatMapUnit.getOverallScore()).isEqualTo(0.5);
    assertThat(heatMapUnit.getMediumRisk()).isEqualTo(1);
    assertThat(heatMapUnit.getLowRisk()).isEqualTo(0);

    heatMapUnit.updateOverallScore(2.0);
    assertThat(heatMapUnit.getScoreList()).isEqualTo(Lists.newArrayList(0.0, 1.0, 2.0));
    assertThat(heatMapUnit.getOverallScore()).isEqualTo(1.0);
    assertThat(heatMapUnit.getHighRisk()).isEqualTo(1);
    assertThat(heatMapUnit.getMediumRisk()).isEqualTo(0);
  }
}