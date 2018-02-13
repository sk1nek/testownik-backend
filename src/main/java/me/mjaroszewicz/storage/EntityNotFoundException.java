package me.mjaroszewicz.storage;

public class EntityNotFoundException extends Throwable {

    public EntityNotFoundException(){
        super();
    }

    public EntityNotFoundException(String msg){
        super(msg);
    }

}
