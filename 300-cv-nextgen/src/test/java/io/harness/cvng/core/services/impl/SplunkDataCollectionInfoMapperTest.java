package io.harness.cvng.core.services.impl;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.KAMAL;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CvNextGenTest;
import io.harness.category.element.UnitTests;
import io.harness.cvng.beans.SplunkDataCollectionInfo;
import io.harness.cvng.core.entities.SplunkCVConfig;
import io.harness.cvng.models.VerificationType;
import io.harness.rule.Owner;

import com.google.inject.Inject;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class SplunkDataCollectionInfoMapperTest extends CvNextGenTest {
  @Inject private SplunkDataCollectionInfoMapper mapper;

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testToDataConnectionInfo() {
    SplunkCVConfig cvConfig = new SplunkCVConfig();
    cvConfig.setUuid(generateUuid());
    cvConfig.setAccountId(generateUuid());
    cvConfig.setQuery("exception");
    cvConfig.setServiceInstanceIdentifier("host");
    SplunkDataCollectionInfo splunkDataCollectionInfo = mapper.toDataCollectionInfo(cvConfig);
    assertThat(splunkDataCollectionInfo.getQuery()).isEqualTo(cvConfig.getQuery());
    assertThat(splunkDataCollectionInfo.getServiceInstanceIdentifier())
        .isEqualTo(cvConfig.getServiceInstanceIdentifier());
    assertThat(splunkDataCollectionInfo.getVerificationType()).isEqualTo(VerificationType.LOG);
    assertThat(splunkDataCollectionInfo.getDataCollectionDsl()).isEqualTo(cvConfig.getDataCollectionDsl());
  }
}
