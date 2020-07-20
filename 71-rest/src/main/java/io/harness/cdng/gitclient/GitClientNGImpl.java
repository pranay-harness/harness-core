package io.harness.cdng.gitclient;

import static io.harness.eraro.ErrorCode.UNREACHABLE_HOST;
import static io.harness.exception.ExceptionUtils.getMessage;

import com.google.common.annotations.VisibleForTesting;

import io.harness.delegate.beans.connector.gitconnector.GitConfigDTO;
import io.harness.delegate.beans.connector.gitconnector.GitConnectionType;
import io.harness.delegate.beans.connector.gitconnector.GitHTTPAuthenticationDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.LsRemoteCommand;
import org.eclipse.jgit.api.TransportCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.errors.NoRemoteRepositoryException;
import org.eclipse.jgit.lib.Ref;
import org.jetbrains.annotations.NotNull;
import software.wings.service.impl.yaml.UsernamePasswordCredentialsProviderWithSkipSslVerify;

import java.net.UnknownHostException;
import java.util.Collection;

@Slf4j
public class GitClientNGImpl implements GitClientNG {
  @Override
  public String validate(GitConfigDTO gitConfig) {
    if (GitConnectionType.REPO == gitConfig.getGitAuth().getGitConnectionType()) {
      try {
        // Init Git repo
        initGitAndGetBranches(gitConfig);
      } catch (TransportException te) {
        logger.info("Git validation failed [{}]", te);
        Throwable cause = te.getCause();
        if (cause instanceof TransportException) {
          TransportException tee = (TransportException) cause;
          if (tee.getCause() instanceof UnknownHostException) {
            return UNREACHABLE_HOST.getDescription() + gitConfig.getGitAuth().getUrl();
          }
        }
      } catch (InvalidRemoteException e) {
        return buildInvalidGitRepoErrorMessage(gitConfig, e);
      } catch (Exception e) {
        if (e.getCause() instanceof NoRemoteRepositoryException) {
          return buildInvalidGitRepoErrorMessage(gitConfig, e);
        }
        return getMessage(e);
      }
    } else {
      // TODO @deepak: implement account level git connector.
      return "Not implemented";
    }
    return null; // no error
  }

  @NotNull
  private String buildInvalidGitRepoErrorMessage(GitConfigDTO gitConfig, Exception e) {
    logger.info("Git validation failed.", e);
    return "Invalid git repo " + gitConfig.getGitAuth().getUrl();
  }

  @VisibleForTesting
  void initGitAndGetBranches(GitConfigDTO gitConfig) throws GitAPIException {
    LsRemoteCommand lsRemoteCommand = Git.lsRemoteRepository();
    getAuthConfiguredCommand(lsRemoteCommand, gitConfig);
    Collection<Ref> refs =
        lsRemoteCommand.setRemote(gitConfig.getGitAuth().getUrl()).setHeads(true).setTags(true).call();
    logger.info("Remote branches [{}]", refs);
  }

  private TransportCommand getAuthConfiguredCommand(TransportCommand gitCommand, GitConfigDTO gitConfig) {
    if (gitConfig.getGitAuth().getUrl().toLowerCase().startsWith("http")) {
      configureHttpCredentialProvider(gitCommand, (GitHTTPAuthenticationDTO) gitConfig.getGitAuth());
    } else {
      throw new NotImplementedException("Not implemented ssh");
    }
    return gitCommand;
  }

  private void configureHttpCredentialProvider(TransportCommand gitCommand, GitHTTPAuthenticationDTO gitAuth) {
    String username = gitAuth.getUsername();
    char[] password = gitAuth.getPassword();
    // todo @deepak: add kerberos later
    gitCommand.setCredentialsProvider(new UsernamePasswordCredentialsProviderWithSkipSslVerify(username, password));
  }
}
