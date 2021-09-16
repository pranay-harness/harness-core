package io.harness.cdng.provision.terraform;

import static io.harness.rule.OwnerRule.HANTANG;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(HarnessTeam.CDP)
public class MyTest {
  @Test
  @Owner(developers = HANTANG)
  @Category(UnitTests.class)
  public void testJsonCreator() throws JsonProcessingException {
    ObjectMapper mapper = new ObjectMapper();
    String str = "{\"name\":\"Prakhar\", \"terraformPlanCommand\":\"Apply\"}";
    PojoNewClass testPOJO = mapper.readValue(str, PojoNewClass.class);
    int a1 = 100;
  }
}
