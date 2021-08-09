package io.harness.datahandler.services;

public interface AdminUserService {
  boolean enableOrDisableUser(String accountId, String userIdOrEmail, boolean enabled);
}
