package software.wings.beans.alert;

public interface AlertData {
  boolean matches(AlertData alertData);

  String buildTitle();
}
