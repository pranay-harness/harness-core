package io.harness.ccm.billing;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import com.google.api.services.cloudresourcemanager.model.Binding;
import com.google.api.services.cloudresourcemanager.model.Policy;
import java.util.ArrayList;
import java.util.List;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@UtilityClass
@Slf4j
@TargetModule(Module._490_CE_COMMONS)
public class GcpResourceManagerUtils {
  // add a member to a pre-existing role or create fresh binding
  public void addBinding(Policy policy, String role, String member) {
    List<Binding> bindings = policy.getBindings();
    // Check if this role is already present in some existing binding. if yes, add this member in same binding.
    for (Binding b : bindings) {
      if (b.getRole().equals(role)) {
        b.getMembers().add(member);
        log.info("Member {} added to role {}", member, role);
        return;
      }
    }
    // At this point the binding needs to be freshly added
    List<String> members = new ArrayList<>();
    members.add(member);
    Binding binding = new Binding();
    binding.setRole(role);
    binding.setMembers(members);
    policy.getBindings().add(binding);

    log.error("Fresh binding was created. Member {} added to role {}", member, role);
  }
}
