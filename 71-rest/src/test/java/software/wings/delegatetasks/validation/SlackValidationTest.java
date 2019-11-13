package software.wings.delegatetasks.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.DELEGATE_ID;

import io.harness.beans.DelegateTask;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.TaskData;
import io.harness.exception.InvalidRequestException;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import software.wings.WingsBaseTest;
import software.wings.beans.SlackMessage;
import software.wings.beans.TaskType;

import java.util.List;

public class SlackValidationTest extends WingsBaseTest {
  @InjectMocks private SlackValidation slackValidation = new SlackValidation(DELEGATE_ID, delegateTask, null);

  private static final String OUTGOING_WEBHOOK_URL = "https://app.harness.io";
  private static final String INCORRECT_OUTGOING_WEBHOOK_URL = "ABCharness.io";
  private static final String SLACK_CHANNEL = "channel";
  private static final String SENDER_NAME = "abc";
  private static final String MESSAGE = "message";
  static DelegateTask delegateTask =
      DelegateTask.builder()
          .uuid("id")
          .async(true)
          .accountId(ACCOUNT_ID)
          .appId(APP_ID)
          .waitId("")
          .data(TaskData.builder()
                    .taskType(TaskType.SLACK.name())
                    .parameters(new Object[] {
                        null, null, new SlackMessage(OUTGOING_WEBHOOK_URL, SLACK_CHANNEL, SENDER_NAME, MESSAGE)})
                    .build())
          .build();

  @Test
  @Category(UnitTests.class)
  public void getCriteriaTest() {
    List<String> criteria = slackValidation.getCriteria();
    assertThat(criteria).hasSize(1);
    assertThat(criteria.get(0)).isEqualTo(OUTGOING_WEBHOOK_URL);
  }

  @Test(expected = InvalidRequestException.class)
  @Category(UnitTests.class)
  public void getCriteriaTestShouldFail() {
    DelegateTask delegateTask =
        DelegateTask.builder()
            .uuid("id")
            .async(true)
            .accountId(ACCOUNT_ID)
            .appId(APP_ID)
            .waitId("")
            .data(TaskData.builder()
                      .taskType(TaskType.SLACK.name())
                      .parameters(new Object[] {null, null,
                          new SlackMessage(INCORRECT_OUTGOING_WEBHOOK_URL, SLACK_CHANNEL, SENDER_NAME, MESSAGE)})
                      .build())
            .build();
    SlackValidation slackValidation = new SlackValidation(DELEGATE_ID, delegateTask, null);
    slackValidation.getCriteria();
  }

  @Test
  @Category(UnitTests.class)
  public void validateSuccessTest() {
    List<DelegateConnectionResult> result = slackValidation.validate();
    assertThat(result).hasSize(1);
    assertThat(result.get(0).isValidated()).isTrue();
  }
}
