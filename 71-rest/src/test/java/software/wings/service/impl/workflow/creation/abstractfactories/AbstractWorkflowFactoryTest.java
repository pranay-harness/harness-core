package software.wings.service.impl.workflow.creation.abstractfactories;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.WingsBaseTest;

public class AbstractWorkflowFactoryTest extends WingsBaseTest {
  @Inject private AbstractWorkflowFactory abstractWorkflowFactory;

  @Test
  @Owner(developers = OwnerRule.YOGESH)
  @Category(UnitTests.class)
  public void getWorkflowCreatorFactory() {
    assertThat(abstractWorkflowFactory.getWorkflowCreatorFactory(AbstractWorkflowFactory.Category.K8S_V2))
        .isExactlyInstanceOf(K8sV2WorkflowFactory.class);
    assertThat(abstractWorkflowFactory.getWorkflowCreatorFactory(AbstractWorkflowFactory.Category.GENERAL))
        .isExactlyInstanceOf(GeneralWorkflowFactory.class);
  }
}
