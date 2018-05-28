package software.wings.delegatetasks.validation;

import static io.harness.network.Http.connectableHttpUrl;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.DelegateTask;

import java.util.List;
import java.util.function.Consumer;

/**
 * Created by brett on 11/1/17
 */
public abstract class AbstractDelegateValidateTask implements DelegateValidateTask {
  private static final Logger logger = LoggerFactory.getLogger(AbstractDelegateValidateTask.class);

  protected String delegateTaskId;

  private String accountId;
  private String delegateId;
  private Object[] parameters;
  private Consumer<List<DelegateConnectionResult>> consumer;

  public AbstractDelegateValidateTask(
      String delegateId, DelegateTask delegateTask, Consumer<List<DelegateConnectionResult>> consumer) {
    this.accountId = delegateTask.getAccountId();
    this.delegateId = delegateId;
    this.parameters = delegateTask.getParameters();
    this.delegateTaskId = delegateTask.getUuid();
    this.consumer = consumer;
  }

  @Override
  public void run() {
    List<DelegateConnectionResult> results = null;
    try {
      long startTime = System.currentTimeMillis();
      results = validate();
      long duration = System.currentTimeMillis() - startTime;
      for (DelegateConnectionResult result : results) {
        result.setAccountId(accountId);
        result.setDelegateId(delegateId);
        if (result.getDuration() == 0) {
          result.setDuration(duration);
        }
      }
    } catch (Exception exception) {
      logger.error("Unexpected error validating delegate task.", exception);
    } finally {
      if (consumer != null) {
        consumer.accept(results);
      }
    }
  }

  @Override
  public List<DelegateConnectionResult> validate() {
    try {
      String criteria = getCriteria().get(0);
      return singletonList(
          DelegateConnectionResult.builder().criteria(criteria).validated(connectableHttpUrl(criteria)).build());
    } catch (Exception e) {
      return emptyList();
    }
  }

  @SuppressFBWarnings("EI_EXPOSE_REP")
  public Object[] getParameters() {
    return parameters;
  }
}
