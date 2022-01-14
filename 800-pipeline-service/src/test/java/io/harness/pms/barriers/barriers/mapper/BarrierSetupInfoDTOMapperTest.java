package io.harness.pms.barriers.barriers.mapper;

import static io.harness.rule.OwnerRule.BRIJESH;

import static junit.framework.TestCase.assertEquals;

import io.harness.CategoryTest;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.pms.barriers.mapper.BarrierSetupInfoDTOMapper;
import io.harness.pms.barriers.response.BarrierSetupInfoDTO;
import io.harness.rule.Owner;
import io.harness.steps.barriers.beans.BarrierSetupInfo;

import java.util.HashSet;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(HarnessTeam.PIPELINE)
public class BarrierSetupInfoDTOMapperTest extends CategoryTest {
  @Test
  @Owner(developers = BRIJESH)
  @Category(UnitTests.class)
  public void testToBarrierSetupInfoDTO() {
    BarrierSetupInfo barrierSetupInfo =
        BarrierSetupInfo.builder().name("name").identifier("id").stages(new HashSet<>()).build();
    BarrierSetupInfoDTO barrierSetupInfoDTO = BarrierSetupInfoDTOMapper.toBarrierSetupInfoDTO.apply(barrierSetupInfo);
    assertEquals(barrierSetupInfoDTO.getIdentifier(), barrierSetupInfo.getIdentifier());
    assertEquals(barrierSetupInfoDTO.getName(), barrierSetupInfo.getName());
    assertEquals(barrierSetupInfoDTO.getStages().size(), 0);
  }
}
