package io.harness.notification.remote;

import static io.harness.NGCommonEntityConstants.ACCOUNT_KEY;
import static io.harness.NGCommonEntityConstants.GROUP_IDENTIFIERS_KEY;
import static io.harness.NotificationResourceConstants.NOTIFICATION_CHANNEL_KEY;
import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.notification.NotificationChannelType;
import io.harness.rest.RestResponse;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

@OwnedBy(PL)
public interface UserGroupClient {
  String USER_GROUP_BASEURI = "ng/userGroup";
  String NOTIFICATION_SETTING_API = USER_GROUP_BASEURI + "/notificationSetting";

  @GET(NOTIFICATION_SETTING_API)
  Call<RestResponse<List<String>>> getNotificationSetting(@Query(value = GROUP_IDENTIFIERS_KEY) List<String> groupIds,
      @Query(value = NOTIFICATION_CHANNEL_KEY) NotificationChannelType notificationChannelType,
      @Query(value = ACCOUNT_KEY) String accountId);
}
