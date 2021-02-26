package io.harness;

import io.harness.beans.SampleBean;
import io.harness.repositories.TestRepository;

import com.google.inject.Inject;

public class Tester {
  @Inject TestRepository cdRepository;

  public void test() {
    cdRepository.findByIdd("123");
  }

  public SampleBean save(String stringToSave) {
    return cdRepository.save(SampleBean.builder().test1(stringToSave).build());
  }
}
