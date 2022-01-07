/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.ccm.serviceAccount;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.OwnedBy;

import com.google.api.services.cloudresourcemanager.model.Binding;
import com.google.api.services.cloudresourcemanager.model.Policy;
import java.util.ArrayList;
import java.util.List;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@UtilityClass
@Slf4j
@OwnedBy(CE)
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
