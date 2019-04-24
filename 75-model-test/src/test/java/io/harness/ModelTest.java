package io.harness;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.harness.rule.ModelRule;
import org.junit.Rule;

@SuppressFBWarnings("URF_UNREAD_FIELD")
public class ModelTest extends CategoryTest {
  @Rule private ModelRule rule = new ModelRule();
}