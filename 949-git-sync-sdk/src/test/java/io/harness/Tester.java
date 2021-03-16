package io.harness;

import io.harness.beans.SampleBean;
import io.harness.gitsync.ChangeSet;
import io.harness.gitsync.GitToHarnessServiceGrpc.GitToHarnessServiceBlockingStub;
import io.harness.repositories.TestRepository;

import com.google.inject.Inject;

public class Tester {
  @Inject TestRepository cdRepository;
  @Inject GitToHarnessServiceBlockingStub gitToHarnessServiceBlockingStub;

  public SampleBean save(String stringToSave) {
    gitToHarnessServiceBlockingStub.syncRequestFromGit(ChangeSet.newBuilder().build());
    return cdRepository.save(SampleBean.builder().test1(stringToSave).build());
  }
}
