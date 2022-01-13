/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.serializer.kryo;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.audit.beans.AuditEntry;
import io.harness.audit.beans.AuditEventDTO;
import io.harness.audit.beans.AuditEventData;
import io.harness.audit.beans.AuditSettingsDTO;
import io.harness.audit.beans.AuthenticationInfoDTO;
import io.harness.audit.beans.Principal;
import io.harness.audit.beans.ResourceDTO;
import io.harness.audit.beans.ResourceScopeDTO;
import io.harness.audit.beans.YamlDiffRecordDTO;
import io.harness.serializer.KryoRegistrar;

import com.esotericsoftware.kryo.Kryo;

@OwnedBy(PL)
public class NGAuditCommonsKryoRegistrar implements KryoRegistrar {
  @Override
  public void register(Kryo kryo) {
    kryo.register(AuditEventData.class, 17360);
    kryo.register(Principal.class, 17361);
    kryo.register(AuthenticationInfoDTO.class, 17362);
    kryo.register(AuditSettingsDTO.class, 17363);
    kryo.register(YamlDiffRecordDTO.class, 17364);
    kryo.register(AuditEventDTO.class, 17365);
    kryo.register(AuditEntry.class, 17366);
    kryo.register(ResourceScopeDTO.class, 17367);
    kryo.register(ResourceDTO.class, 17368);
  }
}
