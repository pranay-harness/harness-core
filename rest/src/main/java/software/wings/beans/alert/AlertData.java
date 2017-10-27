package software.wings.beans.alert;

import com.google.inject.Injector;

public interface AlertData {
  boolean matches(AlertData alertData, Injector injector);

  String buildTitle(Injector injector);
}
