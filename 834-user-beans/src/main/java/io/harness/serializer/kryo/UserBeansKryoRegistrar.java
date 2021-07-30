package io.harness.serializer.kryo;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ccm.license.CeLicenseInfo;
import io.harness.ccm.license.CeLicenseType;
import io.harness.serializer.KryoRegistrar;

import software.wings.beans.Account;
import software.wings.beans.AccountEvent;
import software.wings.beans.AccountEventType;
import software.wings.beans.AccountPreferences;
import software.wings.beans.LicenseInfo;
import software.wings.beans.NameValuePair;
import software.wings.beans.Role;
import software.wings.beans.RoleType;
import software.wings.beans.TechStack;
import software.wings.beans.TrialSignupOptions;

import com.esotericsoftware.kryo.Kryo;

/**
 * Class will register all kryo classes
 */

@OwnedBy(PL)
public class UserBeansKryoRegistrar implements KryoRegistrar {
  @Override
  public void register(Kryo kryo) {
    kryo.register(Role.class, 5194);
    kryo.register(RoleType.class, 5195);
    kryo.register(NameValuePair.class, 5226);
    kryo.register(Account.class, 5356);
    kryo.register(LicenseInfo.class, 5511);
    kryo.register(AccountEventType.class, 7446);
    kryo.register(AccountEvent.class, 7445);
    kryo.register(CeLicenseType.class, 7466);
    kryo.register(TechStack.class, 7447);
    kryo.register(CeLicenseInfo.class, 7465);
    kryo.register(TrialSignupOptions.class, 8093);
    kryo.register(TrialSignupOptions.Products.class, 8094);
    kryo.register(AccountPreferences.class, 8124);
  }
}
