package com.example.carlh.diaryclock.cloud.tasks.persistentAction;

/**
 * Created by carlh on 23.05.2017.
 */

public class ActionHelper {

    public static final String FILE = "system/actions.xml";

    public enum Type {
        UPLOAD ("upload"),
        DELETE_ONLINE ("delete_online"),
        DELETE_OFFLINE ("delete_offline"),
        DOWNLOAD("download"),
        ;
        private final String name;


        public static Type fromString(String text) {
            for (Type b : Type.values()) {
                if (b.name.equalsIgnoreCase(text)) {
                    return b;
                }
            }
            return null;
        }

        private Type(String s) {
            name = s;
        }

    }

}
