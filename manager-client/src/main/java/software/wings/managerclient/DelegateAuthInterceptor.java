package software.wings.managerclient;

import com.nimbusds.jose.EncryptionMethod;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWEAlgorithm;
import com.nimbusds.jose.JWEHeader;
import com.nimbusds.jose.KeyLengthException;
import com.nimbusds.jose.crypto.DirectEncrypter;
import com.nimbusds.jwt.EncryptedJWT;
import com.nimbusds.jwt.JWTClaimsSet;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import javax.crypto.spec.SecretKeySpec;

/**
 * Created by peeyushaggarwal on 12/5/16.
 */
public class DelegateAuthInterceptor implements Interceptor {
  private String accountId;
  private String accountSecret;

  public DelegateAuthInterceptor(String accountId, String accountSecret) {
    this.accountId = accountId;
    this.accountSecret = accountSecret;
  }

  @Override
  public Response intercept(Chain chain) throws IOException {
    JWTClaimsSet jwtClaims = new JWTClaimsSet.Builder()
                                 .issuer(InetAddress.getLocalHost().getHostName())
                                 .subject(accountId)
                                 .audience(chain.request().url().scheme() + "://" + chain.request().url().host() + ":"
                                     + chain.request().url().port())
                                 .expirationTime(new Date(System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(5)))
                                 .notBeforeTime(new Date())
                                 .issueTime(new Date())
                                 .jwtID(UUID.randomUUID().toString())
                                 .build();

    JWEHeader header = new JWEHeader(JWEAlgorithm.DIR, EncryptionMethod.A128GCM);
    EncryptedJWT jwt = new EncryptedJWT(header, jwtClaims);
    DirectEncrypter directEncrypter = null;
    byte[] encodedKey = new byte[0];
    try {
      encodedKey = Hex.decodeHex(accountSecret.toCharArray());
    } catch (DecoderException e) {
      e.printStackTrace();
    }
    try {
      directEncrypter = new DirectEncrypter(new SecretKeySpec(encodedKey, 0, encodedKey.length, "AES"));
    } catch (KeyLengthException e) {
      e.printStackTrace();
      return null;
    }

    try {
      jwt.encrypt(directEncrypter);
    } catch (JOSEException e) {
      e.printStackTrace();
      return null;
    }

    Request request = chain.request();
    return chain.proceed(request.newBuilder().header("Authorization", jwt.serialize()).build());
  }
}
