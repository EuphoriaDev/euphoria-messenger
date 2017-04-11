package ru.euphoria.messenger.api.model;

import java.io.Serializable;

import ru.euphoria.messenger.json.JsonObject;

/**
 * Base object model for VK.
 */
public abstract class VKModel implements Serializable {
    private static final long serialVersionUID = 1L;

    private Object tag;

    /**
     * Creates a new empty model
     */
    protected VKModel() {
    }

    /**
     * Creates a new model with fields from json source
     *
     * @param source the json source to parse
     */
    protected VKModel(JsonObject source) {
    }

    public void setTag(Object tag) {
        this.tag = tag;
    }

    public Object getTag() {
        return tag;
    }
}
