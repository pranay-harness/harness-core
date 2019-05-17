package software.wings.service.impl;

import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.limits.LimitCheckerFactory;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.app.DeployMode;
import software.wings.app.MainConfiguration;

public class PreDeploymentCheckerTest extends WingsBaseTest {
  @Mock private LimitCheckerFactory limitCheckerFactory;
  @Mock private MainConfiguration mainConfiguration;
  @Inject @InjectMocks private PreDeploymentChecker preDeploymentChecker;

  @Test
  @Category(UnitTests.class)
  public void checkDeploymentRateLimit() {
    when(mainConfiguration.getDeployMode()).thenReturn(DeployMode.ONPREM);

    String accountId = "some-account-id";
    String appId = "some-app-id";
    preDeploymentChecker.checkDeploymentRateLimit(accountId, appId);

    verifyZeroInteractions(limitCheckerFactory);
  }
}
