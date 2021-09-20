package io.harness.pms.expressions.functors;

import static io.harness.rule.OwnerRule.BRIJESH;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static org.joor.Reflect.on;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.harness.CategoryTest;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.expression.ExpressionRequest;
import io.harness.pms.contracts.expression.ExpressionResponse;
import io.harness.pms.contracts.expression.RemoteFunctorServiceGrpc.RemoteFunctorServiceBlockingStub;
import io.harness.rule.Owner;

import java.util.Map;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@OwnedBy(HarnessTeam.PIPELINE)
@RunWith(PowerMockRunner.class)
@PrepareForTest(RemoteFunctorServiceBlockingStub.class)
public class RemoteExpressionFunctorTest extends CategoryTest {
  @Mock RemoteFunctorServiceBlockingStub blockingStub;
  @InjectMocks RemoteExpressionFunctor remoteExpressionFunctor;
  static String expressionResponseJson =
      "{\"__recast\":\"io.harness.pms.sdk.core.execution.expression.StringResult\",\"value\":\"DummyValue\"}";

  @Test
  @Owner(developers = BRIJESH)
  @Category(UnitTests.class)
  public void testGet() {
    PowerMockito.mockStatic(RemoteFunctorServiceBlockingStub.class);
    Ambiance ambiance = Ambiance.newBuilder().build();
    on(remoteExpressionFunctor).set("ambiance", ambiance);
    on(remoteExpressionFunctor).set("functorKey", "functorKey");

    ArgumentCaptor<ExpressionRequest> argumentCaptor = ArgumentCaptor.forClass(ExpressionRequest.class);
    doReturn(ExpressionResponse.newBuilder().setValue(expressionResponseJson).build())
        .when(blockingStub)
        .evaluate(any());

    // For single string as argument
    Map<String, Object> map = (Map<String, Object>) remoteExpressionFunctor.get("empty");
    verify(blockingStub, times(1)).evaluate(argumentCaptor.capture());
    ExpressionRequest request = argumentCaptor.getValue();
    assertEquals(request.getAmbiance(), ambiance);
    assertEquals(request.getArgsList().size(), 1);
    assertEquals(request.getFunctorKey(), "functorKey");
    assertNotNull(map);
    assertEquals(map.get("value"), "DummyValue");

    // For array of strings as argument
    String[] allArgs = {"empty", "arg1"};
    map = (Map<String, Object>) remoteExpressionFunctor.get(allArgs);
    verify(blockingStub, times(2)).evaluate(argumentCaptor.capture());
    request = argumentCaptor.getValue();
    assertEquals(request.getAmbiance(), ambiance);
    assertEquals(request.getArgsList().size(), 2);
    assertEquals(request.getFunctorKey(), "functorKey");
    assertNotNull(map);
    assertEquals(map.get("value"), "DummyValue");
  }
}
