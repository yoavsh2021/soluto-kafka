import io.github.cdimascio.dotenv.Dotenv;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;

class Config {
    //Required
    public static int PORT;
    public static String KAFKA_BROKER;
    public static String TOPIC;

    //Optional
    public static String READINESS_TOPIC;
    public static int LINGER_TIME_MS;
    public static String COMPRESSION_TYPE;

    //Autentication
    public static boolean AUTHENTICATED_KAFKA = false;
    public static String SECURITY_PROTOCOL;
    public static String TRUSTSTORE_LOCATION;
    public static String KEYSTORE_LOCATION;
    public static String TRUSTSTORE_PASSWORD;
    public static String KEYSTORE_PASSWORD;
    public static String KEY_PASSWORD;
    public static String SASL_USERNAME;
    public static String SASL_PASSWORD;

    //Statsd monitoring
    public static boolean STATSD_CONFIGURED = false;
    public static String STATSD_PRODUCER_NAME;
    public static String STATSD_API_KEY;
    public static String STATSD_ROOT;
    public static String STATSD_HOST;

    public static void init() throws Exception {
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();

        PORT = getInt(dotenv, "PORT");
        KAFKA_BROKER = getString(dotenv, "KAFKA_BROKER");
        TOPIC = getString(dotenv, "TOPIC");

        READINESS_TOPIC = getOptionalString(dotenv, "READINESS_TOPIC", null);
        LINGER_TIME_MS = getOptionalInt(dotenv, "LINGER_TIME_MS", 0);
        COMPRESSION_TYPE = getOptionalString(dotenv, "COMPRESSION_TYPE", "none");

        String truststoreFilePath = getOptionalString(dotenv, "TRUSTSTORE_FILE_PATH", null);
        if (truststoreFilePath != null) {
            TRUSTSTORE_LOCATION = "client.truststore.jks";
            writeToFile(TRUSTSTORE_LOCATION, readSecretFromFile(getString(dotenv, "TRUSTSTORE_FILE_PATH")));
            TRUSTSTORE_PASSWORD = readSecretFromFile(getString(dotenv, "TRUSTSTORE_PASSWORD_FILE_PATH"));
        }

        SECURITY_PROTOCOL = getOptionalString(dotenv, "SECURITY_PROTOCOL", "");

        if (SECURITY_PROTOCOL.equals("SSL")) {
            KEYSTORE_LOCATION = "client.keystore.p12";
            KEYSTORE_PASSWORD = readSecretFromFile(getString(dotenv, "KEYSTORE_PASSWORD_FILE_PATH"));
            writeToFile(KEYSTORE_LOCATION, readSecretFromFile(getString(dotenv, "KEYSTORE_FILE_PATH")));
            KEY_PASSWORD = readSecretFromFile(getString(dotenv, "KEY_PASSWORD_FILE_PATH"));
            AUTHENTICATED_KAFKA = true;
        }

        if (SECURITY_PROTOCOL.equals("SASL_SSL")) {
            SASL_USERNAME = getString(dotenv, "SASL_USERNAME");
            SASL_PASSWORD = readSecretFromFile(getString(dotenv, "SASL_PASSWORD_FILE_PATH"));
            AUTHENTICATED_KAFKA = true;
        }

        STATSD_PRODUCER_NAME = getOptionalString(dotenv, "STATSD_PRODUCER_NAME", null);
        if (STATSD_PRODUCER_NAME != null) {
            STATSD_API_KEY = readSecretFromFile(getString(dotenv, "STATSD_API_KEY_FILE_PATH"));
            STATSD_ROOT = getString(dotenv, "STATSD_ROOT");
            STATSD_HOST = getString(dotenv, "STATSD_HOST");
            STATSD_CONFIGURED = true;
        }
    }

    private static void writeToFile(String path, String value) throws IOException {
        Files.write(Paths.get(path), Base64.getDecoder().decode(value.getBytes(StandardCharsets.UTF_8)));
    }

    private static String readSecretFromFile(String path) throws IOException {
        return new String(Files.readAllBytes(Paths.get(path)));
    }

    private static String getString(Dotenv dotenv, String name) throws Exception {
        String value = dotenv.get(name);

        if (value == null) {
            throw new Exception("missing env var: " + name);
        }

        return value;
    }

    private static String getOptionalString(Dotenv dotenv, String name, String defaultString) {
        String value = dotenv.get(name);

        if (value == null) {
            return defaultString;
        }

        return value;
    }

    private static int getInt(Dotenv dotenv, String name) {
        return Integer.parseInt(dotenv.get(name));
    }

    private static int getOptionalInt(Dotenv dotenv, String name, int fallback) {
        try {
            return Integer.parseInt(dotenv.get(name));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
