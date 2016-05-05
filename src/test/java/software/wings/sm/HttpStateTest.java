/**
 *
 */
package software.wings.sm;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import software.wings.WingsBaseUnitTest;
import software.wings.beans.Host;
import software.wings.common.UUIDGenerator;

/**
 * @author Rishi
 *
 */
public class HttpStateTest extends WingsBaseUnitTest {
  @Test
  public void shouldAssertResponse() {
    ExecutionContextImpl context = new ExecutionContextImpl();
    SmInstance smInstance = new SmInstance();
    smInstance.setUuid(UUIDGenerator.getUuid());
    String stateName = "healthCheck1";
    smInstance.setStateName(stateName);
    context.setSmInstance(smInstance);

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
    assertThat(response.getStateExecutionData()).isInstanceOf(HttpState.HttpStateExecutionData.class);

    HttpState.HttpStateExecutionData stateExecutionData =
        (HttpState.HttpStateExecutionData) response.getStateExecutionData();
    assertThat(stateExecutionData.getHttpUrl()).isNotNull();
    assertThat(stateExecutionData.getHttpUrl()).isEqualTo("http://app123.application.com:8080/health/status");
  }
}
