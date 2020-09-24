package io.harness.ccm.views.service.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.ccm.views.dao.ViewCustomFieldDao;
import io.harness.ccm.views.entities.ViewCustomField;
import io.harness.ccm.views.entities.ViewField;
import io.harness.ccm.views.entities.ViewFieldIdentifier;
import io.harness.ccm.views.service.ViewCustomFieldService;
import io.harness.exception.InvalidRequestException;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Singleton
public class ViewCustomFieldServiceImpl implements ViewCustomFieldService {
  @Inject private ViewCustomFieldDao viewCustomFieldDao;

  private static final String CUSTOM_FIELD_DUPLICATE_EXCEPTION = "Custom Field with given name already exists";

  @Override
  public ViewCustomField save(ViewCustomField viewCustomField) {
    validateViewCustomField(viewCustomField);
    viewCustomFieldDao.save(viewCustomField);
    return viewCustomField;
  }

  @Override
  public ViewCustomField get(String uuid) {
    return viewCustomFieldDao.getById(uuid);
  }

  @Override
  public List<ViewField> getCustomFields(String accountId) {
    List<ViewCustomField> viewCustomFields = viewCustomFieldDao.findByAccountId(accountId);
    return viewCustomFields.stream()
        .map(viewCustomField
            -> ViewField.builder()
                   .fieldId(viewCustomField.getUuid())
                   .identifier(ViewFieldIdentifier.CUSTOM)
                   .fieldName(viewCustomField.getName())
                   .build())
        .collect(Collectors.toList());
  }

  public boolean validateViewCustomField(ViewCustomField viewCustomField) {
    ViewCustomField savedCustomField = viewCustomFieldDao.findByName(
        viewCustomField.getAccountId(), viewCustomField.getViewId(), viewCustomField.getName());
    if (null != savedCustomField) {
      throw new InvalidRequestException(CUSTOM_FIELD_DUPLICATE_EXCEPTION);
    }
    return true;
  }
}
