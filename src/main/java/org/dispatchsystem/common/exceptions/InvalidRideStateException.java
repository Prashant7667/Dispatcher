package org.dispatchsystem.common.exceptions;

public class InvalidRideStateException extends RuntimeException{
    public InvalidRideStateException(String message){
        super(message);
    }
}
