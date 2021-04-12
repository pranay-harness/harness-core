package io.harness.repositories.shortlistedbranch;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotation.HarnessRepo;
import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.entity.HarnessShortListedBranch;

import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.transaction.annotation.Transactional;

@HarnessRepo
@Transactional
@OwnedBy(DX)
public interface HarnessShortListedBranchRepository
    extends PagingAndSortingRepository<HarnessShortListedBranch, String> {}
