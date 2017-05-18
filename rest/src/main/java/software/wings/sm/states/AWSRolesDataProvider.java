package software.wings.sm.states;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.amazonaws.services.identitymanagement.AmazonIdentityManagementClient;
import com.amazonaws.services.identitymanagement.model.ListRolesRequest;
import com.amazonaws.services.identitymanagement.model.Role;
import software.wings.service.impl.AwsHelperService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.SettingsService;
import software.wings.stencils.DataProvider;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by rishi on 2/9/17.
 */

@Singleton
public class AWSRolesDataProvider implements DataProvider {
  @Inject private SettingsService settingsService;
  @Inject private AwsHelperService awsHelperService;
  @Inject private InfrastructureMappingService infrastructureMappingService;

  @Override
  public Map<String, String> getData(String appId, String... params) {
    AmazonIdentityManagementClient amazonIdentityManagementClient = awsHelperService.getAmazonIdentityManagementClient(
        "AKIAIJ5H5UG5TUB3L2QQ", "Yef4E+CZTR2wRQc3IVfDS4Ls22BAeab9JVlZx2nu".toCharArray());

    return amazonIdentityManagementClient.listRoles(new ListRolesRequest().withMaxItems(400))
        .getRoles()
        .stream()
        .collect(Collectors.toMap(Role::getArn, Role::getRoleName));
  }
}
