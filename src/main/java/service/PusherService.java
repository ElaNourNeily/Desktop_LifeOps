package service;

import com.google.gson.Gson;
import com.pusher.client.Pusher;
import com.pusher.client.PusherOptions;
import com.pusher.client.channel.Channel;
import com.pusher.client.channel.SubscriptionEventListener;
import com.pusher.client.connection.ConnectionEventListener;
import com.pusher.client.connection.ConnectionState;
import com.pusher.client.connection.ConnectionStateChange;

import java.util.Map;
import java.util.Objects;

/**
 * Pusher Channels integration for real-time sync.
 *
 * Config (env or JVM props):
 * - PUSHER_APP_ID / -Dpusher.appId
 * - PUSHER_KEY / -Dpusher.key
 * - PUSHER_SECRET / -Dpusher.secret
 * - PUSHER_CLUSTER / -Dpusher.cluster
 */
public class PusherService {

    private final Gson gson = new Gson();
    private final boolean enabled;

    private final String appId;
    private final String key;
    private final String secret;
    private final String cluster;

    private com.pusher.rest.Pusher httpPusher;
    private Pusher wsPusher;

    public PusherService() {
        String a = read("PUSHER_APP_ID", "pusher.appId");
        String k = read("PUSHER_KEY", "pusher.key");
        String s = read("PUSHER_SECRET", "pusher.secret");
        String c = read("PUSHER_CLUSTER", "pusher.cluster");
        this.appId = a;
        this.key = k;
        this.secret = s;
        this.cluster = c;
        this.enabled = a != null && k != null && s != null && c != null;

        if (enabled) {
            this.httpPusher = new com.pusher.rest.Pusher(appId, key, secret);
            this.httpPusher.setCluster(cluster);
            this.httpPusher.setEncrypted(true);

            PusherOptions options = new PusherOptions().setCluster(cluster);
            this.wsPusher = new Pusher(key, options);
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String channelForBoard(int boardId) {
        return "board-" + boardId;
    }

    public void triggerEvent(String channel, String event, Map<String, Object> data) {
        if (!enabled) return;
        Objects.requireNonNull(channel);
        Objects.requireNonNull(event);
        Objects.requireNonNull(data);
        httpPusher.trigger(channel, event, gson.toJson(data));
    }

    public void connectIfNeeded(ConnectionEventListener listener) {
        if (!enabled) return;
        if (wsPusher.getConnection().getState() == ConnectionState.CONNECTED ||
                wsPusher.getConnection().getState() == ConnectionState.CONNECTING) {
            return;
        }
        wsPusher.connect(listener, ConnectionState.ALL);
    }

    public Channel subscribe(String channel, SubscriptionEventListener eventListener, String... events) {
        if (!enabled) return null;
        Channel ch = wsPusher.subscribe(channel);
        for (String ev : events) {
            ch.bind(ev, eventListener);
        }
        return ch;
    }

    public void unsubscribe(String channel) {
        if (!enabled) return;
        wsPusher.unsubscribe(channel);
    }

    public void disconnect() {
        if (!enabled) return;
        wsPusher.disconnect();
    }

    private String read(String envKey, String propKey) {
        String env = System.getenv(envKey);
        if (env != null && !env.isBlank()) return env.trim();
        String prop = System.getProperty(propKey);
        if (prop != null && !prop.isBlank()) return prop.trim();
        return null;
    }
}

