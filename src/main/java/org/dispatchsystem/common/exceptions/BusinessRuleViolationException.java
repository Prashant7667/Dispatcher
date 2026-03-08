package org.dispatchsystem.common.exceptions;

public class BusinessRuleViolationException extends RuntimeException{
    public BusinessRuleViolationException(String message){
        super(message);
    }
}
