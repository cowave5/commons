package com.cowave.commons.framework.util;

import com.cowave.exception.ViewErrorMessageException;

/**
 *
 * @author shanhuiming
 *
 */
public class AssertsException extends ViewErrorMessageException {

    private transient Object[] args;

    private boolean language;

	public AssertsException(String message) {
        super(message);
    }

    public AssertsException(String message, Throwable cause) {
        super(message, cause);
    }

    public AssertsException language(boolean language){
        this.language = language;
        return this;
    }

    public AssertsException args(Object... args){
        this.args = args;
        return this;
    }

    public boolean getLanguage(){
        return this.language;
    }

    public Object[] getArgs(){
        return this.args;
    }
}
