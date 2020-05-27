package io.harness.execution.export.metadata;

import static io.harness.rule.OwnerRule.GARVIT;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.api.EnvStateExecutionData.Builder.anEnvStateExecutionData;

import io.harness.CategoryTest;
import io.harness.beans.EmbeddedUser;
import io.harness.beans.ExecutionStatus;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.api.ApprovalStateExecutionData;
import software.wings.sm.states.ApprovalState.ApprovalStateType;

import java.time.Instant;

public class ApprovalMetadataTest extends CategoryTest {
  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testFromStateExecutionData() {
    assertThat(ApprovalMetadata.fromStateExecutionData(null)).isNull();
    assertThat(ApprovalMetadata.fromStateExecutionData(anEnvStateExecutionData().build())).isNull();

    Instant now = Instant.now();
    ApprovalStateExecutionData approvalStateExecutionData =
        ApprovalStateExecutionData.builder()
            .timeoutMillis(1000)
            .approvalStateType(ApprovalStateType.USER_GROUP)
            .userGroups(asList(null, "ug1"))
            .variables(asList(MetadataTestUtils.prepareNameValuePair(1), MetadataTestUtils.prepareNameValuePair(2)))
            .approvedBy(EmbeddedUser.builder().name("n").email("e").build())
            .approvedOn(now.toEpochMilli())
            .comments("c")
            .build();
    approvalStateExecutionData.setStatus(ExecutionStatus.SUCCESS);
    ApprovalMetadata approvalMetadata = ApprovalMetadata.fromStateExecutionData(approvalStateExecutionData);
    assertThat(approvalMetadata).isNotNull();
    assertThat(approvalMetadata.getStatus()).isEqualTo(ExecutionStatus.SUCCESS);
    assertThat(approvalMetadata.getTimeout().toMillis()).isEqualTo(1000);
    assertThat(approvalMetadata.getApprovalType()).isEqualTo(ApprovalStateType.USER_GROUP);
    assertThat(approvalMetadata.getUserGroupIds()).isNotNull();
    assertThat(approvalMetadata.getUserGroupIds()).containsExactly("ug1");
    assertThat(approvalMetadata.getUserGroups()).isNull();
    assertThat(approvalMetadata.getVariables()).isNotNull();
    assertThat(approvalMetadata.getVariables().size()).isEqualTo(2);
    assertThat(approvalMetadata.getApprovedBy()).isNotNull();
    assertThat(approvalMetadata.getApprovedBy().getName()).isEqualTo("n");
    assertThat(approvalMetadata.getApprovedBy().getEmail()).isEqualTo("e");
    assertThat(approvalMetadata.getApprovedOn().toInstant()).isEqualTo(now);
    assertThat(approvalMetadata.getComments()).isEqualTo("c");
  }

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testAddUserGroup() {
    ApprovalMetadata approvalMetadata = ApprovalMetadata.builder().build();
    approvalMetadata.addUserGroup(null);
    assertThat(approvalMetadata.getUserGroups()).isNull();

    approvalMetadata.addUserGroup("ug1");
    assertThat(approvalMetadata.getUserGroups()).isNotNull();
    assertThat(approvalMetadata.getUserGroups()).containsExactly("ug1");

    approvalMetadata.addUserGroup("ug2");
    assertThat(approvalMetadata.getUserGroups()).isNotNull();
    assertThat(approvalMetadata.getUserGroups()).containsExactly("ug1", "ug2");
  }
}
