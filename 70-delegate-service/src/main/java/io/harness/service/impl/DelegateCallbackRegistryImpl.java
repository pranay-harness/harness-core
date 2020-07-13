package io.harness.service.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.callback.DelegateCallback;
import io.harness.delegate.beans.DelegateCallbackRecord;
import io.harness.delegate.beans.DelegateCallbackRecord.DelegateCallbackRecordKeys;
import io.harness.exception.UnexpectedException;
import io.harness.persistence.HPersistence;
import io.harness.service.intfc.DelegateCallbackRegistry;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.Date;
import javax.validation.executable.ValidateOnExecution;

@Singleton
@ValidateOnExecution
@Slf4j
public class DelegateCallbackRegistryImpl implements DelegateCallbackRegistry {
  @Inject private HPersistence persistence;

  @Override
  public String ensureCallback(DelegateCallback delegateCallback) {
    try {
      byte[] bytes = delegateCallback.toByteArray();
      // TODO: use better hash-sum
      MessageDigest md = MessageDigest.getInstance("MD5");
      byte[] hashInBytes = md.digest(bytes);
      String uuid = Base64.encodeBase64URLSafeString(hashInBytes);

      DelegateCallbackRecord.builder().uuid(uuid).callbackMetadata(bytes).build();

      persistence.upsert(
          persistence.createQuery(DelegateCallbackRecord.class).filter(DelegateCallbackRecordKeys.uuid, uuid),
          persistence.createUpdateOperations(DelegateCallbackRecord.class)
              .set(DelegateCallbackRecordKeys.uuid, uuid)
              .set(DelegateCallbackRecordKeys.callbackMetadata, bytes)
              .set(DelegateCallbackRecordKeys.validUntil, Date.from(OffsetDateTime.now().plusMonths(1).toInstant())));

      return uuid;
    } catch (NoSuchAlgorithmException e) {
      throw new UnexpectedException("Unexpected", e);
    }
  }
}
