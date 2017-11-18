package com.example.carlh.diaryclock.cloud.tasks.persistentAction;


/**
 * Created by carlh on 23.05.2017.
 */

public class Action {
    private ActionHelper.Type type;
    private Long id;

    public  Action(ActionHelper.Type type, Long id){
        this.type = type;
        this.id = id;
    }

    public ActionHelper.Type getType() {
        return type;
    }

    public void setAction(ActionHelper.Type type) {
        this.type = type;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
