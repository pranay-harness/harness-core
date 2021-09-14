/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.exceptionhandler.handler;

import static io.harness.exception.WingsException.USER;

import io.harness.exception.ExplanationException;
import io.harness.exception.HintException;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.NestedExceptionUtils;
import io.harness.exception.WingsException;
import io.harness.exception.exceptionmanager.exceptionhandler.ExceptionHandler;
import io.harness.exception.runtime.JGitRuntimeException;

import com.google.common.collect.ImmutableSet;
import com.jcraft.jsch.JSchException;
import java.net.UnknownHostException;
import java.util.Set;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.errors.NoRemoteRepositoryException;
import org.eclipse.jgit.errors.TransportException;

public class JGitExceptionHandler implements ExceptionHandler {
  public static Set<Class<? extends Exception>> exceptions() {
    return ImmutableSet.<Class<? extends Exception>>builder().add(JGitRuntimeException.class).build();
  }
  @Override
  public WingsException handleException(Exception exception) {
    JGitRuntimeException jGitRuntimeException = (JGitRuntimeException) exception;
    Throwable e = jGitRuntimeException.getCause();

    if (e instanceof InvalidRemoteException || e.getCause() instanceof NoRemoteRepositoryException) {
      return NestedExceptionUtils.hintWithExplanationException(HintException.HINT_INVALID_GIT_REPO,
          ExplanationException.INVALID_GIT_REPO, new InvalidRequestException(e.getMessage(), USER));
    }

    if (e instanceof org.eclipse.jgit.api.errors.TransportException) {
      org.eclipse.jgit.api.errors.TransportException te = (org.eclipse.jgit.api.errors.TransportException) e;
      Throwable cause = te.getCause();
      if (cause instanceof TransportException) {
        TransportException tee = (TransportException) cause;
        if (tee.getCause() instanceof UnknownHostException) {
          return NestedExceptionUtils.hintWithExplanationException(HintException.HINT_INVALID_GIT_HOST,
              ExplanationException.INVALID_GIT_REPO, new InvalidRequestException(e.getMessage(), USER));
        }
        if (tee.getCause() instanceof JSchException) {
          JSchException jSchException = (JSchException) cause.getCause();
          if (jSchException.getMessage().contains("invalid privatekey")) {
            return NestedExceptionUtils.hintWithExplanationException(HintException.HINT_MALFORMED_GIT_SSH_KEY,
                ExplanationException.MALFORMED_GIT_SSH_KEY, new InvalidRequestException(e.getMessage(), USER));
          }
          if (jSchException.getMessage().contains("Auth fail")) {
            return NestedExceptionUtils.hintWithExplanationException(HintException.HINT_INVALID_GIT_SSH_KEY,
                ExplanationException.INVALID_GIT_SSH_AUTHORIZATION, new InvalidRequestException(e.getMessage(), USER));
          }
        }
      }
      if (e.getMessage().contains("not authorized")) {
        return NestedExceptionUtils.hintWithExplanationException(HintException.HINT_INVALID_GIT_AUTHORIZATION,
            ExplanationException.INVALID_GIT_AUTHORIZATION, new InvalidRequestException(e.getMessage(), USER));
      }
      if (e.getMessage().contains("authentication not supported")) {
        return NestedExceptionUtils.hintWithExplanationException(HintException.HINT_INVALID_GIT_AUTHENTICATION,
            ExplanationException.INVALID_GIT_AUTHENTICATION, new InvalidRequestException(e.getMessage(), USER));
      }
    }
    return new InvalidRequestException(e.getMessage(), USER);
  }
}
