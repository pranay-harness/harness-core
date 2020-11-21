package io.harness.cdng;

import static io.harness.rule.OwnerRule.SAHIL;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.ngpipeline.pipeline.beans.yaml.NgPipeline;
import io.harness.rule.Owner;
import io.harness.walktree.visitor.validation.ValidationVisitor;
import io.harness.walktree.visitor.validation.modes.ModeType;
import io.harness.yaml.core.StageElement;
import io.harness.yaml.utils.YamlPipelineUtils;

import com.google.inject.Inject;
import com.google.inject.Injector;
import java.io.IOException;
import java.net.URL;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class NgPipelineValidationTest extends CDNGBaseTest {
  @Inject Injector injector;

  @Before
  public void setup() {}

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  @Ignore("Will be implemented with Parameter Field")
  public void testCDPipeline() throws IOException {
    ClassLoader classLoader = this.getClass().getClassLoader();
    final URL testFile = classLoader.getResource("cdng/validationpipeline.yaml");
    NgPipeline ngPipeline = YamlPipelineUtils.read(testFile, NgPipeline.class);
    ValidationVisitor validationVisitor = new ValidationVisitor(injector, ModeType.PRE_INPUT_SET, true);

    validationVisitor.walkElementTree(ngPipeline);

    assertThat(validationVisitor.getCurrentObject()).isInstanceOf(NgPipeline.class);
    NgPipeline validationResponse = (NgPipeline) validationVisitor.getCurrentObject();
    assertThat(validationResponse.getIdentifier()).isEqualTo("pipeline.identifier");
    assertThat(validationResponse.getStages().size()).isEqualTo(1);
    assertThat(((StageElement) validationResponse.getStages().get(0)).getIdentifier())
        .isEqualTo("pipeline.stage.identifier");
  }
}
