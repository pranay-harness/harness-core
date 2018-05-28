package io.harness;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.junit.Rule;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

@SuppressFBWarnings("URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
public class MockableTest extends CategoryTest {
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();
}
