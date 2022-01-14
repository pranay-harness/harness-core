package io.harness.ccm.commons.beans.billing;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.OwnedBy;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@OwnedBy(CE)
public class CEBucketPolicyJson {
  String Version;
  List<CEBucketPolicyStatement> Statement;
}
