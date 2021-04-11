package io.harness.gitsync.impl;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.entity.HarnessShortListedBranch;
import io.harness.gitsync.service.HarnessShortlistedBranchService;
import io.harness.repositories.shortlistedbranch.HarnessShortListedBranchRepository;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.AllArgsConstructor;

@Singleton
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@OwnedBy(DX)
public class HarnessShortlistedBranchServiceImpl implements HarnessShortlistedBranchService {
  HarnessShortListedBranchRepository harnessShortListedBranchRepository;

  @Override
  public HarnessShortListedBranch save(HarnessShortListedBranch shortlistedBranch) {
    return harnessShortListedBranchRepository.save(shortlistedBranch);
  }
}
