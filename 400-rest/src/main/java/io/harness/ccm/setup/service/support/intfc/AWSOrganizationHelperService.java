package io.harness.ccm.setup.service.support.intfc;

import com.amazonaws.services.organizations.model.Account;
import software.wings.beans.AwsCrossAccountAttributes;

import java.util.List;

public interface AWSOrganizationHelperService {
  List<Account> listAwsAccounts(AwsCrossAccountAttributes awsCrossAccountAttributes);
}
