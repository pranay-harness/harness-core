package io.harness.rule;

import io.harness.rule.OwnerRule.Owner;
import lombok.extern.slf4j.Slf4j;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

@Slf4j
public class OwnerWatcherRule extends TestWatcher {
  @Override
  protected void failed(Throwable e, Description description) {
    final Owner owner = description.getAnnotation(Owner.class);
    if (owner == null) {
      return;
    }

    for (String developer : owner.developers()) {
      OwnerRule.fileOwnerAs(developer, "failed");
    }
  }
}