package org.sample.handler.trial.account.task;

import org.sample.handler.trial.account.exceptions.TrialAccountException;
import org.wso2.carbon.user.api.RealmConfiguration;

public interface TrialAccountRetriverFactory {
    public abstract TrialAccountRetriver buildCountRetriever(RealmConfiguration realmConfiguration)
            throws TrialAccountException;

    /**
     * @return
     */
    public abstract String getType();
}
