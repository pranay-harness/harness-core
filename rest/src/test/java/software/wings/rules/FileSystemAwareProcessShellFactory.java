package software.wings.rules;

import static java.util.Arrays.asList;

import org.apache.sshd.common.util.GenericUtils;
import org.apache.sshd.server.Command;
import org.apache.sshd.server.shell.InvertedShell;
import org.apache.sshd.server.shell.ProcessShellFactory;

import java.util.Collections;
import java.util.List;

/**
 * Created by peeyushaggarwal on 7/27/16.
 */
public class FileSystemAwareProcessShellFactory extends ProcessShellFactory {
  /**
   * Instantiates a new File system aware process shell factory.
   */
  public FileSystemAwareProcessShellFactory() {
    this(Collections.<String>emptyList());
  }

  /**
   * Instantiates a new File system aware process shell factory.
   *
   * @param command the command
   */
  public FileSystemAwareProcessShellFactory(String... command) {
    this(GenericUtils.isEmpty(command) ? Collections.<String>emptyList() : asList(command));
  }

  /**
   * Instantiates a new File system aware process shell factory.
   *
   * @param command the command
   */
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
