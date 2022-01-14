package io.harness.repositories.gittoharnessstatus;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.common.beans.GitToHarnessProgress;

import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Update;

@OwnedBy(DX)
public interface GitToHarnessProgressRepositoryCustom {
  GitToHarnessProgress findAndModify(Criteria criteria, Update update);
}
