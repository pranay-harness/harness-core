package io.harness.store;

import io.harness.annotation.StoreIn;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.persistence.PersistentEntity;

@StoreIn("foo")
@OwnedBy(HarnessTeam.PL)
class TestPersistentEntity implements PersistentEntity {}
