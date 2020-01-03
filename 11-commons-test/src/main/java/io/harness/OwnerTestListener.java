package io.harness;

import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;
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

    if (System.getenv("SONAR_TOKEN") != null) {
      OwnerRule.checkForJira(description.getDisplayName(), owner.developers()[0], OwnerRule.PRIORITY_VALUE1);
    }

    for (String developer : owner.developers()) {
      OwnerRule.fileOwnerAs(developer, "ignore");
    }
  }
}