package software.wings.utils;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.eraro.ResponseMessage;
import io.harness.exception.InvalidRequestException;
import software.wings.beans.RestResponse;
import software.wings.beans.RestResponse.Builder;
import software.wings.beans.User;
import software.wings.security.UserThreadLocal;
import software.wings.service.intfc.HarnessUserGroupService;

/**
 * @author marklu on 2019-02-08
 */
@Singleton
public class AccountPermissionUtils {
  @Inject private HarnessUserGroupService harnessUserGroupService;

  public <T> RestResponse<T> checkIfHarnessUser(String errorMessage) {
    User existingUser = UserThreadLocal.get();
    if (existingUser == null) {
      throw new InvalidRequestException("Invalid User");
    }

    RestResponse<T> errorResponse = null;
    if (!harnessUserGroupService.isHarnessSupportUser(existingUser.getUuid())) {
      errorResponse =
          Builder.aRestResponse()
              .withResponseMessages(Lists.newArrayList(ResponseMessage.builder().message(errorMessage).build()))
              .build();
    }
    return errorResponse;
  }
}
