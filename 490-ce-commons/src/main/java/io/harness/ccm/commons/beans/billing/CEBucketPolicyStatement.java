package io.harness.ccm.commons.beans.billing;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.OwnedBy;

import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@OwnedBy(CE)
public class CEBucketPolicyStatement {
  String Sid;
  String Effect;
  Map<String, List<String>> Principal;
  Object Action;
  Object Resource;
  Map<String, Map<String, String>> Condition;
}
