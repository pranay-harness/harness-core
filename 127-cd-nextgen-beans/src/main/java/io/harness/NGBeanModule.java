package io.harness;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import com.google.inject.AbstractModule;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(HarnessTeam.CDP)
@Slf4j
public class NGBeanModule extends AbstractModule {
  private static NGBeanModule instance;

  public static NGBeanModule getInstance() {
    if (instance == null) {
      instance = new NGBeanModule();
    }
    return instance;
  }

  @Override
  protected void configure() {}
}
