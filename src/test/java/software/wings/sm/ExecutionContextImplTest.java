/**
 *
 */

package software.wings.sm;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Injector;

import org.junit.Test;
import software.wings.WingsBaseTest;
import software.wings.api.HostElement;
import software.wings.api.ServiceElement;
import software.wings.api.ServiceTemplateElement;
import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.common.UUIDGenerator;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.EnvironmentService;

import javax.inject.Inject;

// TODO: Auto-generated Javadoc

/**
 * The Class ExecutionContextImplTest.
 *
 * @author Rishi
 */
public class ExecutionContextImplTest extends WingsBaseTest {
  /**
   * The Injector.
   */
  @Inject Injector injector;
  /**
   * The App service.
   */
  @Inject AppService appService;
  /**
   * The Environment service.
   */
  @Inject EnvironmentService environmentService;

  /**
   * Should fetch context element.
   */
  @Test
  public void shouldFetchContextElement() {
    StateExecutionInstance stateExecutionInstance = new StateExecutionInstance();

    ExecutionContextImpl context = new ExecutionContextImpl(stateExecutionInstance);

    ServiceElement element1 = new ServiceElement();
    element1.setUuid(UUIDGenerator.getUuid());
    element1.setName("svc1");
    context.pushContextElement(element1);

    ServiceElement element2 = new ServiceElement();
    element2.setUuid(UUIDGenerator.getUuid());
    element2.setName("svc2");
    context.pushContextElement(element2);

    ServiceElement element3 = new ServiceElement();
    element3.setUuid(UUIDGenerator.getUuid());
    element3.setName("svc3");
    context.pushContextElement(element3);

    ServiceElement element = context.getContextElement(ContextElementType.SERVICE);
    assertThat(element).isNotNull().isEqualToComparingFieldByField(element3);
  }

  /**
   * Should fetch context element.
   */
  @Test
  public void shouldRenderExpression() {
    StateExecutionInstance stateExecutionInstance = new StateExecutionInstance();
    stateExecutionInstance.setStateName("abc");
    ExecutionContextImpl context = new ExecutionContextImpl(stateExecutionInstance);
    injector.injectMembers(context);

    ServiceElement svc = new ServiceElement();
    svc.setUuid(UUIDGenerator.getUuid());
    svc.setName("svc1");
    context.pushContextElement(svc);

    ServiceTemplateElement st = new ServiceTemplateElement();
    st.setUuid(UUIDGenerator.getUuid());
    st.setName("st1");
    st.setServiceElement(
        ServiceElement.Builder.aServiceElement().withUuid(UUIDGenerator.getUuid()).withName("svc2").build());
    context.pushContextElement(st);

    HostElement host = new HostElement();
    host.setUuid(UUIDGenerator.getUuid());
    host.setHostName("host1");
    context.pushContextElement(host);

    Application app = Application.Builder.anApplication().withName("AppA").build();
    app = appService.save(app);

    Environment env = Environment.EnvironmentBuilder.anEnvironment().withName("DEV").build();
    env = environmentService.save(env);

    WorkflowStandardParams std = new WorkflowStandardParams();
    std.setAppId(app.getUuid());
    std.setEnvId(env.getUuid());

    String timeStampId = std.getTimestampId();

    injector.injectMembers(std);
    context.pushContextElement(std);

    String expr =
        "$HOME/${env.name}/${app.name}/${service.name}/${serviceTemplate.name}/${host.name}/${timestampId}/runtime";
    String path = context.renderExpression(expr);
    assertThat(path).isEqualTo("$HOME/DEV/AppA/svc2/st1/host1/" + timeStampId + "/runtime");
  }
}
