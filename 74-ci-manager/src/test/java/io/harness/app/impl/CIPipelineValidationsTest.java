package io.harness.app.impl;

import static io.harness.rule.OwnerRule.ALEKSANDAR;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.beans.CIPipeline;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class CIPipelineValidationsTest extends CIManagerTest {
  @Inject CIPipelineValidations ciPipelineValidations;

  CIPipeline pipeline =
      CIPipeline.builder().identifier("testIdentifier").description("testDescription").uuid("testUUID").build();

  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  @Ignore("TODO:: Need to implement validation")
  public void validateCIPipeline() {
    ciPipelineValidations.validateCIPipeline(pipeline);
    assertThat(ciPipelineValidations).isNotNull();
  }
}