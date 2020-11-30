package io.harness.notification.remote;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.ng.core.dto.UserGroupDTO;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

@OwnedBy(PL)
public interface UserGroupClient {
  String USER_GROUP_BASEURI = "/user-groups";

  @POST(USER_GROUP_BASEURI + "/batch")
  Call<ResponseDTO<List<UserGroupDTO>>> getUserGroups(@Body List<String> userGroupIds);
}
