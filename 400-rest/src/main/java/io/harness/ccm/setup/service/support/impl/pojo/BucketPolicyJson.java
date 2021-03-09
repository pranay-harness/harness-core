package io.harness.ccm.setup.service.support.impl.pojo;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class BucketPolicyJson {
  String Version;
  List<BucketPolicyStatement> Statement;
}
