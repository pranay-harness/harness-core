/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.utils;

import static io.harness.rule.OwnerRule.ADWAIT;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import org.junit.Test;
import org.junit.experimental.categories.Category;

public class EcsConventionTest extends CategoryTest {
  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testGetTaskFamily() {
    String asgNamePrefix = EcsConvention.getTaskFamily("appName", "serviceName", "envName");
    assertThat(asgNamePrefix).isEqualTo("appName__serviceName__envName");

    asgNamePrefix = EcsConvention.getTaskFamily("app&Name", "service+Name", "env*Name");
    assertThat(asgNamePrefix).isEqualTo("app__Name__service__Name__env__Name");

    asgNamePrefix = EcsConvention.getTaskFamily("app/Name", "service.Name", "env'Name");
    assertThat(asgNamePrefix).isEqualTo("app__Name__service__Name__env__Name");

    asgNamePrefix = EcsConvention.getTaskFamily("app$Name", "service Name", "env\"Name");
    assertThat(asgNamePrefix).isEqualTo("app__Name__service__Name__env__Name");

    asgNamePrefix = EcsConvention.getTaskFamily("app$Name", "service|Name", "env\\Name");
    assertThat(asgNamePrefix).isEqualTo("app__Name__service__Name__env__Name");

    asgNamePrefix = EcsConvention.getTaskFamily("appName", null, null);
    assertThat(asgNamePrefix).isEqualTo("appName__null__null");
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testGetServiceNamePrefixFromServiceName() {
    String serviceNamePrefix = EcsConvention.getServiceNamePrefixFromServiceName("abc__test__123");
    assertThat(serviceNamePrefix).isEqualTo("abc__test__");

    serviceNamePrefix = EcsConvention.getServiceNamePrefixFromServiceName("abc__test__service__123");
    assertThat(serviceNamePrefix).isEqualTo("abc__test__service__");

    serviceNamePrefix = EcsConvention.getServiceNamePrefixFromServiceName("abc__test__service__test");
    assertThat(serviceNamePrefix).isEqualTo("abc__test__service__test");
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testGetContainerName() {
    String containerName = EcsConvention.getContainerName("container");
    assertThat(containerName).isEqualTo("container");

    containerName = EcsConvention.getContainerName("contai+ner");
    assertThat(containerName).isEqualTo("contai_ner");

    containerName = EcsConvention.getContainerName("contai*ner");
    assertThat(containerName).isEqualTo("contai_ner");

    containerName = EcsConvention.getContainerName("contai/ner");
    assertThat(containerName).isEqualTo("contai_ner");

    containerName = EcsConvention.getContainerName("contai$ner");
    assertThat(containerName).isEqualTo("contai_ner");

    containerName = EcsConvention.getContainerName("contai&ner");
    assertThat(containerName).isEqualTo("contai_ner");

    containerName = EcsConvention.getContainerName("contai\\ner");
    assertThat(containerName).isEqualTo("contai_ner");

    containerName = EcsConvention.getContainerName("contai\"ner");
    assertThat(containerName).isEqualTo("contai_ner");

    containerName = EcsConvention.getContainerName("contai'ner");
    assertThat(containerName).isEqualTo("contai_ner");

    containerName = EcsConvention.getContainerName("contai:ner");
    assertThat(containerName).isEqualTo("contai_ner");

    containerName = EcsConvention.getContainerName("contai.ner");
    assertThat(containerName).isEqualTo("contai_ner");

    containerName = EcsConvention.getContainerName("contai|ner");
    assertThat(containerName).isEqualTo("contai_ner");
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testGetVolumeName() {
    String volumePath = EcsConvention.getVolumeName("path");
    assertThat(volumePath).isEqualTo("vol_path_vol");

    volumePath = EcsConvention.getVolumeName("pa+th");
    assertThat(volumePath).isEqualTo("vol_pa__th_vol");

    volumePath = EcsConvention.getVolumeName("pa/th");
    assertThat(volumePath).isEqualTo("vol_pa__th_vol");

    volumePath = EcsConvention.getVolumeName("pa*th");
    assertThat(volumePath).isEqualTo("vol_pa__th_vol");

    volumePath = EcsConvention.getVolumeName("pa$th");
    assertThat(volumePath).isEqualTo("vol_pa__th_vol");

    volumePath = EcsConvention.getVolumeName("pa&th");
    assertThat(volumePath).isEqualTo("vol_pa__th_vol");

    volumePath = EcsConvention.getVolumeName("pa|th");
    assertThat(volumePath).isEqualTo("vol_pa__th_vol");

    volumePath = EcsConvention.getVolumeName("pa.th");
    assertThat(volumePath).isEqualTo("vol_pa__th_vol");

    volumePath = EcsConvention.getVolumeName("pa:th");
    assertThat(volumePath).isEqualTo("vol_pa__th_vol");

    volumePath = EcsConvention.getVolumeName("pa\"th");
    assertThat(volumePath).isEqualTo("vol_pa__th_vol");

    volumePath = EcsConvention.getVolumeName("pa'th");
    assertThat(volumePath).isEqualTo("vol_pa__th_vol");

    volumePath = EcsConvention.getVolumeName("pa\\th");
    assertThat(volumePath).isEqualTo("vol_pa__th_vol");
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testGetRevisionFromServiceName() {
    int revision = EcsConvention.getRevisionFromServiceName("aaa__123");
    assertThat(revision).isEqualTo(123);

    revision = EcsConvention.getRevisionFromServiceName("aaa__bbb__ccc__123");
    assertThat(revision).isEqualTo(123);

    revision = EcsConvention.getRevisionFromServiceName("aaabbbccc");
    assertThat(revision).isEqualTo(-1);

    // case where after last __, no number is mentioned.
    revision = EcsConvention.getRevisionFromServiceName("aaa__bbb__ccc");
    assertThat(revision).isEqualTo(-1);

    revision = EcsConvention.getRevisionFromServiceName(null);
    assertThat(revision).isEqualTo(-1);
  }
}
