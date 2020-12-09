package io.harness.app.impl;

import static io.harness.rule.OwnerRule.ALEKSANDAR;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.ngpipeline.pipeline.beans.entities.NgPipelineEntity;
import io.harness.ngpipeline.pipeline.beans.yaml.NgPipeline;
import io.harness.pms.yaml.ParameterField;
import io.harness.rule.Owner;

import com.google.inject.Inject;
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
