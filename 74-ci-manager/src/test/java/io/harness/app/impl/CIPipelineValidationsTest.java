package io.harness.app.impl;

import static io.harness.rule.OwnerRule.ALEKSANDAR;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.beans.ParameterField;
import io.harness.category.element.UnitTests;
import io.harness.cdng.pipeline.NgPipeline;
import io.harness.cdng.pipeline.beans.entities.NgPipelineEntity;
import io.harness.rule.Owner;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class CIPipelineValidationsTest extends CIManagerTest {
  @Inject CIPipelineValidations ciPipelineValidations;

  NgPipeline ngPipeline = NgPipeline.builder().description(ParameterField.createValueField("testDescription")).build();
  NgPipelineEntity pipeline =
      NgPipelineEntity.builder().identifier("testIdentifier").uuid("testUUID").ngPipeline(ngPipeline).build();

  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  @Ignore("TODO:: Need to implement validation")
  public void validateCIPipeline() {
    ciPipelineValidations.validateCIPipeline(pipeline);
    assertThat(ciPipelineValidations).isNotNull();
  }
}