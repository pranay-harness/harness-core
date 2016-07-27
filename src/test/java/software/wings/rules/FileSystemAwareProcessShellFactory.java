package software.wings.rules;

import org.apache.sshd.common.util.GenericUtils;
import org.apache.sshd.server.Command;
import org.apache.sshd.server.shell.InvertedShell;
import org.apache.sshd.server.shell.ProcessShellFactory;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Created by peeyushaggarwal on 7/27/16.
 */
public class FileSystemAwareProcessShellFactory extends ProcessShellFactory {
  public FileSystemAwareProcessShellFactory() {
    this(Collections.<String>emptyList());
  }

  public FileSystemAwareProcessShellFactory(String... command) {
    this(GenericUtils.isEmpty(command) ? Collections.<String>emptyList() : Arrays.asList(command));
  }

  public FileSystemAwareProcessShellFactory(List<String> command) {
    super(command);
  }

  @Override
  public Command create() {
    return new FileSystemAwareInvertedShellWrapper(createInvertedShell());
  }

  @Override
  protected InvertedShell createInvertedShell() {
    return new FileSystemAwareProcessShell(resolveEffectiveCommand(getCommand()));
  }
}
