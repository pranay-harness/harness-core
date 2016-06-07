package software.wings.sm.states;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Injector;

import org.junit.Test;
import software.wings.WingsBaseTest;
import software.wings.api.HostElement;
import software.wings.api.HttpStateExecutionData;
import software.wings.common.UUIDGenerator;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.StateExecutionInstance;

import javax.inject.Inject;

// TODO: Auto-generated Javadoc

/**
 * The Class HttpStateTest.
 *
 * @author Rishi
 */
public class HttpStateTest extends WingsBaseTest {
  @Inject private Injector injector;

  /**
   * Should assert response.
   */
  @Test
  public void shouldAssertResponse() {
    StateExecutionInstance stateExecutionInstance = new StateExecutionInstance();
    String stateName = "healthCheck1";
    stateExecutionInstance.setUuid(UUIDGenerator.getUuid());
    stateExecutionInstance.setStateName(stateName);

    ExecutionContextImpl context = new ExecutionContextImpl(stateExecutionInstance, null, injector);

    HostElement host = new HostElement();
    host.setHostName("app123.application.com");
    context.pushContextElement(host);

    HttpState httpState = new HttpState(stateName);
    httpState.setUrl("http://${host.hostName}:8080/health/status");

    String assertion =
        "(${httpResponseCode}==200 || ${httpResponseCode}==201) && ${xmlFormat()} && ${xpath('//health/status/text()')}.equals('Enabled')";
    httpState.setAssertion(assertion);

    ExecutionResponse response = httpState.execute(context);
    assertThat(response).isNotNull();
    assertThat(response.isAsynch()).as("Asynch Execution").isFalse();
    assertThat(response.getStateExecutionData()).isNotNull().isInstanceOf(HttpStateExecutionData.class);

    HttpStateExecutionData stateExecutionData = (HttpStateExecutionData) response.getStateExecutionData();
    assertThat(stateExecutionData.getHttpUrl())
        .isNotNull()
        .isEqualTo("http://app123.application.com:8080/health/status");
  }
}
