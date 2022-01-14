package io.harness.ccm.setup.service.support.impl.pojo;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.OwnedBy;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@OwnedBy(CE)
public class BucketPolicyJson {
  String Version;
  List<BucketPolicyStatement> Statement;
}
