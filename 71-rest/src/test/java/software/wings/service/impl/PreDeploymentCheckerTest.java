package software.wings.service.impl;

import static org.mockito.Mockito.verifyZeroInteractions;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.limits.LimitCheckerFactory;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import software.wings.WingsBaseTest;
import software.wings.app.DeployMode;

@RunWith(PowerMockRunner.class)
@PrepareForTest(PreDeploymentChecker.class)
public class PreDeploymentCheckerTest extends WingsBaseTest {
  @Mock private LimitCheckerFactory limitCheckerFactory;
  @Inject @InjectMocks private PreDeploymentChecker preDeploymentChecker;

  @Test
  @Category(UnitTests.class)
  public void checkDeploymentRateLimit() {
    PowerMockito.mockStatic(System.class);

    Mockito.when(System.getenv(DeployMode.DEPLOY_MODE)).thenReturn(DeployMode.ONPREM.toString());

    String accountId = "some-account-id";
    String appId = "some-app-id";
    preDeploymentChecker.checkDeploymentRateLimit(accountId, appId);

    verifyZeroInteractions(limitCheckerFactory);
  }
}
