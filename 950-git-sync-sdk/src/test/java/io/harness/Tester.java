package io.harness;

import io.harness.repositories.TestRepository;

import com.google.inject.Inject;

public class Tester {
  @Inject TestRepository cdRepository;

  public void test() {
    cdRepository.findByIdd("123");
  }
}
