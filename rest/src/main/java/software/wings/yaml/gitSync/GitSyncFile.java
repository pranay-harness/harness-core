package software.wings.yaml.gitSync;

import com.google.common.base.MoreObjects;

import software.wings.yaml.gitSync.EntityUpdateEvent.SourceType;

import java.util.Objects;

public class GitSyncFile {
  private String name;
  private String yaml;
  private SourceType sourceType;
  private Class klass;
  private String rootPath;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getYaml() {
    return yaml;
  }

  public void setYaml(String yaml) {
    this.yaml = yaml;
  }

  public SourceType getSourceType() {
    return sourceType;
  }

  public void setSourceType(SourceType sourceType) {
    this.sourceType = sourceType;
  }

  public Class getKlass() {
    return klass;
  }

  public void setKlass(Class klass) {
    this.klass = klass;
  }

  public String getRootPath() {
    return rootPath;
  }

  public void setRootPath(String rootPath) {
    this.rootPath = rootPath;
  }

  @Override
  public boolean equals(Object o) {
    if (o == this)
      return true;
    if (!(o instanceof GitSyncFile)) {
      return false;
    }
    GitSyncFile gsf = (GitSyncFile) o;
    return Objects.equals(name, gsf.name) && Objects.equals(yaml, gsf.yaml)
        && Objects.equals(sourceType, gsf.sourceType) && Objects.equals(klass, gsf.klass)
        && Objects.equals(rootPath, gsf.rootPath);
  }

  @Override
  public String toString() {
    MoreObjects.toStringHelper(this);
    return MoreObjects.toStringHelper(this)
        .add("name", getName())
        .add("yaml", getYaml())
        .add("sourceType", getSourceType())
        .add("klass", getKlass())
        .add("rootPath", getRootPath())
        .toString();
  }

  public static final class Builder {
    private String name;
    private String yaml;
    private SourceType sourceType;
    private Class klass;
    private String rootPath;

    private Builder() {}

    public static GitSyncFile.Builder aGitSyncFile() {
      return new GitSyncFile.Builder();
    }

    public GitSyncFile.Builder withName(String name) {
      this.name = name;
      return this;
    }

    public GitSyncFile.Builder withYaml(String yaml) {
      this.yaml = yaml;
      return this;
    }

    public GitSyncFile.Builder withSourceType(SourceType sourceType) {
      this.sourceType = sourceType;
      return this;
    }

    public GitSyncFile.Builder withClass(Class klass) {
      this.klass = klass;
      return this;
    }

    public GitSyncFile.Builder withRootPath(String rootPath) {
      this.rootPath = rootPath;
      return this;
    }

    public GitSyncFile.Builder but() {
      return aGitSyncFile()
          .withName(name)
          .withYaml(yaml)
          .withSourceType(sourceType)
          .withClass(klass)
          .withRootPath(rootPath);
    }

    public GitSyncFile build() {
      GitSyncFile gitSyncFile = new GitSyncFile();
      gitSyncFile.setName(name);
      gitSyncFile.setYaml(yaml);
      gitSyncFile.setSourceType(sourceType);
      gitSyncFile.setKlass(klass);
      gitSyncFile.setRootPath(rootPath);
      return gitSyncFile;
    }
  }
}
