package software.wings.search.entities;

import software.wings.beans.Account;
import software.wings.beans.Application;
import software.wings.search.framework.changestreams.ChangeEvent;
import software.wings.search.framework.changestreams.ChangeEvent.ChangeEventBuilder;
import software.wings.search.framework.changestreams.ChangeType;

public class ApplicationEntityTestUtils {
  public static Application createApplication(Account account, String appId, String appName) {
    Application application = new Application();
    application.setName(appName);
    application.setAccountId(account.getUuid());
    application.setUuid(appId);
    return application;
  }

  public static ChangeEvent createApplicationChangeEvent(Application application, ChangeType changeType) {
    ChangeEventBuilder changeEventBuilder = ChangeEvent.builder();
    changeEventBuilder = changeEventBuilder.changeType(changeType)
                             .fullDocument(application)
                             .token("token")
                             .uuid(application.getUuid())
                             .entityType(Application.class);
    return changeEventBuilder.build();
  }
}
