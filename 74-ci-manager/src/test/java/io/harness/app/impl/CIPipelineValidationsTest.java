package io.harness.app.impl;

import static io.harness.rule.OwnerRule.ALEKSANDAR;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.beans.ParameterField;
import io.harness.category.element.UnitTests;
import io.harness.cdng.pipeline.CDPipeline;
import io.harness.cdng.pipeline.beans.entities.CDPipelineEntity;
import io.harness.rule.Owner;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class CIPipelineValidationsTest extends CIManagerTest {
  @Inject CIPipelineValidations ciPipelineValidations;

  CDPipeline cdPipeline = CDPipeline.builder().description(ParameterField.createValueField("testDescription")).build();
  CDPipelineEntity pipeline =
      CDPipelineEntity.builder().identifier("testIdentifier").uuid("testUUID").cdPipeline(cdPipeline).build();

  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  @Ignore("TODO:: Need to implement validation")
  public void validateCIPipeline() {
    ciPipelineValidations.validateCIPipeline(pipeline);
    assertThat(ciPipelineValidations).isNotNull();
  }
}