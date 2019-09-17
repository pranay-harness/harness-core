package software.wings.sm.states.spotinst;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.delegate.task.aws.LoadBalancerDetailsForBGDeployment;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import software.wings.WingsBaseTest;
import software.wings.sm.ExecutionContext;

import java.util.Arrays;
import java.util.List;

public class SpotinstStateHelperTest extends WingsBaseTest {
  @Inject SpotInstStateHelper spotInstStateHelper;

  @Test
  @Category(UnitTests.class)
  public void testAddLoadBalancerConfigAfterExpressionEvaluation() throws Exception {
    ExecutionContext context = mock(ExecutionContext.class);
    when(context.renderExpression(anyString())).thenAnswer(new Answer<String>() {
      @Override
      public String answer(InvocationOnMock invocation) throws Throwable {
        Object[] args = invocation.getArguments();
        return (String) args[0];
      }
    });

    List<LoadBalancerDetailsForBGDeployment> lbDetails =
        spotInstStateHelper.addLoadBalancerConfigAfterExpressionEvaluation(
            Arrays.asList(LoadBalancerDetailsForBGDeployment.builder()
                              .loadBalancerName("LB1")
                              .prodListenerPort("8080")
                              .stageListenerPort("80")
                              .build(),
                LoadBalancerDetailsForBGDeployment.builder()
                    .loadBalancerName("LB1")
                    .prodListenerPort("8080")
                    .stageListenerPort("80")
                    .build(),
                LoadBalancerDetailsForBGDeployment.builder()
                    .loadBalancerName("LB2")
                    .prodListenerPort("8080")
                    .stageListenerPort("80")
                    .build()),
            context);

    assertThat(lbDetails.size()).isEqualTo(2);
  }
}
