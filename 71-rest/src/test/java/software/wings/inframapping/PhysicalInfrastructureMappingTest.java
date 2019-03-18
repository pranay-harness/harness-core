package software.wings.inframapping;

import static org.junit.Assert.assertEquals;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.WingsBaseTest;
import software.wings.beans.PhysicalInfrastructureMapping;
import software.wings.beans.PhysicalInfrastructureMapping.Builder;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PhysicalInfrastructureMappingTest extends WingsBaseTest {
  @Test
  @Category(UnitTests.class)
  public void testApplyProvisionerVariables() {
    PhysicalInfrastructureMapping infrastructureMapping = Builder.aPhysicalInfrastructureMapping().build();

    HashMap<String, Object> blueprintProperties = Maps.newHashMap();

    Map<String, Object> host1 = Maps.newLinkedHashMap();
    host1.put("Hostname", "abc.com");
    host1.put("amiId", 123);

    Map<String, Object> host2 = Maps.newLinkedHashMap();
    host2.put("Hostname", "abcd.com");
    host2.put("amiId", 1234);

    List<Map<String, Object>> hosts = Lists.newArrayList(host1, host2);
    blueprintProperties.put("hostArrayPath", hosts);

    infrastructureMapping.applyProvisionerVariables(blueprintProperties, null);

    assertEquals(2, infrastructureMapping.hosts().size());
    assertEquals("abc.com", infrastructureMapping.hosts().get(0).getPublicDns());
    assertEquals(123, infrastructureMapping.hosts().get(0).getProperties().get("amiId"));
    assertEquals("abcd.com", infrastructureMapping.hosts().get(1).getPublicDns());
    assertEquals(1234, infrastructureMapping.hosts().get(1).getProperties().get("amiId"));

    Assertions.assertThatExceptionOfType(InvalidRequestException.class)
        .isThrownBy(() -> infrastructureMapping.applyProvisionerVariables(null, null));

    Assertions.assertThatExceptionOfType(InvalidRequestException.class)
        .isThrownBy(() -> infrastructureMapping.applyProvisionerVariables(Collections.EMPTY_MAP, null));

    Map<String, Object> host3 = Maps.newLinkedHashMap();
    host2.put("amiId", 1234);
    hosts.add(host3);

    Assertions.assertThatExceptionOfType(InvalidRequestException.class)
        .isThrownBy(() -> infrastructureMapping.applyProvisionerVariables(Maps.newHashMap(), null));
  }
}
