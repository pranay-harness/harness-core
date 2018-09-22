package software.wings.utils;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class EcsConventionTest {
  @Test
  public void testGetTaskFamily() {
    String asgNamePrefix = EcsConvention.getTaskFamily("appName", "serviceName", "envName");
    assertEquals("appName__serviceName__envName", asgNamePrefix);

    asgNamePrefix = EcsConvention.getTaskFamily("app&Name", "service+Name", "env*Name");
    assertEquals("app__Name__service__Name__env__Name", asgNamePrefix);

    asgNamePrefix = EcsConvention.getTaskFamily("app/Name", "service.Name", "env'Name");
    assertEquals("app__Name__service__Name__env__Name", asgNamePrefix);

    asgNamePrefix = EcsConvention.getTaskFamily("app$Name", "service Name", "env\"Name");
    assertEquals("app__Name__service__Name__env__Name", asgNamePrefix);

    asgNamePrefix = EcsConvention.getTaskFamily("app$Name", "service|Name", "env\\Name");
    assertEquals("app__Name__service__Name__env__Name", asgNamePrefix);

    asgNamePrefix = EcsConvention.getTaskFamily("appName", null, null);
    assertEquals("appName__null__null", asgNamePrefix);
  }

  @Test
  public void testGetServiceNamePrefixFromServiceName() {
    String serviceNamePrefix = EcsConvention.getServiceNamePrefixFromServiceName("abc__test__123");
    assertEquals("abc__test__", serviceNamePrefix);

    serviceNamePrefix = EcsConvention.getServiceNamePrefixFromServiceName("abc__test__service__123");
    assertEquals("abc__test__service__", serviceNamePrefix);
  }

  @Test
  public void testGetContainerName() {
    String containerName = EcsConvention.getContainerName("container");
    assertEquals("container", containerName);

    containerName = EcsConvention.getContainerName("contai+ner");
    assertEquals("contai_ner", containerName);

    containerName = EcsConvention.getContainerName("contai*ner");
    assertEquals("contai_ner", containerName);

    containerName = EcsConvention.getContainerName("contai/ner");
    assertEquals("contai_ner", containerName);

    containerName = EcsConvention.getContainerName("contai$ner");
    assertEquals("contai_ner", containerName);

    containerName = EcsConvention.getContainerName("contai&ner");
    assertEquals("contai_ner", containerName);

    containerName = EcsConvention.getContainerName("contai\\ner");
    assertEquals("contai_ner", containerName);

    containerName = EcsConvention.getContainerName("contai\"ner");
    assertEquals("contai_ner", containerName);

    containerName = EcsConvention.getContainerName("contai'ner");
    assertEquals("contai_ner", containerName);

    containerName = EcsConvention.getContainerName("contai:ner");
    assertEquals("contai_ner", containerName);

    containerName = EcsConvention.getContainerName("contai.ner");
    assertEquals("contai_ner", containerName);

    containerName = EcsConvention.getContainerName("contai|ner");
    assertEquals("contai_ner", containerName);
  }

  @Test
  public void testGetVolumeName() {
    String volumePath = EcsConvention.getVolumeName("path");
    assertEquals("vol_path_vol", volumePath);

    volumePath = EcsConvention.getVolumeName("pa+th");
    assertEquals("vol_pa__th_vol", volumePath);

    volumePath = EcsConvention.getVolumeName("pa/th");
    assertEquals("vol_pa__th_vol", volumePath);

    volumePath = EcsConvention.getVolumeName("pa*th");
    assertEquals("vol_pa__th_vol", volumePath);

    volumePath = EcsConvention.getVolumeName("pa$th");
    assertEquals("vol_pa__th_vol", volumePath);

    volumePath = EcsConvention.getVolumeName("pa&th");
    assertEquals("vol_pa__th_vol", volumePath);

    volumePath = EcsConvention.getVolumeName("pa|th");
    assertEquals("vol_pa__th_vol", volumePath);

    volumePath = EcsConvention.getVolumeName("pa.th");
    assertEquals("vol_pa__th_vol", volumePath);

    volumePath = EcsConvention.getVolumeName("pa:th");
    assertEquals("vol_pa__th_vol", volumePath);

    volumePath = EcsConvention.getVolumeName("pa\"th");
    assertEquals("vol_pa__th_vol", volumePath);

    volumePath = EcsConvention.getVolumeName("pa'th");
    assertEquals("vol_pa__th_vol", volumePath);

    volumePath = EcsConvention.getVolumeName("pa\\th");
    assertEquals("vol_pa__th_vol", volumePath);
  }

  @Test
  public void testGetRevisionFromServiceName() {
    int revision = EcsConvention.getRevisionFromServiceName("aaa__123");
    assertEquals(123, revision);

    revision = EcsConvention.getRevisionFromServiceName("aaa__bbb__ccc__123");
    assertEquals(123, revision);

    revision = EcsConvention.getRevisionFromServiceName("aaabbbccc");
    assertEquals(-1, revision);

    // case where after last __, no number is mentioned.
    revision = EcsConvention.getRevisionFromServiceName("aaa__bbb__ccc");
    assertEquals(-1, revision);

    revision = EcsConvention.getRevisionFromServiceName(null);
    assertEquals("null string as input doesn't work", -1, revision);
  }
}
