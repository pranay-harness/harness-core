package software.wings.sm.states;

import static io.harness.rule.OwnerRule.SATYAM;
import static io.harness.rule.OwnerRule.TMACARI;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.spy;

import com.google.common.collect.ImmutableMap;

import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.WingsBaseTest;
import software.wings.api.AmiServiceSetupElement;

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
  public void testGetTimeoutFromContext() {
    AwsStateHelper helper = new AwsStateHelper();

    AmiServiceSetupElement setupElement = AmiServiceSetupElement.builder().autoScalingSteadyStateTimeout(10).build();
    assertThat(helper.getAmiStateTimeout(setupElement)).isEqualTo(600000);

    setupElement = AmiServiceSetupElement.builder().autoScalingSteadyStateTimeout(0).build();
    assertThat(helper.getAmiStateTimeout(setupElement)).isEqualTo(null);

    setupElement = AmiServiceSetupElement.builder().autoScalingSteadyStateTimeout(null).build();
    assertThat(helper.getAmiStateTimeout(setupElement)).isEqualTo(null);

    setupElement = AmiServiceSetupElement.builder().autoScalingSteadyStateTimeout(35792).build();
    assertThat(helper.getAmiStateTimeout(setupElement)).isEqualTo(null);
  }
}
