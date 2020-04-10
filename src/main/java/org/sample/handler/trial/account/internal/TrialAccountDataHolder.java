package org.sample.handler.trial.account.internal;

import org.osgi.framework.BundleContext;
import org.sample.handler.trial.account.constants.TrialAccountConstants;
import org.sample.handler.trial.account.task.TrialAccountRetriverFactory;
import org.wso2.carbon.identity.event.services.IdentityEventService;
import org.wso2.carbon.identity.governance.IdentityGovernanceService;
import org.wso2.carbon.user.core.service.RealmService;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class TrialAccountDataHolder {
    private static volatile TrialAccountDataHolder accountServiceDataHolder = new TrialAccountDataHolder();

    private RealmService realmService;
    private IdentityEventService identityEventService;
    private BundleContext bundleContext;
    private IdentityGovernanceService identityGovernanceService;
    private String expiryTriggerTime;
    private long trialAccountPeriod;
    private boolean isTrialAccountEnabled;
    private Map<String, TrialAccountRetriverFactory> trialAccountRetrivalFactories = new HashMap<>();
    private String trialAccountSuspentionThreadPoolSize = "1";

    public int getTrialAccountSuspentionThreadPoolSize() {
        return Integer.parseInt(trialAccountSuspentionThreadPoolSize);
    }

    public BundleContext getBundleContext() {
        return bundleContext;
    }

    public IdentityGovernanceService getIdentityGovernanceService() {
        return identityGovernanceService;
    }

    public void setIdentityGovernanceService(IdentityGovernanceService identityGovernanceService) {
        this.identityGovernanceService = identityGovernanceService;
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    public static TrialAccountDataHolder getInstance() {
        return accountServiceDataHolder;
    }

    public void setRealmService(RealmService realmService) {
        this.realmService = realmService;
    }

    public RealmService getRealmService() {
        return realmService;
    }

    public Map<String, TrialAccountRetriverFactory> getTrialAccountRetrivalFactories() {
        return trialAccountRetrivalFactories;
    }

    public void addTrialAccountRetrivalFactories(String key, TrialAccountRetriverFactory factory) {
        this.trialAccountRetrivalFactories.put(key,factory);
    }

    public Date getExpiryTriggerTime() throws ParseException {
        DateFormat dateFormat = new SimpleDateFormat(TrialAccountConstants.TRIGGER_TIME_FORMAT);
        return dateFormat.parse(expiryTriggerTime);
    }

    public void setExpiryTriggerTime(String notificationTriggerTime) {
        this.expiryTriggerTime = notificationTriggerTime;
    }
    public void setIdentityEventService(IdentityEventService identityEventService) {
        this.identityEventService = identityEventService;
    }

    public IdentityEventService getIdentityEventService() {
        return identityEventService;
    }

    public void setTrialAccountPeriod(long period){
        this.trialAccountPeriod = period;
    }

    public long getTrialAccountPeriod(){
        return this.trialAccountPeriod;
    }

    public void setIsTrialAccountEnabled(boolean isEnabled){
        this.isTrialAccountEnabled = isEnabled;
    }

    public boolean getTrialAccountEnabled(){
        return this.isTrialAccountEnabled;
    }




}
