package software.wings.service.impl;

import org.junit.Ignore;
import org.junit.Test;
import software.wings.WingsBaseTest;
import software.wings.beans.ServiceVariable;
import software.wings.beans.SettingAttribute;
import software.wings.dl.WingsPersistence;
import software.wings.rules.Integration;
import software.wings.security.encryption.Encryptable;
import software.wings.security.encryption.SimpleEncryption;

import java.lang.reflect.Field;
import java.util.List;
import javax.inject.Inject;

/**
 * Created by mike@ on 5/4/17.
 */
@Integration
@Ignore
public class PasswordEncryptionUtil extends WingsBaseTest {
  @Inject WingsPersistence wingsPersistence;

  @Test
  @Ignore
  public void encryptUnencryptedPasswordsInSettingAttributes() {
    List<SettingAttribute> settings = wingsPersistence.list(SettingAttribute.class);
    settings.forEach(setting -> {
      if (setting.getValue() instanceof Encryptable) {
        boolean passwordNeedsFixing = false;
        if (((Encryptable) setting.getValue()).getAccountId() == null) {
          passwordNeedsFixing = true;
        } else
          try {
            Field passwordField = setting.getValue().getClass().getDeclaredField("password");
            passwordField.setAccessible(true);
            if (null != passwordField) {
              try {
                SimpleEncryption encryption = new SimpleEncryption(setting.getAccountId());
                char[] outputChars = encryption.decryptChars((char[]) passwordField.get(setting.getValue()));
              } catch (Exception e) {
                passwordNeedsFixing = true;
              }
            }
          } catch (NoSuchFieldException nsfe) {
            System.out.println("No password for this Encryptable, can't encrypt it: " + setting.getValue().toString());
          }
        if (passwordNeedsFixing) {
          ((Encryptable) setting.getValue()).setAccountId(setting.getAccountId());
          wingsPersistence.save(setting);
        }
      }
    });
  }

  @Test
  @Ignore
  public void encryptUnencryptedConfigValuesInServiceVariables() {
    List<ServiceVariable> variables = wingsPersistence.list(ServiceVariable.class);
    variables.forEach(setting -> {
      boolean variableNeedsFixing = false;
      try {
        Field passwordField = setting.getClass().getDeclaredField("value");
        passwordField.setAccessible(true);
        if (null != passwordField) {
          try {
            SimpleEncryption encryption = new SimpleEncryption(setting.getAccountId());
            char[] outputChars = encryption.decryptChars((char[]) passwordField.get(setting.getValue()));
          } catch (Exception e) {
            variableNeedsFixing = true;
          }
        }
      } catch (NoSuchFieldException nsfe) {
        System.out.println("No password for this Encryptable, can't encrypt it: " + setting.getValue().toString());
      }
      if (variableNeedsFixing) {
        wingsPersistence.save(setting);
      }
    });
  }
}
