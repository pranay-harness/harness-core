package software.wings.delegatetasks.validation;

import static io.harness.network.Http.connectableHttpUrl;
import static java.util.Collections.singletonList;

import io.harness.beans.DelegateTask;
import io.harness.exception.InvalidRequestException;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.SlackMessage;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.function.Consumer;

@Slf4j
public class SlackValidation extends AbstractDelegateValidateTask {
  public SlackValidation(final String delegateId, final DelegateTask delegateTask,
      final Consumer<List<DelegateConnectionResult>> postExecute) {
    super(delegateId, delegateTask, postExecute);
  }

  @Override
  public List<DelegateConnectionResult> validate() {
    List<String> criteria = getCriteria();
    if (criteria.size() == 0) {
      throw new InvalidRequestException("Criteria should be not empty for slack validation");
    }
    String url = criteria.get(0);
    return singletonList(DelegateConnectionResult.builder().criteria(url).validated(connectableHttpUrl(url)).build());
  }

  @Override
  public List<String> getCriteria() {
    Object parameter = getParameters()[2];
    String outgoingWebhookUrl;
    if (parameter instanceof SlackMessage) {
      outgoingWebhookUrl = ((SlackMessage) parameter).getOutgoingWebhookUrl();
    } else {
      throw new InvalidRequestException("Expected the parameter to be of type: SlackMessagePayload");
    }

    try {
      URL url = new URL(outgoingWebhookUrl);
      String baseUrl = url.getProtocol() + "://" + url.getHost();
      return singletonList(baseUrl);
    } catch (MalformedURLException e) {
      logger.error("Slack URL is incorrect", e);
      throw new InvalidRequestException("Could not create slack webhook url for validation");
    }
  }
}
