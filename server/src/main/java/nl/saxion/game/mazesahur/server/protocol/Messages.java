package nl.saxion.game.mazesahur.server.protocol;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Simple DTOs for JSON serialization/deserialization.
 * All messages carry a "type" discriminator.
 */
public final class Messages {

    private Messages() {
    }

    public static class BaseMessage {
        public String type;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class JoinRequest extends BaseMessage {
        public String room;
        public String name;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class InputMessage extends BaseMessage {
        public long seq;
        @JsonProperty("moveX")
        public float moveX;
        @JsonProperty("moveZ")
        public float moveZ;
        public float yaw;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static final class JoinedResponse extends BaseMessage {
        public String playerId;
        public String room;
        public long seed;
        public List<PlayerState> players;
    }

    public static final class StateMessage extends BaseMessage {
        public long ts;
        public List<PlayerState> players;
        public long seqAck;
        public EnemyState enemy;
    }

    public static final class PlayerState {
        public String id;
        public String name;
        public float x;
        public float y;
        public float z;
        public float yaw;
    }

    public static final class EnemyState {
        public float x;
        public float y;
        public float z;
        public float yaw;
    }
}
