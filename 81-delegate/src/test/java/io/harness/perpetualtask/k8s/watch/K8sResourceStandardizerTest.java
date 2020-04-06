package io.harness.perpetualtask.k8s.watch;

import static io.harness.rule.OwnerRule.AVMOHAN;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableMap;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import lombok.val;
import org.assertj.core.api.SoftAssertions;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class K8sResourceStandardizerTest extends CategoryTest {
  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void testCpuNano() throws Exception {
    final ImmutableMap<String, Long> map = ImmutableMap.<String, Long>builder()
                                               .put(".1", 100000000L)
                                               .put("0.1", 100000000L)
                                               .put("1", 1000000000L)
                                               .put("1.5", 1500000000L)
                                               .put("25m", 25000000L)
                                               .put("746640510n", 746640510L)

                                               .put("0", 0L)
                                               .build();
    SoftAssertions.assertSoftly(softly -> {
      for (val e : map.entrySet()) {
        softly.assertThat(K8sResourceStandardizer.getCpuNano(e.getKey()))
            .as("getCpuNano(%s) == %s", e.getKey(), e.getValue())
            .isEqualTo(e.getValue());
      }
    });
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void testMemByte() throws Exception {
    final ImmutableMap<String, Long> map = ImmutableMap.<String, Long>builder()
                                               .put("1", 1L)
                                               .put("1k", 1_000L)
                                               .put("1M", 1_000_000L)
                                               .put("1G", 1_000_000_000L)
                                               .put("1T", 1_000_000_000_000L)
                                               .put("1P", 1_000_000_000_000_000L)

                                               .put("1Ki", 1024L)
                                               .put("1Mi", 1048576L)
                                               .put("1Gi", 1073741824L)
                                               .put("1Ti", 1099511627776L)
                                               .put("1Pi", 1125899906842624L)

                                               .put("0", 0L)
                                               .build();
    SoftAssertions.assertSoftly(softly -> {
      for (val e : map.entrySet()) {
        softly.assertThat(K8sResourceStandardizer.getMemoryByte(e.getKey()))
            .as("getMemoryByte(%s) == %s", e.getKey(), e.getValue())
            .isEqualTo(e.getValue());
      }
    });
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void testCpuFractionalNano() throws Exception {
    assertThat(K8sResourceStandardizer.getCpuNano("111.2n")).isEqualTo(111);
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void testMemByteFractionalByte() throws Exception {
    assertThat(K8sResourceStandardizer.getMemoryByte("1123m")).isEqualTo(1);
  }
}
