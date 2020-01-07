package software.wings.service.impl.expression;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.RAGHU;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.beans.Application.Builder.anApplication;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.WingsBaseTest;
import software.wings.api.DeploymentType;
import software.wings.api.PcfInstanceElement.PcfInstanceElementKeys;
import software.wings.beans.Service;

import java.util.List;

public class ServiceExpressionBuilderTest extends WingsBaseTest {
  private String appId;
  @Inject private ServiceExpressionBuilder serviceExpressionBuilder;

  @Before
  public void setUp() {
    appId = wingsPersistence.save(anApplication().name(generateUuid()).accountId(generateUuid()).build());
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void shouldNotReturnPcfExpression() {
    final String serviceId = wingsPersistence.save(
        Service.builder().appId(appId).name(generateUuid()).deploymentType(DeploymentType.ECS).build());
    final List<String> continuousVerificationVariables =
        serviceExpressionBuilder.getContinuousVerificationVariables(appId, serviceId);

    assertThat(continuousVerificationVariables.size()).isEqualTo(1);
    assertThat(continuousVerificationVariables.get(0)).isEqualTo("host.hostName");
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void shouldReturnPcfExpression() {
    final String serviceId = wingsPersistence.save(
        Service.builder().appId(appId).name(generateUuid()).deploymentType(DeploymentType.PCF).build());
    final List<String> continuousVerificationVariables =
        serviceExpressionBuilder.getContinuousVerificationVariables(appId, serviceId);

    assertThat(continuousVerificationVariables.size()).isEqualTo(4);
    assertThat(continuousVerificationVariables.get(0)).isEqualTo("host.hostName");
    assertThat(continuousVerificationVariables.get(1))
        .isEqualTo("host.pcfElement." + PcfInstanceElementKeys.applicationId);
    assertThat(continuousVerificationVariables.get(2))
        .isEqualTo("host.pcfElement." + PcfInstanceElementKeys.displayName);
    assertThat(continuousVerificationVariables.get(3))
        .isEqualTo("host.pcfElement." + PcfInstanceElementKeys.instanceIndex);
  }
}
