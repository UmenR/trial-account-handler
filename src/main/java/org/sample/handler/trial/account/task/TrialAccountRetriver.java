package org.sample.handler.trial.account.task;

import org.sample.handler.trial.account.exceptions.TrialAccountException;
import org.sample.handler.trial.account.util.TrialAccountUser;
import org.wso2.carbon.user.api.RealmConfiguration;

import java.util.List;

public interface TrialAccountRetriver {
    public List<TrialAccountUser> getTrialAccounts(long trialPeriodMin,long trialPeriodMax, String tenantDomain) throws TrialAccountException;

    void init(RealmConfiguration realmConfiguration);
}
