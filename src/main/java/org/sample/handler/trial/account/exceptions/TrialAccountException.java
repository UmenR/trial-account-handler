package org.sample.handler.trial.account.exceptions;

import org.wso2.carbon.identity.event.IdentityEventException;

public class TrialAccountException extends IdentityEventException {

    public TrialAccountException(String message) {

        super(message);
    }

    public TrialAccountException(String message, Throwable cause) {

        super(message, cause);
    }

    public TrialAccountException(String code, String message) {

        super(code, message);
    }
}
