package io.harness;

import io.harness.rule.OwnerRule;
import io.harness.rule.OwnerRule.Owner;
import lombok.extern.slf4j.Slf4j;
import org.junit.runner.Description;
import org.junit.runner.notification.RunListener;

@Slf4j
public class OwnerTestListener extends RunListener {
  @Override
  public void testIgnored(Description description) throws Exception {
    final Owner owner = description.getAnnotation(Owner.class);
    if (owner == null) {
      return;
    }

    OwnerRule.checkForJira(description.getDisplayName(), owner.developers()[0]);

    for (String developer : owner.developers()) {
      OwnerRule.fileOwnerAs(developer, "ignore");
    }
  }
}