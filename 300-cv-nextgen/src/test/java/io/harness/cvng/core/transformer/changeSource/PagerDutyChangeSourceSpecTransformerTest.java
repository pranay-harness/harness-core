package io.harness.cvng.core.transformer.changeSource;

import static io.harness.rule.OwnerRule.SOWMYA;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CvNextGenTestBase;
import io.harness.category.element.UnitTests;
import io.harness.cvng.BuilderFactory;
import io.harness.cvng.core.beans.monitoredService.ChangeSourceDTO;
import io.harness.cvng.core.beans.monitoredService.changeSourceSpec.PagerDutyChangeSourceSpec;
import io.harness.cvng.core.entities.changeSource.ChangeSource;
import io.harness.cvng.core.entities.changeSource.PagerDutyChangeSource;
import io.harness.rule.Owner;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class PagerDutyChangeSourceSpecTransformerTest extends CvNextGenTestBase {
  private PagerDutyChangeSourceSpecTransformer pagerDutyChangeSourceSpecTransformer;
  private BuilderFactory builderFactory;

  @Before
  public void setup() {
    pagerDutyChangeSourceSpecTransformer = new PagerDutyChangeSourceSpecTransformer();
    builderFactory = BuilderFactory.getDefault();
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void test_getEntity() {
    ChangeSourceDTO changeSourceDTO = builderFactory.getPagerDutyChangeSourceDTOBuilder().build();
    ChangeSource pagerDutyChangeSource = pagerDutyChangeSourceSpecTransformer.getEntity(
        builderFactory.getContext().getServiceEnvironmentParams(), changeSourceDTO);
    assertThat(pagerDutyChangeSource.getClass()).isEqualTo(PagerDutyChangeSource.class);
    assertThat(pagerDutyChangeSource.getIdentifier()).isEqualTo(changeSourceDTO.getIdentifier());
    assertThat(pagerDutyChangeSource.getAccountId()).isEqualTo(builderFactory.getContext().getAccountId());
    assertThat(pagerDutyChangeSource.getProjectIdentifier())
        .isEqualTo(builderFactory.getContext().getProjectIdentifier());
    assertThat(pagerDutyChangeSource.getServiceIdentifier())
        .isEqualTo(builderFactory.getContext().getServiceIdentifier());
    assertThat(pagerDutyChangeSource.getEnvIdentifier()).isEqualTo(builderFactory.getContext().getEnvIdentifier());
    assertThat(pagerDutyChangeSource.isEnabled()).isTrue();
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void test_getSpec() {
    PagerDutyChangeSource changeSource = builderFactory.getPagerDutyChangeSourceBuilder().build();
    PagerDutyChangeSourceSpec changeSourceSpec = pagerDutyChangeSourceSpecTransformer.getSpec(changeSource);
    assertThat(changeSourceSpec.getConnectorRef()).isEqualTo(changeSource.getConnectorIdentifier());
    assertThat(changeSourceSpec.getPagerDutyServiceId()).isEqualTo(changeSource.getPagerDutyServiceId());
  }
}
