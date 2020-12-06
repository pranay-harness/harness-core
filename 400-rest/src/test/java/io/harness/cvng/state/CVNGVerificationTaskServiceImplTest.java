package io.harness.cvng.state;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.KAMAL;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.cvng.state.CVNGVerificationTask.Status;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;

import com.google.inject.Inject;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class CVNGVerificationTaskServiceImplTest extends WingsBaseTest {
  @Inject private CVNGVerificationTaskService cvngVerificationTaskService;
  private String accountId;
  private String activityId;
  @Before
  public void setup() {
    accountId = generateUuid();
    activityId = generateUuid();
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testMarkDone() {
    String cvngVerificationTaskId = cvngVerificationTaskService.create(create());
    cvngVerificationTaskService.markDone(cvngVerificationTaskId);
    CVNGVerificationTask updated = cvngVerificationTaskService.get(cvngVerificationTaskId);
    assertThat(updated.getStatus()).isEqualTo(Status.DONE);
  }

  private CVNGVerificationTask create() {
    return CVNGVerificationTask.builder()
        .accountId(accountId)
        .activityId(activityId)
        .correlationId(generateUuid())
        .status(Status.IN_PROGRESS)
        .build();
  }
}