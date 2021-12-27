package io.harness.cdng;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.plancreator.steps.common.StepElementParameters;
import io.harness.pms.yaml.ParameterField;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static io.harness.rule.OwnerRule.ABOSII;
import static org.assertj.core.api.Assertions.assertThat;

public class TimeOutHelperTest extends CategoryTest {
    TimeOutHelper timeOutHelper;
    @Test
    @Owner(developers = ABOSII)
    @Category(UnitTests.class)
    public void testGetTimeoutValue() {
        StepElementParameters definedValue =
                StepElementParameters.builder().timeout(ParameterField.createValueField("15m")).build();
        StepElementParameters nullValue = StepElementParameters.builder().timeout(ParameterField.ofNull()).build();
        assertThat(timeOutHelper.getTimeoutValue(definedValue)).isEqualTo("15m");
        assertThat(timeOutHelper.getTimeoutValue(nullValue)).isEqualTo("10m");
    }

    @Test
    @Owner(developers = ABOSII)
    @Category(UnitTests.class)
    public void testGetTimeoutInMin() {
        StepElementParameters value =
                StepElementParameters.builder().timeout(ParameterField.createValueField("15m")).build();
        assertThat(timeOutHelper.getTimeoutInMin(value)).isEqualTo(15);
    }

    @Test
    @Owner(developers = ABOSII)
    @Category(UnitTests.class)
    public void testGetTimeoutInMillis() {
        StepElementParameters value =
                StepElementParameters.builder().timeout(ParameterField.createValueField("15m")).build();
        assertThat(timeOutHelper.getTimeoutInMillis(value)).isEqualTo(900000);
    }
}
