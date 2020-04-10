package org.sample.handler.trial.account.constants;

public class TrialAccountConstants {
    public static final String TRIAL_ACCOUNT_CLAIM = "http://wso2.org/claims/identity/trialAccount";
    public static final String TRIAL_ACCOUNT_EXPIRED_CLAIM = "http://wso2.org/claims/identity/trialAccountExpired";

    // Should import from elsewhere
    public static final String ACCOUNT_CREATED_TIME = "http://wso2.org/claims/created";
    public final static String USERNAME_CLAIM = "http://wso2.org/claims/username";

    public static final String TRIAL_ACCOUNT_PERIOD = "trialAccountHandler.trial.account.period";
    public static final String TRIAL_ACCOUNT_EXPIRY_ENABLED = "trialAccountHandler.trial.account.expiry.enabled";


    public static final String TRIAL_ACCOUNT_SUSPENSION_TRIGGER_TIME = "trialAccountHandler.trial.account.expiry.trigger.time";
    public static final String TRIGGER_TIME_FORMAT = "HH:mm:ss";
    public static final long SCHEDULER_DELAY = 24;
}

