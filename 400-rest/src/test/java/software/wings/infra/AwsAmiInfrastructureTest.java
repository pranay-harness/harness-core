/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.infra;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.rule.OwnerRule.GEORGE;
import static io.harness.rule.OwnerRule.PRASHANT;

import static software.wings.infra.InfraDefinitionTestConstants.CLASSIC_LOAD_BALANCERS;
import static software.wings.infra.InfraDefinitionTestConstants.HOSTNAME_CONVENTION;
import static software.wings.infra.InfraDefinitionTestConstants.REGION;
import static software.wings.infra.InfraDefinitionTestConstants.STAGE_CLASSIC_LOAD_BALANCERS;
import static software.wings.infra.InfraDefinitionTestConstants.STAGE_TARGET_GROUP_ARNS;
import static software.wings.infra.InfraDefinitionTestConstants.TARGET_GROUP_ARNS;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import software.wings.beans.AwsAmiInfrastructureMapping;
import software.wings.beans.InfrastructureMapping;
import software.wings.utils.WingsTestConstants;

import java.util.Map;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(CDP)
public class AwsAmiInfrastructureTest extends CategoryTest {
  private final AwsAmiInfrastructure awsAmiInfrastructure =
      AwsAmiInfrastructure.builder()
          .autoScalingGroupName(WingsTestConstants.AUTO_SCALING_GROUP_NAME)
          .classicLoadBalancers(CLASSIC_LOAD_BALANCERS)
          .cloudProviderId(WingsTestConstants.COMPUTE_PROVIDER_ID)
          .hostNameConvention(HOSTNAME_CONVENTION)
          .region(REGION)
          .stageClassicLoadBalancers(STAGE_CLASSIC_LOAD_BALANCERS)
          .stageTargetGroupArns(STAGE_TARGET_GROUP_ARNS)
          .targetGroupArns(TARGET_GROUP_ARNS)
          .build();

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void testGetInfraMapping() {
    InfrastructureMapping infrastructureMapping = awsAmiInfrastructure.getInfraMapping();

    assertThat(AwsAmiInfrastructureMapping.class).isEqualTo(infrastructureMapping.getClass());

    AwsAmiInfrastructureMapping infraMapping = (AwsAmiInfrastructureMapping) infrastructureMapping;

    assertThat(REGION).isEqualTo(infraMapping.getRegion());
    assertThat(CLASSIC_LOAD_BALANCERS).isEqualTo(infraMapping.getClassicLoadBalancers());
    assertThat(HOSTNAME_CONVENTION).isEqualTo(infraMapping.getHostNameConvention());
    assertThat(WingsTestConstants.AUTO_SCALING_GROUP_NAME).isEqualTo(infraMapping.getAutoScalingGroupName());
    assertThat(WingsTestConstants.COMPUTE_PROVIDER_ID).isEqualTo(infraMapping.getComputeProviderSettingId());
    assertThat(STAGE_CLASSIC_LOAD_BALANCERS).isEqualTo(infraMapping.getStageClassicLoadBalancers());
    assertThat(TARGET_GROUP_ARNS).isEqualTo(infraMapping.getTargetGroupArns());
    assertThat(STAGE_TARGET_GROUP_ARNS).isEqualTo(infraMapping.getStageTargetGroupArns());
    assertThat(REGION).isEqualTo(infraMapping.getRegion());
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void testGetMappingClass() {
    Class<? extends InfrastructureMapping> mappingClass = awsAmiInfrastructure.getMappingClass();

    assertThat(mappingClass).isNotNull();
    assertThat(AwsAmiInfrastructureMapping.class).isEqualTo(mappingClass);
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void testGetFieldMapForClass() {
    Map<String, Object> fieldMap = awsAmiInfrastructure.getFieldMapForClass();
    assertThat(fieldMap).isNotNull();

    assertThat(fieldMap.containsKey("cloudProviderId")).isFalse();

    assertThat(fieldMap.containsKey("region")).isTrue();
    assertThat(REGION).isEqualTo(fieldMap.get("region"));

    assertThat(fieldMap.containsKey("hostNameConvention")).isFalse();

    assertThat(fieldMap.containsKey("autoScalingGroupName")).isFalse();

    assertThat(fieldMap.containsKey("classicLoadBalancers")).isFalse();

    assertThat(fieldMap.containsKey("stageClassicLoadBalancers")).isFalse();

    assertThat(fieldMap.containsKey("targetGroupArns")).isFalse();

    assertThat(fieldMap.containsKey("stageTargetGroupArns")).isFalse();
  }
}
