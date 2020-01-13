package software.wings.beans.template;

import static io.harness.rule.OwnerRule.ROHIT_KUMAR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidArgumentsException;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.service.impl.template.TemplateBaseTestHelper;

import java.util.Arrays;
import java.util.List;

public class TemplateHelperTest extends TemplateBaseTestHelper {
  @Test
  @Owner(developers = ROHIT_KUMAR)
  @Category(UnitTests.class)
  public void test_mappedEntity() {
    final List<TemplateType> supportedTemplateTypes =
        Arrays.asList(TemplateType.HTTP, TemplateType.SHELL_SCRIPT, TemplateType.ARTIFACT_SOURCE, TemplateType.SSH);
    for (TemplateType supportedTemplateType : supportedTemplateTypes) {
      assertThat(TemplateHelper.mappedEntity(supportedTemplateType)).isNotNull();
    }

    Arrays.stream(TemplateType.values())
        .filter(templateType -> !supportedTemplateTypes.contains(templateType))
        .forEach(templateType
            -> assertThatExceptionOfType(InvalidArgumentsException.class)
                   .isThrownBy(() -> TemplateHelper.mappedEntity(templateType)));
  }
}