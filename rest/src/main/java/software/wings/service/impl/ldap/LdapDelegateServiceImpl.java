package software.wings.service.impl.ldap;

import com.google.common.collect.Sets;
import com.google.inject.Inject;

import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import org.apache.commons.lang3.StringUtils;
import org.ldaptive.LdapEntry;
import org.ldaptive.LdapException;
import org.ldaptive.ResultCode;
import org.ldaptive.SearchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.sso.LdapGroupResponse;
import software.wings.beans.sso.LdapSettings;
import software.wings.beans.sso.LdapTestResponse;
import software.wings.beans.sso.LdapTestResponse.Status;
import software.wings.beans.sso.LdapUserResponse;
import software.wings.helpers.ext.ldap.LdapConstants;
import software.wings.helpers.ext.ldap.LdapResponse;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.intfc.ldap.LdapDelegateService;
import software.wings.service.intfc.security.EncryptionService;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Impl for Ldap Delegate Service  {@link LdapDelegateService}.
 * Created by Pranjal on 08/21/2018
 */
public class LdapDelegateServiceImpl implements LdapDelegateService {
  private static final Logger logger = LoggerFactory.getLogger(LdapDelegateServiceImpl.class);
  @Inject private EncryptionService encryptionService;

  @Override
  public LdapTestResponse validateLdapConnectionSettings(
      LdapSettings settings, EncryptedDataDetail encryptedDataDetail) {
    logger.info("Initiating validateLdapConnectionSettings with ldap settings : ", settings);
    settings.decryptFields(encryptedDataDetail, encryptionService);
    LdapHelper helper = new LdapHelper(settings.getConnectionSettings());
    LdapResponse response = helper.validateConnectionConfig();
    if (response.getStatus() == LdapResponse.Status.SUCCESS) {
      return LdapTestResponse.builder().status(Status.SUCCESS).message(response.getMessage()).build();
    }
    return LdapTestResponse.builder().status(Status.FAILURE).message(response.getMessage()).build();
  }

  @Override
  public LdapTestResponse validateLdapUserSettings(LdapSettings settings, EncryptedDataDetail encryptedDataDetail) {
    logger.info("Initiating validateLdapUserSettings with ldap settings : ", settings);
    settings.decryptFields(encryptedDataDetail, encryptionService);
    LdapHelper helper = new LdapHelper(settings.getConnectionSettings());
    LdapResponse response = helper.validateUserConfig(settings.getUserSettings());
    if (response.getStatus() == LdapResponse.Status.SUCCESS) {
      return LdapTestResponse.builder().status(Status.SUCCESS).message(response.getMessage()).build();
    }
    return LdapTestResponse.builder().status(Status.FAILURE).message(response.getMessage()).build();
  }

  @Override
  public LdapTestResponse validateLdapGroupSettings(LdapSettings settings, EncryptedDataDetail encryptedDataDetail) {
    logger.info("Initiating validateLdapGroupSettings with ldap settings : ", settings);
    settings.decryptFields(encryptedDataDetail, encryptionService);
    LdapHelper helper = new LdapHelper(settings.getConnectionSettings());
    LdapResponse response = helper.validateGroupConfig(settings.getGroupSettings());
    if (response.getStatus() == LdapResponse.Status.SUCCESS) {
      return LdapTestResponse.builder().status(Status.SUCCESS).message(response.getMessage()).build();
    }
    return LdapTestResponse.builder().status(Status.FAILURE).message(response.getMessage()).build();
  }

  @Override
  public LdapTestResponse validateUserSettings(LdapSettings settings, EncryptedDataDetail encryptedDataDetail) {
    LdapTestResponse response = validateLdapConnectionSettings(settings, encryptedDataDetail);
    if (response.getStatus().equals(Status.FAILURE)) {
      throw new WingsException(ErrorCode.INVALID_LDAP_CONFIGURATION, response.getMessage());
    }
    response = validateLdapUserSettings(settings, encryptedDataDetail);
    if (response.getStatus().equals(Status.FAILURE)) {
      throw new WingsException(ErrorCode.INVALID_LDAP_CONFIGURATION, response.getMessage());
    }
    return response;
  }

  @Override
  public LdapResponse authenticate(LdapSettings settings, EncryptedDataDetail settingsEncryptedDataDetail,
      String username, EncryptedDataDetail passwordEncryptedDataDetail) {
    settings.decryptFields(settingsEncryptedDataDetail, encryptionService);
    String password = null;
    try {
      password = new String(encryptionService.getDecryptedValue(passwordEncryptedDataDetail));
    } catch (IOException e) {
      throw new WingsException("Failed to decrypt the password.");
    }
    LdapHelper helper = new LdapHelper(settings.getConnectionSettings());
    return helper.authenticate(settings.getUserSettings(), username, password);
  }

  private LdapGroupResponse buildLdapGroupResponse(LdapEntry group, LdapSettings settings) {
    Set<String> availableAttrs = Sets.newHashSet(group.getAttributeNames());
    String name = group.getAttribute(settings.getGroupSettings().getNameAttr()).getStringValue();
    String description = StringUtils.EMPTY;
    String descriptionAttr = settings.getGroupSettings().getDescriptionAttr();

    if (availableAttrs.contains(descriptionAttr)) {
      description = group.getAttribute(settings.getGroupSettings().getDescriptionAttr()).getStringValue();
    }

    int totalMembers = Integer.parseInt(group.getAttribute(LdapConstants.GROUP_SIZE_ATTR).getStringValue());
    boolean selectable = true;
    String message = "";

    if (LdapConstants.MAX_GROUP_MEMBERS_LIMIT < totalMembers) {
      selectable = false;
      message = LdapConstants.GROUP_MEMBERS_EXCEEDED;
    }

    return LdapGroupResponse.builder()
        .dn(group.getDn())
        .name(name)
        .description(description)
        .totalMembers(totalMembers)
        .selectable(selectable)
        .message(message)
        .build();
  }

  private LdapUserResponse buildLdapUserResponse(LdapEntry user, LdapSettings settings) {
    String name;
    String email = user.getAttribute(settings.getUserSettings().getEmailAttr()).getStringValue();

    if (Arrays.asList(user.getAttributeNames()).contains(settings.getUserSettings().getDisplayNameAttr())) {
      name = user.getAttribute(settings.getUserSettings().getDisplayNameAttr()).getStringValue();
    } else {
      name = email;
    }

    return LdapUserResponse.builder().dn(user.getDn()).name(name).email(email).build();
  }

  @Override
  public Collection<LdapGroupResponse> searchGroupsByName(
      LdapSettings settings, EncryptedDataDetail encryptedDataDetail, String nameQuery) {
    settings.decryptFields(encryptedDataDetail, encryptionService);
    LdapHelper helper = new LdapHelper(settings.getConnectionSettings());
    try {
      SearchResult groups = helper.searchGroupsByName(settings.getGroupSettings(), nameQuery);
      helper.populateGroupSize(groups, settings.getUserSettings());
      return groups.getEntries()
          .stream()
          .map(group -> buildLdapGroupResponse(group, settings))
          .collect(Collectors.toList());
    } catch (LdapException e) {
      throw new WingsException(e.getResultCode().toString());
    }
  }

  @Override
  public LdapGroupResponse fetchGroupByDn(LdapSettings settings, EncryptedDataDetail encryptedDataDetail, String dn) {
    settings.decryptFields(encryptedDataDetail, encryptionService);
    LdapHelper helper = new LdapHelper(settings.getConnectionSettings());
    try {
      SearchResult groups = helper.getGroupByDn(settings.getGroupSettings(), dn);
      helper.populateGroupSize(groups, settings.getUserSettings());
      LdapEntry group = groups.getEntries().isEmpty() ? null : groups.getEntries().iterator().next();
      if (null == group) {
        return null;
      }

      LdapGroupResponse groupResponse = buildLdapGroupResponse(group, settings);
      if (!groupResponse.isSelectable()) {
        return groupResponse;
      }

      SearchResult users = helper.listGroupUsers(settings.getUserSettings(), dn);
      Collection<LdapUserResponse> userResponses =
          users.getEntries().stream().map(user -> buildLdapUserResponse(user, settings)).collect(Collectors.toList());

      groupResponse.setUsers(userResponses);
      return groupResponse;
    } catch (LdapException e) {
      if (e.getResultCode().equals(ResultCode.NO_SUCH_OBJECT)) {
        return null;
      }
      throw new WingsException(e.getResultCode().toString());
    }
  }
}
