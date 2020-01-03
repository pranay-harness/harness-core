package software.wings.service.impl.instance;

import static io.harness.rule.OwnerRule.ROHIT_KUMAR;

import io.harness.category.element.UnitTests;
import io.harness.exception.WingsException;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import software.wings.WingsBaseTest;
import software.wings.beans.InfrastructureMappingType;

public class InstanceHandlerTest extends WingsBaseTest {
  @Spy InstanceUtils instanceUtil;
  @InjectMocks InstanceHandler instanceHandler = Mockito.mock(InstanceHandler.class, Mockito.CALLS_REAL_METHODS);
  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = ROHIT_KUMAR)
  @Category(UnitTests.class)
  public void validateInstanceType_valid_inframappings() {
    instanceHandler.validateInstanceType(InfrastructureMappingType.DIRECT_KUBERNETES.name());
    instanceHandler.validateInstanceType(InfrastructureMappingType.AWS_SSH.name());
  }

  @Test(expected = WingsException.class)
  @Owner(developers = ROHIT_KUMAR)
  @Category(UnitTests.class)
  public void validateInstanceType_invalid_or_not_supported_infra() {
    instanceHandler.validateInstanceType("abc");
  }
}
