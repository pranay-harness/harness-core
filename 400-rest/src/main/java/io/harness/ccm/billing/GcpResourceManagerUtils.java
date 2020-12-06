package io.harness.ccm.billing;

import com.google.api.services.cloudresourcemanager.model.Binding;
import com.google.api.services.cloudresourcemanager.model.Policy;
import java.util.List;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@UtilityClass
@Slf4j
public class GcpResourceManagerUtils {
  // add a member to a pre-existing role
  public void addBinding(Policy policy, String role, String member) {
    List<Binding> bindings = policy.getBindings();
    for (Binding b : bindings) {
      if (b.getRole().equals(role)) {
        b.getMembers().add(member);
        log.info("Member " + member + " added to role " + role);
        return;
      }
    }
    log.error("Role not found in policy; member not added");
  }
}
