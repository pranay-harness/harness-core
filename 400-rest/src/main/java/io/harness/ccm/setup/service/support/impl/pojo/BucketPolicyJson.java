package io.harness.ccm.setup.service.support.impl.pojo;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class BucketPolicyJson {
  String Version;
  List<BucketPolicyStatement> Statement;
}
