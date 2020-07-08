package io.harness.ccm.setup.service.support.impl.pojo;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

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
