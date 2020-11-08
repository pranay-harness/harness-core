
package io.harness.states;

import static io.harness.rule.OwnerRule.HARSH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.joor.Reflect.on;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import com.google.inject.Inject;

import io.harness.ambiance.Ambiance;
import io.harness.category.element.UnitTests;
import io.harness.connector.apis.dto.ConnectorDTO;
import io.harness.connector.apis.dto.ConnectorInfoDTO;
import io.harness.delegate.beans.connector.gitconnector.GitAuthType;
import io.harness.delegate.beans.connector.gitconnector.GitConfigDTO;
import io.harness.delegate.beans.connector.gitconnector.GitHTTPAuthenticationDTO;
import io.harness.encryption.SecretRefData;
import io.harness.executionplan.CIExecutionTest;
import io.harness.ngpipeline.status.BuildStatusUpdateParameter;
import io.harness.rule.Owner;
import io.harness.stateutils.buildstate.ConnectorUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import software.wings.beans.ci.pod.ConnectorDetails;

import java.io.IOException;
import java.util.HashMap;

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
  public void shouldExecuteBuildStatusStep() throws IOException {
    HashMap<String, String> setupAbstractions = new HashMap<>();
    setupAbstractions.put("accountId", "accountId");
    setupAbstractions.put("projectId", "projectId");
    setupAbstractions.put("orgId", "orgId");
    Ambiance ambiance = Ambiance.builder().setupAbstractions(setupAbstractions).build();
    GitConfigDTO gitConfigDTO = GitConfigDTO.builder()
                                    .url("https://github.com/wings-software/portal.git")
                                    .gitAuth(GitHTTPAuthenticationDTO.builder()
                                                 .username("username")
                                                 .passwordRef(SecretRefData.builder().build())
                                                 .build())
                                    .gitAuthType(GitAuthType.HTTP)
                                    .build();

    when(connectorUtils.getConnectorDetails(any(), any()))
        .thenReturn(
            ConnectorDetails.builder()
                .connectorDTO(ConnectorDTO.builder()
                                  .connectorInfo(ConnectorInfoDTO.builder().connectorConfig(gitConfigDTO).build())
                                  .build())
                .build());

    assertThat(buildStatusStep.obtainTask(ambiance, BuildStatusUpdateParameter.builder().desc("desc").build(), null))
        .isNotNull();
  }
}
