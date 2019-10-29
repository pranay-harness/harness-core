package software.wings.search.entities;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import software.wings.beans.Account;
import software.wings.beans.Environment;
import software.wings.search.framework.changestreams.ChangeEvent;
import software.wings.search.framework.changestreams.ChangeEvent.ChangeEventBuilder;
import software.wings.search.framework.changestreams.ChangeType;

public class EnvironmentEntityTestUtils {
  public static Environment createEnvironment(Account account, String appId, String envId, String envName) {
    Environment environment = new Environment();
    environment.setUuid(envId);
    environment.setAppId(appId);
    environment.setAccountId(account.getUuid());
    environment.setName(envName);
    return environment;
  }

  private static DBObject getEnvironmentChanges() {
    BasicDBObject basicDBObject = new BasicDBObject();
    basicDBObject.put("name", "edited_name");
    return basicDBObject;
  }

  public static ChangeEvent createEnvironmentChangeEvent(Environment environment, ChangeType changeType) {
    ChangeEventBuilder changeEventBuilder = ChangeEvent.builder();
    changeEventBuilder = changeEventBuilder.changeType(changeType)
                             .fullDocument(environment)
                             .token("token")
                             .uuid(environment.getUuid())
                             .entityType(Environment.class);

    if (changeType.equals(ChangeType.UPDATE)) {
      changeEventBuilder = changeEventBuilder.changes(getEnvironmentChanges());
    }

    return changeEventBuilder.build();
  }
}
