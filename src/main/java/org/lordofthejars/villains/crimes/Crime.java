package org.lordofthejars.villains.crimes;

import io.vertx.core.json.JsonObject;

public class Crime {

    private String name;
    private String villain;
    private String wiki;

    public Crime(JsonObject jsonObject) {

        if (jsonObject.containsKey("NAME")) {
            this.name = jsonObject.getString("NAME");
        }

        if (jsonObject.containsKey("VILLAIN")) {
            this.villain = jsonObject.getString("VILLAIN");
        }

        if (jsonObject.containsKey("WIKI")) {
            this.wiki = jsonObject.getString("WIKI");
        }

    }

    public String getName() {
        return name;
    }

    public String getVillain() {
        return villain;
    }

    public String getWiki() {
        return wiki;
    }
}
