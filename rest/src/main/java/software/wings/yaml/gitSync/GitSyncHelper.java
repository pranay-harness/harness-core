package software.wings.yaml.gitSync;

import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.TransportConfigCallback;
import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.PushResult;
import org.eclipse.jgit.transport.SshSessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class GitSyncHelper {
  private final Logger logger = LoggerFactory.getLogger(getClass());

  private SshSessionFactory sshSessionFactory;
  private TransportConfigCallback transportConfigCallback;
  private Git git;

  public GitSyncHelper(String passphrase, String sshKeyPath) {
    this.sshSessionFactory = new CustomJschConfigSessionFactory(passphrase, sshKeyPath);
    this.transportConfigCallback = new CustomTransportConfigCallback(this.sshSessionFactory);
  }

  public Git clone(String Uri, File localPath) {
    // clone the repo
    CloneCommand cloneCommand = Git.cloneRepository();
    cloneCommand.setURI(Uri);
    cloneCommand.setDirectory(localPath);
    cloneCommand.setTransportConfigCallback(this.transportConfigCallback);

    try {
      this.git = cloneCommand.call();
      return this.git;
    } catch (Exception e) {
      e.printStackTrace();
    }

    return null;
  }

  public DirCache add(String filePattern) {
    try {
      DirCache dirCache = git.add().addFilepattern(filePattern).call();

      return dirCache;
    } catch (Exception e) {
      e.printStackTrace();
    }

    return null;
  }

  public RevCommit commit(String author, String email, String message) {
    if (this.git != null) {
      try {
        RevCommit revCommit = this.git.commit().setAuthor(author, email).setMessage(message).call();
        return revCommit;
      } catch (Exception e) {
        e.printStackTrace();
      }
    }

    return null;
  }

  public Iterable<PushResult> push(String remote) {
    if (this.git != null) {
      try {
        PushCommand pushCommand = git.push();
        pushCommand.setRemote(remote);
        // pushCommand.setRefSpecs( new RefSpec( "release_2_0_2:release_2_0_2" ) );
        pushCommand.setTransportConfigCallback(transportConfigCallback);
        Iterable<PushResult> pushResults = pushCommand.call();

        return pushResults;
      } catch (Exception e) {
        e.printStackTrace();
      }
    }

    return null;
  }

  public void spitOutStatus() {
    if (this.git != null) {
      try {
        Status status = git.status().call();

        logger.info("Added: " + status.getAdded());
        logger.info("Changed: " + status.getChanged());
        logger.info("Conflicting: " + status.getConflicting());
        logger.info("ConflictingStageState: " + status.getConflictingStageState());
        logger.info("IgnoredNotInIndex: " + status.getIgnoredNotInIndex());
        logger.info("Missing: " + status.getMissing());
        logger.info("Modified: " + status.getModified());
        logger.info("Removed: " + status.getRemoved());
        logger.info("Untracked: " + status.getUntracked());
        logger.info("UntrackedFolders: " + status.getUntrackedFolders());
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  public SshSessionFactory getSshSessionFactory() {
    return sshSessionFactory;
  }

  public void setSshSessionFactory(SshSessionFactory sshSessionFactory) {
    this.sshSessionFactory = sshSessionFactory;
  }

  public TransportConfigCallback getTransportConfigCallback() {
    return transportConfigCallback;
  }

  public void setTransportConfigCallback(TransportConfigCallback transportConfigCallback) {
    this.transportConfigCallback = transportConfigCallback;
  }

  public Git getGit() {
    return git;
  }

  public void setGit(Git git) {
    this.git = git;
  }
}
