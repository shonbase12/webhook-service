import redis.clients.jedis.Jedis;
import java.util.logging.Logger;

/**
 * Redis-backed implementation of idempotency key store.
 */
public class RedisIdempotencyStore {
    private static final Logger logger = Logger.getLogger(RedisIdempotencyStore.class.getName());
    private Jedis jedis;
    private static final int EXPIRY_SECONDS = 24 * 60 * 60; // 24 hours

    public RedisIdempotencyStore(String redisHost, int redisPort) {
        this.jedis = new Jedis(redisHost, redisPort);
    }

    public boolean containsKey(String idempotencyKey) {
        return jedis.exists(idempotencyKey);
    }

    public void putKey(String idempotencyKey) {
        jedis.setex(idempotencyKey, EXPIRY_SECONDS, "processed");
        logger.info("Stored idempotency key in Redis with expiry: " + idempotencyKey);
    }

    public void close() {
        if (jedis != null) {
            jedis.close();
        }
    }
}
