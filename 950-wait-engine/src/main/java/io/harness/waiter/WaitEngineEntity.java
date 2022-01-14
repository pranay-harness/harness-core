package io.harness.waiter;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UuidAccess;

@OwnedBy(HarnessTeam.DEL)
public interface WaitEngineEntity extends PersistentEntity, UuidAccess {}
