package MyServer;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
public class crypto_test {
    public static void main(String[] args) throws NoSuchAlgorithmException {
        SecureRandom random= new SecureRandom();
        KeyPairGenerator keyPairGenerator= KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(1024, random);
        KeyPair keyPair=keyPairGenerator.genKeyPair();
        System.out.println(keyPair);
    }
}
