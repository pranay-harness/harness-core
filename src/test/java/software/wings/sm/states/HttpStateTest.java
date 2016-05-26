/**
 *
 */
package software.wings.sm.states;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import software.wings.WingsBaseTest;
import software.wings.api.HttpStateExecutionData;
import software.wings.beans.Host;
import software.wings.common.UUIDGenerator;
import software.wings.sm.ExecutionContextFactory;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.ExpressionProcessorFactory;
import software.wings.sm.StateExecutionInstance;
import software.wings.utils.ExpressionEvaluator;

import javax.inject.Inject;

/**
 * @author Rishi
 *
 */
public class HttpStateTest extends WingsBaseTest {
  @Inject private ExecutionContextFactory executionContextFactory;

  @Test
  public void shouldAssertResponse() {
    StateExecutionInstance stateExecutionInstance = new StateExecutionInstance();
    String stateName = "healthCheck1";
    stateExecutionInstance.setUuid(UUIDGenerator.getUuid());
    stateExecutionInstance.setStateName(stateName);

    ExecutionContextImpl context = executionContextFactory.create(stateExecutionInstance, null);

    Host host = new Host();
    host.setHostName("app123.application.com");
    context.pushContextElement(host);

    HttpState httpState = new HttpState(stateName);
    httpState.setUrl("http://${host.hostName}:8080/health/status");

    String assertion =
        "(${httpResponseCode}==200 || ${httpResponseCode}==201) && ${xmlFormat()} && ${xpath('//health/status/text()')}.equals('Enabled')";
    httpState.setAssertion(assertion);

    ExecutionResponse response = httpState.execute(context);
    assertThat(response).isNotNull();
    assertThat(response.isAsynch()).as("Asynch Execution").isEqualTo(false);
    assertThat(response.getStateExecutionData()).isNotNull();
    assertThat(response.getStateExecutionData()).isInstanceOf(HttpStateExecutionData.class);

    HttpStateExecutionData stateExecutionData = (HttpStateExecutionData) response.getStateExecutionData();
    assertThat(stateExecutionData.getHttpUrl()).isNotNull();
    assertThat(stateExecutionData.getHttpUrl()).isEqualTo("http://app123.application.com:8080/health/status");
  }
}
