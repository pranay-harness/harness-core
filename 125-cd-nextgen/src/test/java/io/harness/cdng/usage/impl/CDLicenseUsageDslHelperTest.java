/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.usage.impl;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.dtos.InstanceDTO;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.jooq.Record3;
import org.jooq.Row3;
import org.jooq.Table;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(HarnessTeam.CDP)
public class CDLicenseUsageDslHelperTest {
  private static final String accountIdentifier = "ACCOUNT_ID";
  private static final String orgIdentifier = "ORG_ID";
  private static final String projectIdentifier = "PROJECT_ID";
  private static final String instanceKey = "INSTANCE";
  private static final String serviceIdentifier = "SERVICE";
  private static final String envIdentifier = "ENV_ID";
  private CDLicenseUsageDslHelper cdLicenseUsageDslHelper = new CDLicenseUsageDslHelper();

  @Test
  @Owner(developers = OwnerRule.TATHAGAT)
  @Category(UnitTests.class)
  public void testGetOrgProjectServiceRows() {
    List<InstanceDTO> testInstanceDTOData = createTestInstanceDTOData(3);
    testInstanceDTOData.add(InstanceDTO.builder()
                                .instanceKey(instanceKey + 3)
                                .accountIdentifier(accountIdentifier + 2)
                                .projectIdentifier(projectIdentifier + 2)
                                .orgIdentifier(orgIdentifier + 2)
                                .envIdentifier(envIdentifier + 3)
                                .serviceIdentifier(serviceIdentifier + 2)
                                .build());
    Row3<String, String, String>[] orgProjectServiceRows =
        cdLicenseUsageDslHelper.getOrgProjectServiceRows(testInstanceDTOData);
    assertThat(orgProjectServiceRows).hasSize(3);
    assertThat(Arrays.stream(orgProjectServiceRows).findFirst().get().eq("ORG_ID1", "PROJECT_ID1", "SERVICE1"))
        .isNotNull();
  }

  @Test
  @Owner(developers = OwnerRule.TATHAGAT)
  @Category(UnitTests.class)
  public void testGetOrgProjectServiceTableFromInstances() {
    List<InstanceDTO> testInstanceDTOData = Collections.emptyList();
    Table<Record3<String, String, String>> orgProjectServiceTableFromInstances =
        cdLicenseUsageDslHelper.getOrgProjectServiceTableFromInstances(testInstanceDTOData);
    assertThat(orgProjectServiceTableFromInstances).isNull();
  }

  @Test
  @Owner(developers = OwnerRule.TATHAGAT)
  @Category(UnitTests.class)
  public void testGetOrgProjectServiceTableFromInstancesEmptyTable() {
    List<InstanceDTO> testInstanceDTOData = createTestInstanceDTOData(3);
    Table<Record3<String, String, String>> orgProjectServiceTableFromInstances =
        cdLicenseUsageDslHelper.getOrgProjectServiceTableFromInstances(testInstanceDTOData);
    assertThat(orgProjectServiceTableFromInstances).isNotNull();
  }

  List<InstanceDTO> createTestInstanceDTOData(int dataSize) {
    List<InstanceDTO> instanceDTOList = new ArrayList<>();
    for (int i = 0; i < dataSize; i++) {
      instanceDTOList.add(InstanceDTO.builder()
                              .instanceKey(instanceKey + i)
                              .accountIdentifier(accountIdentifier + i)
                              .projectIdentifier(projectIdentifier + i)
                              .orgIdentifier(orgIdentifier + i)
                              .envIdentifier(envIdentifier + i)
                              .serviceIdentifier(serviceIdentifier + i)
                              .build());
    }
    return instanceDTOList;
  }
}
