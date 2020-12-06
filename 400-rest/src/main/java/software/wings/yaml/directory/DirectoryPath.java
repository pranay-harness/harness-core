package software.wings.yaml.directory;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

public class DirectoryPath implements Cloneable {
  private static final String delimiter = "/";

  private String path;

  public DirectoryPath(String path) {
    this.path = path;
  }

  public DirectoryPath add(String pathPart) {
    if (isEmpty(path)) {
      this.path = pathPart;
    } else {
      this.path += delimiter + pathPart;
    }

    return this;
  }

  @Override
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
