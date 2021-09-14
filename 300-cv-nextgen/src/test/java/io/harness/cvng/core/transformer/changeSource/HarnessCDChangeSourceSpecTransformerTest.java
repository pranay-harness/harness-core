/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.core.transformer.changeSource;

import static io.harness.rule.OwnerRule.ABHIJITH;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.cvng.BuilderFactory;
import io.harness.cvng.core.beans.monitoredService.ChangeSourceDTO;
import io.harness.cvng.core.beans.monitoredService.changeSourceSpec.HarnessCDChangeSourceSpec;
import io.harness.cvng.core.beans.params.ServiceEnvironmentParams;
import io.harness.cvng.core.entities.changeSource.ChangeSource;
import io.harness.cvng.core.entities.changeSource.HarnessCDChangeSource;
import io.harness.rule.Owner;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class HarnessCDChangeSourceSpecTransformerTest {
  ChangeSourceSpecTransformer changeSourceSpecTransformer;

  ServiceEnvironmentParams environmentParams;
  BuilderFactory builderFactory;

  @Before
  public void setup() {
    changeSourceSpecTransformer = new HarnessCDChangeSourceSpecTransformer();
    builderFactory = BuilderFactory.getDefault();
    environmentParams = ServiceEnvironmentParams.builder()
                            .accountIdentifier(builderFactory.getContext().getAccountId())
                            .orgIdentifier(builderFactory.getContext().getOrgIdentifier())
                            .projectIdentifier(builderFactory.getContext().getProjectIdentifier())
                            .serviceIdentifier(builderFactory.getContext().getServiceIdentifier())
                            .environmentIdentifier(builderFactory.getContext().getEnvIdentifier())
                            .build();
  }

  @Test
  @Owner(developers = ABHIJITH)
  @Category(UnitTests.class)
  public void test_getEntity() {
    ChangeSourceDTO changeSourceDTO = builderFactory.getHarnessCDChangeSourceDTOBuilder().build();
    ChangeSource cdngChangeSource = changeSourceSpecTransformer.getEntity(environmentParams, changeSourceDTO);
    assertThat(cdngChangeSource.getClass()).isEqualTo(HarnessCDChangeSource.class);
    assertThat(cdngChangeSource.getIdentifier()).isEqualTo(changeSourceDTO.getIdentifier());
    assertThat(cdngChangeSource.getAccountId()).isEqualTo(builderFactory.getContext().getAccountId());
    assertThat(cdngChangeSource.getProjectIdentifier()).isEqualTo(builderFactory.getContext().getProjectIdentifier());
    assertThat(cdngChangeSource.getServiceIdentifier()).isEqualTo(environmentParams.getServiceIdentifier());
    assertThat(cdngChangeSource.getEnvIdentifier()).isEqualTo(environmentParams.getEnvironmentIdentifier());
    assertThat(cdngChangeSource.isEnabled()).isTrue();
  }

  @Test
  @Owner(developers = ABHIJITH)
  @Category(UnitTests.class)
  public void test_getSpec() {
    assertThat(changeSourceSpecTransformer.getSpec(builderFactory.getHarnessCDChangeSourceBuilder().build()))
        .isEqualTo(new HarnessCDChangeSourceSpec());
  }
}
