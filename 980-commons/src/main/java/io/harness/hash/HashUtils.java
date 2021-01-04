package io.harness.hash;

import io.harness.exception.InvalidRequestException;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import lombok.experimental.UtilityClass;

@UtilityClass
public class HashUtils {
  public static String calculateSha256(String input) {
    {
      try {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] messageDigest = md.digest(input.getBytes());
        BigInteger no = new BigInteger(1, messageDigest);
        String hashtext = no.toString(16);
        while (hashtext.length() < 32) {
          hashtext = "0" + hashtext;
        }
        return hashtext;
      } catch (NoSuchAlgorithmException e) {
        throw new InvalidRequestException("Cannot hash the input", e);
      }
    }
  }
}
