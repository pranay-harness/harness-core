package io.harness.spotinst.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ElastiGroupInstanceDetails {
  private String instanceId;
  private String instanceType;
  private String product;
  private String groupId;
  private String availabilityZone;
  private String privateIp;
  private String publicIp;
}
