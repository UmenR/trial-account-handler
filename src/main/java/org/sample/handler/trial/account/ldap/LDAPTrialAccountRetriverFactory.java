package org.sample.handler.trial.account.ldap;

import org.sample.handler.trial.account.exceptions.TrialAccountException;
import org.sample.handler.trial.account.task.TrialAccountRetriver;
import org.sample.handler.trial.account.task.TrialAccountRetriverFactory;
import org.wso2.carbon.user.api.RealmConfiguration;

public class LDAPTrialAccountRetriverFactory implements TrialAccountRetriverFactory {

    public static final String LDAP =
            "org.sample.handler.trial.account.ldap.LDAPTrialAccountRetriver";

    @Override
    public TrialAccountRetriver buildCountRetriever(RealmConfiguration realmConfiguration)
            throws TrialAccountException {
        LDAPTrialAccountRetriver ldapNotificationReceiversRetrieval = new LDAPTrialAccountRetriver();
        ldapNotificationReceiversRetrieval.init(realmConfiguration);
        return ldapNotificationReceiversRetrieval;
    }

    @Override
    public String getType() {
        return LDAP;
    }
}
