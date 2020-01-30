package software.wings.delegatetasks.validation;

import static io.harness.rule.OwnerRule.VAIBHAV_SI;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import io.harness.beans.DelegateTask;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.TaskData;
import io.harness.rule.Owner;
import org.joor.Reflect;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.helpers.ext.helm.HelmConstants.HelmVersion;
import software.wings.helpers.ext.helm.HelmDeployService;
import software.wings.utils.WingsTestConstants;

public class HelmCommandValidationTest extends WingsBaseTest {
  @Mock HelmDeployService helmDeployService;
  @Mock ContainerValidationHelper containerValidationHelper;

  private HelmCommandValidation helmCommandValidation = spy(new HelmCommandValidation(WingsTestConstants.DELEGATE_ID,
      DelegateTask.builder().data(TaskData.builder().parameters(new Object[] {}).build()).build(), null));

  @Before
  public void setUp() throws Exception {
    Reflect.on(helmCommandValidation).set("helmDeployService", helmDeployService);
    Reflect.on(helmCommandValidation).set("containerValidationHelper", containerValidationHelper);
  }

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void testGetCriteria() {
    doReturn("").when(containerValidationHelper).getCriteria(null);

    assertThat(helmCommandValidation.getCriteria(null, HelmVersion.V3)).isEqualTo("helm3: ");
    assertThat(helmCommandValidation.getCriteria(null, HelmVersion.V2)).isEqualTo("helm: ");
    assertThat(helmCommandValidation.getCriteria(null, null)).isEqualTo("helm: ");
  }
}