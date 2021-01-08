
package io.harness.states;

import static io.harness.rule.OwnerRule.HARSH;

import static org.assertj.core.api.Assertions.assertThat;
import static org.joor.Reflect.on;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.ci.pod.ConnectorDetails;
import io.harness.delegate.beans.connector.scm.GitAuthType;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitConfigDTO;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitHTTPAuthenticationDTO;
import io.harness.encryption.SecretRefData;
import io.harness.executionplan.CIExecutionTest;
import io.harness.ngpipeline.status.BuildStatusUpdateParameter;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.rule.Owner;
import io.harness.stateutils.buildstate.ConnectorUtils;

import com.google.inject.Inject;
import java.io.IOException;
import java.util.HashMap;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;

public class BuildStatusStepTest extends CIExecutionTest {
  @Mock private ConnectorUtils connectorUtils;
  @Inject private BuildStatusStep buildStatusStep;

  @Before
  public void setUp() {
    on(buildStatusStep).set("connectorUtils", connectorUtils);
  }

  @Test
  @Owner(developers = HARSH)
  @Category(UnitTests.class)
  @Ignore("Recreate test object after pms integration")
  public void shouldExecuteBuildStatusStep() throws IOException {
    HashMap<String, String> setupAbstractions = new HashMap<>();
    setupAbstractions.put("accountId", "accountId");
    setupAbstractions.put("projectId", "projectId");
    setupAbstractions.put("orgId", "orgId");
    setupAbstractions.put("expressionFunctorToken", "1234");
    Ambiance ambiance = Ambiance.newBuilder().putAllSetupAbstractions(setupAbstractions).build();
    GitConfigDTO gitConfigDTO = GitConfigDTO.builder()
                                    .url("https://github.com/wings-software/portal.git")
                                    .gitAuth(GitHTTPAuthenticationDTO.builder()
                                                 .username("username")
                                                 .passwordRef(SecretRefData.builder().build())
                                                 .build())
                                    .gitAuthType(GitAuthType.HTTP)
                                    .build();

    when(connectorUtils.getConnectorDetails(any(), any()))
        .thenReturn(ConnectorDetails.builder().connectorConfig(gitConfigDTO).build());

    assertThat(buildStatusStep.obtainTask(ambiance, BuildStatusUpdateParameter.builder().desc("desc").build(), null))
        .isNotNull();
  }
}
