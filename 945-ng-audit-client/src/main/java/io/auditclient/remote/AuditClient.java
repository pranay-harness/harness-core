package io.harness.auditclient.remote;

import io.harness.NGCommonEntityConstants;
import io.harness.NGResourceFilterConstants;
import io.harness.beans.PageResponse;
import io.harness.connector.ConnectorValidationResult;
import io.harness.ng.core.NGAccessWithEncryptionConsumer;
import io.harness.rest.RestResponse;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.serializer.kryo.KryoRequest;
import io.harness.serializer.kryo.KryoResponse;

import software.wings.settings.SettingVariableTypes;

import java.util.List;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface AuditClient {
  String AUDIT_SAVE_API = "ng/audits";
  String AUDIT_LIST_API = "ng/";
}