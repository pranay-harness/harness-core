package io.harness.commandlibrary.server;

import io.harness.CategoryTest;
import org.junit.Rule;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

public abstract class CommandLibraryServerBaseTest extends CategoryTest {
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();
  @Rule public CommandLibraryServerTestRule wingsRule = new CommandLibraryServerTestRule();
}
