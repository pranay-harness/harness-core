package software.wings.search.entities.application;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import software.wings.beans.Application;
import software.wings.search.framework.changestreams.ChangeEvent;
import software.wings.search.framework.changestreams.ChangeEvent.ChangeEventBuilder;
import software.wings.search.framework.changestreams.ChangeType;

public class ApplicationEntityTestUtils {
  public static Application createApplication(String accountId, String appId, String appName) {
    Application application = new Application();
    application.setName(appName);
    application.setAccountId(accountId);
    application.setUuid(appId);
    return application;
  }

  public static DBObject getApplicationChanges() {
    BasicDBObject basicDBObject = new BasicDBObject();
    basicDBObject.put("name", "edited_name");
    return basicDBObject;
  }

  public static ChangeEvent createApplicationChangeEvent(Application application, ChangeType changeType) {
    ChangeEventBuilder changeEventBuilder = ChangeEvent.builder();
    changeEventBuilder = changeEventBuilder.changeType(changeType)
                             .fullDocument(application)
                             .token("token")
                             .uuid(application.getUuid())
                             .entityType(Application.class);

    if (changeType == ChangeType.UPDATE) {
      changeEventBuilder = changeEventBuilder.changes(getApplicationChanges());
    }
    return changeEventBuilder.build();
  }
}