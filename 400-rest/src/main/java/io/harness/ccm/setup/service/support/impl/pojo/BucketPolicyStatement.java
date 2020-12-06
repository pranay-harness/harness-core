package io.harness.ccm.setup.service.support.impl.pojo;

import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BucketPolicyStatement {
  String Sid;
  String Effect;
  Map<String, List<String>> Principal;
  Object Action;
  Object Resource;
  Map<String, Map<String, String>> Condition;
}
