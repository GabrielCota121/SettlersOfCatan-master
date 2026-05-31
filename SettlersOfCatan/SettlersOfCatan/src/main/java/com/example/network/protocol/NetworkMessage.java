package com.example.network.protocol;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.HashMap;
import java.util.Map;

/**
 * Envelope genérico trafegado pelo WebSocket. Toda mensagem (lobby ou jogo)
 * é serializada como um {@code NetworkMessage} em JSON.
 *
 * <p>O campo {@link #data} é um saco flexível de chave/valor para o payload
 * específico de cada {@link MessageType}, evitando uma classe por mensagem.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NetworkMessage {

    private String type;
    private String roomId;
    private String senderName;
    private Map<String, Object> data = new HashMap<>();

    public NetworkMessage() {}

    public NetworkMessage(String type) {
        this.type = type;
    }

    public static NetworkMessage of(String type) {
        return new NetworkMessage(type);
    }

    // ----- Builder fluente -----
    public NetworkMessage room(String roomId) {
        this.roomId = roomId;
        return this;
    }

    public NetworkMessage sender(String senderName) {
        this.senderName = senderName;
        return this;
    }

    public NetworkMessage put(String key, Object value) {
        this.data.put(key, value);
        return this;
    }

    // ----- Leitura conveniente do payload -----
    public String getString(String key) {
        Object v = data.get(key);
        return v == null ? null : v.toString();
    }

    public int getInt(String key, int defaultValue) {
        Object v = data.get(key);
        if (v instanceof Number n) return n.intValue();
        if (v == null) return defaultValue;
        try {
            return Integer.parseInt(v.toString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public long getLong(String key, long defaultValue) {
        Object v = data.get(key);
        if (v instanceof Number n) return n.longValue();
        if (v == null) return defaultValue;
        try {
            return Long.parseLong(v.toString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        Object v = data.get(key);
        if (v instanceof Boolean b) return b;
        if (v == null) return defaultValue;
        return Boolean.parseBoolean(v.toString());
    }

    // ----- Getters / Setters (necessários para o Jackson) -----
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }

    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }

    public Map<String, Object> getData() { return data; }
    public void setData(Map<String, Object> data) { this.data = data == null ? new HashMap<>() : data; }
}
