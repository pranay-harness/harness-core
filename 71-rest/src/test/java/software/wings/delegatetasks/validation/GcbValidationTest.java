package software.wings.delegatetasks.validation;

import static io.harness.rule.OwnerRule.AGORODETKI;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.helpers.ext.gcb.GcbServiceImpl.GCB_BASE_URL;
import static software.wings.helpers.ext.gcb.GcbServiceImpl.GCS_BASE_URL;

import io.harness.CategoryTest;
import io.harness.beans.DelegateTask;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.TaskData;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.Arrays;

public class GcbValidationTest extends CategoryTest {
  GcbValidation gcbValidation =
      new GcbValidation("id", DelegateTask.builder().data(TaskData.builder().taskType("type").build()).build(),
          delegateConnectionResults -> {});

  @Test
  @Owner(developers = AGORODETKI)
  @Category(UnitTests.class)
  public void shouldReturnListOfCriteria() {
    assertThat(gcbValidation.getCriteria()).isEqualTo(Arrays.asList(GCB_BASE_URL, GCS_BASE_URL));
  }

  @Test
  @Owner(developers = AGORODETKI)
  @Category(UnitTests.class)
  public void shouldValidateCriteria() {
    assertThat(gcbValidation.validate().size()).isEqualTo(2);
  }
}
