package io.harness.ccm.graphql.dto.demo;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@OwnedBy(CE)
public class InstanceDataDemo {
  String instancetype;
  String region;
  String cloudprovider;
}
