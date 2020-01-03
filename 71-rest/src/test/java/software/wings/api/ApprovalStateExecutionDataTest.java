package software.wings.api;

import static io.harness.rule.OwnerRule.YOGESH;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.beans.EmbeddedUser;
import io.harness.beans.ExecutionStatus;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.MockitoAnnotations;
import software.wings.WingsBaseTest;
import software.wings.beans.NameValuePair;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ApprovalStateExecutionDataTest extends WingsBaseTest {
  private ApprovalStateExecutionData approvalStateExecutionData;

  @Before
  public void setUp() {
    approvalStateExecutionData = ApprovalStateExecutionData.builder()
                                     .approvalId("approvalId")
                                     .userGroups(asList("userGroupID"))
                                     .variables(asList(NameValuePair.builder().name("var-1").value("value-1").build(),
                                         NameValuePair.builder().name("var-2").build()))
                                     .approvedBy(EmbeddedUser.builder().name("admin").email("admin@harness.io").build())
                                     .comments("comment")
                                     .timeoutMillis(3000)
                                     .appId("appId")
                                     .approvalField("Status")
                                     .currentStatus("APPROVED")
                                     .approvalValue("DONE")
                                     .rejectionField("Status")
                                     .rejectionValue("REJECTED")
                                     .build();
    MockitoAnnotations.initMocks(approvalStateExecutionData);
    approvalStateExecutionData.setStatus(ExecutionStatus.SUCCESS);
    approvalStateExecutionData.setStartTs(0L);
    approvalStateExecutionData.setEndTs(1000L);
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void getExecutionSummary() {
    final Map<String, ExecutionDataValue> executionSummary = approvalStateExecutionData.getExecutionSummary();
    List<String> keys = executionSummary.keySet().stream().collect(Collectors.toList());
    assertThat(keys).isNotNull().containsExactlyInAnyOrder("total", "breakdown", "approvalId", "status", "timeoutMins",
        "approvalCriteria", "currentStatus", "rejectionCriteria", "approvedBy", "variables", "comments");
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void getExecutionDetails() {
    final Map<String, ExecutionDataValue> executionDetails = approvalStateExecutionData.getExecutionDetails();
    List<String> keys = executionDetails.keySet().stream().collect(Collectors.toList());
    assertThat(keys).isNotNull().containsExactlyInAnyOrder("startTs", "endTs", "authorizationStatus",
        "isUserAuthorized", "approvalId", "status", "timeoutMins", "approvalCriteria", "currentStatus",
        "rejectionCriteria", "approvedBy", "variables", "comments");
  }
}