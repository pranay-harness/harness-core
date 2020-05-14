package software.wings.delegatetasks.citasks.cik8handler;

import static io.harness.rule.OwnerRule.SHUBHAM;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;
import static okhttp3.Protocol.HTTP_1_0;
import static org.joor.Reflect.on;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;

import io.fabric8.kubernetes.client.dsl.ExecWatch;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import software.wings.WingsBaseTest;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

public class K8ExecCommandListenerTest extends WingsBaseTest {
  @InjectMocks K8ExecCommandListener k8ExecCommandListener;

  private static final Protocol protocol = HTTP_1_0;
  private static final int successCode = 200;
  private static final int failureCode = 400;
  private static final String url = "http://harness.io";
  private static final String successMsg = "Success";
  private static final String errMsg = "Error";
  private static final Integer timeoutSecs = 10;
  private static final Integer smallTimeoutSec = 0;

  private Response getSuccessResponse() {
    Request request = new Request.Builder().url(url).build();
    return new Response.Builder().request(request).protocol(protocol).code(successCode).message(successMsg).build();
  }

  private Response getErrResponse() {
    Request request = new Request.Builder().url(url).build();
    return new Response.Builder().request(request).protocol(protocol).code(failureCode).message(errMsg).build();
  }

  @Test()
  @Owner(developers = SHUBHAM)
  @Category(UnitTests.class)
  public void onOpen() {
    Response response = getSuccessResponse();
    AtomicBoolean status = new AtomicBoolean(false);
    on(k8ExecCommandListener).set("cmdStatus", status);

    k8ExecCommandListener.onOpen(response);
    assertFalse(status.get());
  }

  @Test()
  @Owner(developers = SHUBHAM)
  @Category(UnitTests.class)
  public void onSuccess() {
    Response response = getSuccessResponse();
    AtomicBoolean status = new AtomicBoolean(false);
    on(k8ExecCommandListener).set("cmdStatus", status);

    k8ExecCommandListener.onClose(successCode, successMsg);
    assertTrue(status.get());
  }

  @Test()
  @Owner(developers = SHUBHAM)
  @Category(UnitTests.class)
  public void onFailure() {
    AtomicBoolean status = new AtomicBoolean(false);
    on(k8ExecCommandListener).set("cmdStatus", status);
    Response response = getErrResponse();

    k8ExecCommandListener.onFailure(new Exception("Failure"), response);
    assertFalse(status.get());
  }

  @Test()
  @Owner(developers = SHUBHAM)
  @Category(UnitTests.class)
  public void getReturnStatusWithSuccessStatus() throws TimeoutException, InterruptedException {
    ExecWatch watch = mock(ExecWatch.class);
    Semaphore wait = new Semaphore(0);
    AtomicBoolean status = new AtomicBoolean(false);
    on(k8ExecCommandListener).set("execWait", wait);
    on(k8ExecCommandListener).set("cmdStatus", status);
    status.set(true);
    wait.release();

    assertTrue(k8ExecCommandListener.isCommandExecutionComplete(timeoutSecs));
  }

  @Test()
  @Owner(developers = SHUBHAM)
  @Category(UnitTests.class)
  public void getReturnStatusWithFailStatus() throws TimeoutException, InterruptedException {
    ExecWatch watch = mock(ExecWatch.class);
    Semaphore wait = new Semaphore(0);
    AtomicBoolean status = new AtomicBoolean(false);
    on(k8ExecCommandListener).set("execWait", wait);
    on(k8ExecCommandListener).set("cmdStatus", status);
    status.set(false);
    wait.release();

    assertFalse(k8ExecCommandListener.isCommandExecutionComplete(timeoutSecs));
  }

  @Test(expected = TimeoutException.class)
  @Owner(developers = SHUBHAM)
  @Category(UnitTests.class)
  public void getReturnStatusWithException() throws TimeoutException, InterruptedException {
    ExecWatch watch = mock(ExecWatch.class);
    Semaphore wait = new Semaphore(0);
    AtomicBoolean status = new AtomicBoolean(false);
    on(k8ExecCommandListener).set("execWait", wait);
    on(k8ExecCommandListener).set("cmdStatus", status);
    status.set(false);
    doNothing().when(watch).close();

    k8ExecCommandListener.isCommandExecutionComplete(smallTimeoutSec);
  }
}