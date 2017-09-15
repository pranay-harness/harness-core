package software.wings.yaml.gitSync;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class JGitHelloWorld {
  private final Logger logger = LoggerFactory.getLogger(getClass());

  public static void main(String[] args) {
    // Creation of a temp folder that will contain the Git repository
    try {
      File workingDirectory = File.createTempFile("harness-git-test", "");
      workingDirectory.delete();
      workingDirectory.mkdirs();

      // Create a Repository object
      Repository repo = FileRepositoryBuilder.create(new File(workingDirectory, ".git"));
      repo.create();
      Git git = new Git(repo);

      // Create a new file and add it to the index
      File newFile = new File(workingDirectory, "myNewFile");
      newFile.createNewFile();
      git.add().addFilepattern("myNewFile").call();

      // Now, we do the commit with a message
      RevCommit rev = git.commit().setAuthor("gildas", "gildas@example.com").setMessage("My first commit").call();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
