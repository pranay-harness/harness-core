/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.rules;

import static java.util.Arrays.asList;

import java.util.Collections;
import java.util.List;
import org.apache.sshd.common.util.GenericUtils;
import org.apache.sshd.server.Command;
import org.apache.sshd.server.shell.InvertedShell;
import org.apache.sshd.server.shell.ProcessShellFactory;

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
