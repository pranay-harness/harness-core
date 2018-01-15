package software.wings.yaml.directory;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

public class DirectoryPath {
  private String path;
  private final String delimiter = "/";

  public DirectoryPath(String path) {
    this.path = path;
  }

  public DirectoryPath add(String pathPart) {
    if (isEmpty(path)) {
      this.path = pathPart;
    } else {
      this.path += this.delimiter + pathPart;
    }

    return this;
  }

  public DirectoryPath clone() {
    return new DirectoryPath(this.path);
  }

  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }
}
