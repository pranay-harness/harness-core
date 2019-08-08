package software.wings.beans.trigger;

public interface PayloadSource {
  enum Type { BITBUCKET, GITHUB, GITLABS }
  Type getType();
}
