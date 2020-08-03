package software.wings.sm.states;

import static io.harness.rule.OwnerRule.SATYAM;
import static io.harness.rule.OwnerRule.TMACARI;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import com.google.common.collect.ImmutableMap;

import io.harness.category.element.UnitTests;
import io.harness.context.ContextElementType;
import io.harness.exception.InvalidRequestException;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.WingsBaseTest;
import software.wings.api.AmiServiceSetupElement;
import software.wings.sm.ExecutionContextImpl;

import java.util.Map;

public class AwsStateHelperTest extends WingsBaseTest {
  @Test
  @Owner(developers = SATYAM)
  @Category(UnitTests.class)
  public void testFetchRequiredAsgCapacity() {
    AwsStateHelper helper = spy(AwsStateHelper.class);
    Map<String, Integer> map = ImmutableMap.of("foo__1", 1, "foo__2", 1);
    assertThatThrownBy(() -> helper.fetchRequiredAsgCapacity(map, "foo__3"))
        .isInstanceOf(InvalidRequestException.class);
    assertThat(helper.fetchRequiredAsgCapacity(map, "foo__1")).isEqualTo(1);
  }

  @Test
  @Owner(developers = TMACARI)
  @Category(UnitTests.class)
  public void testGetTimeoutMillis() {
    AwsStateHelper helper = new AwsStateHelper();

    ExecutionContextImpl mockContext = mock(ExecutionContextImpl.class);
    doReturn(AmiServiceSetupElement.builder().autoScalingSteadyStateTimeout(10).build())
        .when(mockContext)
        .getContextElement(ContextElementType.AMI_SERVICE_SETUP);
    assertThat(helper.getAmiStateTimeoutFromContext(mockContext)).isEqualTo(600000);

    doReturn(AmiServiceSetupElement.builder().autoScalingSteadyStateTimeout(0).build())
        .when(mockContext)
        .getContextElement(ContextElementType.AMI_SERVICE_SETUP);
    assertThat(helper.getAmiStateTimeoutFromContext(mockContext)).isEqualTo(null);

    doReturn(AmiServiceSetupElement.builder().autoScalingSteadyStateTimeout(null).build())
        .when(mockContext)
        .getContextElement(ContextElementType.AMI_SERVICE_SETUP);
    assertThat(helper.getAmiStateTimeoutFromContext(mockContext)).isEqualTo(null);

    doReturn(AmiServiceSetupElement.builder().autoScalingSteadyStateTimeout(35792).build())
        .when(mockContext)
        .getContextElement(ContextElementType.AMI_SERVICE_SETUP);
    assertThat(helper.getAmiStateTimeoutFromContext(mockContext)).isEqualTo(null);
  }
}