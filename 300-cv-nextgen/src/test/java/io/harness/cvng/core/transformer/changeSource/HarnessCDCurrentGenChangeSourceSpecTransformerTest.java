package io.harness.cvng.core.transformer.changeSource;

import static io.harness.rule.OwnerRule.SOWMYA;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CvNextGenTestBase;
import io.harness.category.element.UnitTests;
import io.harness.cvng.BuilderFactory;
import io.harness.cvng.core.beans.monitoredService.ChangeSourceDTO;
import io.harness.cvng.core.beans.monitoredService.changeSourceSpec.HarnessCDCurrentGenChangeSourceSpec;
import io.harness.cvng.core.entities.changeSource.ChangeSource;
import io.harness.cvng.core.entities.changeSource.HarnessCDCurrentGenChangeSource;
import io.harness.rule.Owner;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class HarnessCDCurrentGenChangeSourceSpecTransformerTest extends CvNextGenTestBase {
  private HarnessCDCurrentGenChangeSourceSpecTransformer harnessCDCurrentGenChangeSourceSpecTransformer;
  private BuilderFactory builderFactory;

  @Before
  public void setup() {
    harnessCDCurrentGenChangeSourceSpecTransformer = new HarnessCDCurrentGenChangeSourceSpecTransformer();
    builderFactory = BuilderFactory.getDefault();
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void test_getEntity() {
    ChangeSourceDTO changeSourceDTO = builderFactory.getHarnessCDCurrentGenChangeSourceDTOBuilder().build();
    ChangeSource harnessCDCurrentGenChangeSource = harnessCDCurrentGenChangeSourceSpecTransformer.getEntity(
        builderFactory.getContext().getServiceEnvironmentParams(), changeSourceDTO);
    assertThat(harnessCDCurrentGenChangeSource.getClass()).isEqualTo(HarnessCDCurrentGenChangeSource.class);
    assertThat(harnessCDCurrentGenChangeSource.getIdentifier()).isEqualTo(changeSourceDTO.getIdentifier());
    assertThat(harnessCDCurrentGenChangeSource.getAccountId()).isEqualTo(builderFactory.getContext().getAccountId());
    assertThat(harnessCDCurrentGenChangeSource.getProjectIdentifier())
        .isEqualTo(builderFactory.getContext().getProjectIdentifier());
    assertThat(harnessCDCurrentGenChangeSource.getServiceIdentifier())
        .isEqualTo(builderFactory.getContext().getServiceIdentifier());
    assertThat(harnessCDCurrentGenChangeSource.getEnvIdentifier())
        .isEqualTo(builderFactory.getContext().getEnvIdentifier());
    assertThat(harnessCDCurrentGenChangeSource.isEnabled()).isTrue();
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void test_getSpec() {
    HarnessCDCurrentGenChangeSource changeSource = builderFactory.getHarnessCDCurrentGenChangeSourceBuilder().build();
    HarnessCDCurrentGenChangeSourceSpec changeSourceSpec =
        harnessCDCurrentGenChangeSourceSpecTransformer.getSpec(changeSource);
    assertThat(changeSourceSpec.getHarnessApplicationId()).isEqualTo(changeSource.getHarnessApplicationId());
    assertThat(changeSourceSpec.getHarnessServiceId()).isEqualTo(changeSource.getHarnessServiceId());
    assertThat(changeSourceSpec.getHarnessEnvironmentId()).isEqualTo(changeSource.getHarnessEnvironmentId());
  }
}
