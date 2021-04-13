package io.harness.ngtriggers.helpers;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.rule.OwnerRule.MATT;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.ngtriggers.beans.entity.TriggerWebhookEvent;
import io.harness.ngtriggers.beans.scm.WebhookPayloadData;
import io.harness.ngtriggers.utils.WebhookTriggerFilterUtils;
import io.harness.rule.Owner;

import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(PIPELINE)
public class JexlScenarioTest extends CategoryTest {
  private String smallPayload = "{\n"
      + "  \"Type\" : \"Notification\",\n"
      + "  \"MessageId\" : \"43eb6d30-4059-59d7-a714-35e414b6c4b9\",\n"
      + "  \"TopicArn\" : \"arn:aws:sns:eu-central-1:448640225317:aws_cc_push_trigger\",\n"
      + "  \"Subject\" : \"UPDATE: AWS CodeCommit eu-central-1 push: test\",\n"
      + "  \"Timestamp\" : \"2021-03-23T20:42:23.163Z\",\n"
      + "  \"arr\" : [\"abc\", \"def\"],\n"
      + "  \"SignatureVersion\" : \"1\"\n"
      + "}";

  private String bigPayload = "{\n"
      + " \"Type\" : \"Notification\",\n"
      + " \"MessageId\" : \"43eb6d30-4059-59d7-a714-35e414b6c4b9\",\n"
      + " \"TopicArn\" : \"arn:aws:sns:eu-central-1:448640225317:aws_cc_push_trigger\",\n"
      + " \"Subject\" : \"UPDATE: AWS CodeCommit eu-central-1 push: test\",\n"
      + " \"Message\" : \"{\\\"Records\\\":[{\\\"awsRegion\\\":\\\"eu-central-1\\\",\\\"codecommit\\\":{\\\"references\\\":[{\\\"commit\\\":\\\"f3c8a35a6e83e9ab85c05845c6731c7990de8672\\\",\\\"ref\\\":\\\"refs/heads/main\\\"}]},\\\"customData\\\":null,\\\"eventId\\\":\\\"58939308-51e9-4dad-abd5-97c8dabe3815\\\",\\\"eventName\\\":\\\"ReferenceChanges\\\",\\\"eventPartNumber\\\":1,\\\"eventSource\\\":\\\"aws:codecommit\\\",\\\"eventSourceARN\\\":\\\"arn:aws:codecommit:eu-central-1:448640225317:test\\\",\\\"eventTime\\\":\\\"2021-03-23T20:42:23.131+0000\\\",\\\"eventTotalParts\\\":1,\\\"eventTriggerConfigId\\\":\\\"96765393-40e7-4f49-aed7-64c9ba599195\\\",\\\"eventTriggerName\\\":\\\"push\\\",\\\"eventVersion\\\":\\\"1.0\\\",\\\"userIdentityARN\\\":\\\"arn:aws:iam::448640225317:user/vault-okta-aleksandar.radisavljevic@harness.io-a1616531435-217\\\"}]}\",\n"
      + " \"Timestamp\" : \"2021-03-23T20:42:23.163Z\",\n"
      + " \"SignatureVersion\" : \"1\",\n"
      + " \"Signature\" : \"qWIfB0MLIwWfGGEe+JgloCOL47LdizJU7Y9ETbz6jIeH/NNkyBYtzys18Q0TyJOsKzagddG1wqeS0K5n0Ewz2ZZys5nQNt3EAgagW8SrajWnToSuhFhAvJxxBhBsnHAoU7pV+4cZIWLKhF8ro4otm9xuDxnLD2sWTR8+xXzVdJJGDhcbxnykNJZ05O6kV/TEeCSCUBdQkhVZmyKM8XrN39QMnC8zZkFXXE9ssy51zVuH7IpSsq44pmEZQYA5lFXVHyu7p5fjih3Et03/IJg5i77zCjGRbjeRcER1JPlL74d+0U5jfINDRYSfc/gDBKQ3/zArLF12REO1orivX76NIQ==\","
      + " \"SigningCertURL\" : \"https://sns.eu-central-1.amazonaws.com/SimpleNotificationService-010a507c1833636cd94bdb98bd93083a.pem\",\n"
      + " \"UnsubscribeURL\" : \"https://sns.eu-central-1.amazonaws.com/?Action=Unsubscribe&SubscriptionArn=arn:aws:sns:eu-central-1:448640225317:aws_cc_push_trigger:bf6ca40a-eb7c-43a8-b452-ec5869813da4\"\n"
      + "}";

  @Test
  @Owner(developers = MATT)
  @Category(UnitTests.class)
  public void testSimplePayload() {
    WebhookPayloadData webhookPayloadData =
        WebhookPayloadData.builder().originalEvent(TriggerWebhookEvent.builder().payload(smallPayload).build()).build();
    assertThat(WebhookTriggerFilterUtils.checkIfJexlConditionsMatch(
                   webhookPayloadData, "<+eventPayload.arr.contains(\"abc\")>"))
        .isTrue();
  }

  @Test
  @Owner(developers = MATT)
  @Category(UnitTests.class)
  public void testJexlAnd() {
    WebhookPayloadData webhookPayloadData =
        WebhookPayloadData.builder().originalEvent(TriggerWebhookEvent.builder().payload(bigPayload).build()).build();
    assertThat(WebhookTriggerFilterUtils.checkIfJexlConditionsMatch(
                   webhookPayloadData, "eventPayload.Type == \"Notification\" && eventPayload.SignatureVersion == 1"))
        .isTrue();
  }

  @Test
  @Owner(developers = MATT)
  @Category(UnitTests.class)
  public void testJexlOr() {
    WebhookPayloadData webhookPayloadData =
        WebhookPayloadData.builder().originalEvent(TriggerWebhookEvent.builder().payload(bigPayload).build()).build();
    assertThat(WebhookTriggerFilterUtils.checkIfJexlConditionsMatch(
                   webhookPayloadData, "eventPayload.Subject.contains(\"GIT\") || eventPayload.SignatureVersion == 1"))
        .isTrue();
  }

  @Test
  @Owner(developers = MATT)
  @Category(UnitTests.class)
  public void testJexlArithmetic() {
    WebhookPayloadData webhookPayloadData =
        WebhookPayloadData.builder().originalEvent(TriggerWebhookEvent.builder().payload(bigPayload).build()).build();
    assertThat(WebhookTriggerFilterUtils.checkIfJexlConditionsMatch(
                   webhookPayloadData, "size(eventPayload.Signature) + size(eventPayload.MessageId) > 300"))
        .isTrue();
  }

  @Test
  @Owner(developers = MATT)
  @Category(UnitTests.class)
  public void testJexlParens() {
    WebhookPayloadData webhookPayloadData =
        WebhookPayloadData.builder().originalEvent(TriggerWebhookEvent.builder().payload(bigPayload).build()).build();
    assertThat(
        WebhookTriggerFilterUtils.checkIfJexlConditionsMatch(webhookPayloadData,
            "(size(eventPayload.Signature) + size(eventPayload.MessageId) > 300) && (eventPayload.Subject.contains(\"GIT\") || eventPayload.SignatureVersion == 1)"))
        .isTrue();
  }

  // If there's a way to parse a string to json using jexl, we can read the message to parse nested json
  // @Test
  // @Owner(developers = MATT)
  // @Category(UnitTests.class)
  // public void testJexlJsonParser() {
  //   WebhookPayloadData webhookPayloadData =
  //           WebhookPayloadData.builder()
  //                   .originalEvent(TriggerWebhookEvent.builder().payload(bigPayload).build()).build();
  //   assertThat(WebhookTriggerFilterUtils.checkIfJexlConditionsMatch(
  //       webhookPayloadData,"size(JSON:parse(eventPayload.Message)) > 0")).isTrue();
  // }
}
