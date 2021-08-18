package io.harness.cvng.core.transformer.changeSource;

import static io.harness.rule.OwnerRule.ABHIJITH;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.cvng.BuilderFactory;
import io.harness.cvng.core.beans.EnvironmentParams;
import io.harness.cvng.core.beans.ProjectParams;
import io.harness.cvng.core.beans.monitoredService.ChangeSourceDTO;
import io.harness.cvng.core.beans.monitoredService.changeSourceSpec.HarnessCDNGChangeSourceSpec;
import io.harness.cvng.core.entities.changeSource.ChangeSource;
import io.harness.cvng.core.entities.changeSource.HarnessCDNGChangeSource;
import io.harness.rule.Owner;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class HarnessHarnessHarnessCDNGChangeSourceSpecTransformerTest {
  @Inject @Named("CDNG_DEPLOYMENT") ChangeSourceSpecTransformer changeSourceSpecTransformer;

  ProjectParams projectParams;
  EnvironmentParams environmentParams;
  BuilderFactory builderFactory;

  @Before
  public void setup() {
    builderFactory = BuilderFactory.getDefault();
    projectParams = builderFactory.getContext().getProjectParams();
    environmentParams = EnvironmentParams.builder()
                            .projectParams(projectParams)
                            .serviceIdentifier(builderFactory.getContext().getServiceIdentifier())
                            .envIdentifier(builderFactory.getContext().getEnvIdentifier())
                            .build();
  }

  @Test
  @Owner(developers = ABHIJITH)
  @Category(UnitTests.class)
  public void test_getEntity() {
    ChangeSourceDTO changeSourceDTO = builderFactory.getHarnessCDNGChangeSourceDTOBuilder().build();
    ChangeSource cdngChangeSource = changeSourceSpecTransformer.getEntity(environmentParams, changeSourceDTO);
    assertThat(cdngChangeSource.getClass()).isEqualTo(HarnessCDNGChangeSource.class);
    assertThat(cdngChangeSource.getDescription()).isEqualTo(changeSourceDTO.getDescription());
    assertThat(cdngChangeSource.getIdentifier()).isEqualTo(changeSourceDTO.getIdentifier());
    assertThat(cdngChangeSource.getAccountId()).isEqualTo(projectParams.getAccountIdentifier());
    assertThat(cdngChangeSource.getProjectIdentifier()).isEqualTo(projectParams.getProjectIdentifier());
    assertThat(cdngChangeSource.getServiceIdentifier()).isEqualTo(environmentParams.getServiceIdentifier());
    assertThat(cdngChangeSource.getEnvIdentifier()).isEqualTo(environmentParams.getEnvIdentifier());
    assertThat(cdngChangeSource.isEnabled()).isTrue();
  }

  @Test
  @Owner(developers = ABHIJITH)
  @Category(UnitTests.class)
  public void test_getSpec() {
    assertThat(changeSourceSpecTransformer.getSpec(HarnessCDNGChangeSource.builder().build()))
        .isEqualTo(new HarnessCDNGChangeSourceSpec());
  }
}