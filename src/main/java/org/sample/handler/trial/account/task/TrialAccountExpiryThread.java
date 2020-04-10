package org.sample.handler.trial.account.task;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sample.handler.trial.account.constants.TrialAccountConstants;
import org.sample.handler.trial.account.exceptions.TrialAccountException;
import org.sample.handler.trial.account.internal.TrialAccountDataHolder;
import org.sample.handler.trial.account.util.TrialAccountUser;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.identity.application.common.model.Property;
import org.wso2.carbon.identity.base.IdentityException;
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.identity.governance.IdentityGovernanceException;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.core.UserCoreConstants;
import org.wso2.carbon.user.core.UserRealm;
import org.wso2.carbon.user.core.UserStoreManager;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.carbon.user.core.tenant.Tenant;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TrialAccountExpiryThread implements Runnable {

    private static final Log log = LogFactory.getLog(TrialAccountExpiryThread.class);

    @Override
    public void run() {
        log.info("Running task");
        if (log.isDebugEnabled()) {
            log.debug("Idle account suspension task started.");
        }

        RealmService realmService = TrialAccountDataHolder.getInstance().getRealmService();

        Tenant tenants[] = new Tenant[0];

        try {
            tenants = (Tenant[]) realmService.getTenantManager().getAllTenants();
        } catch (UserStoreException e) {
            log.error("Error occurred while retrieving tenants", e);
        }

        handleTask(MultitenantConstants.SUPER_TENANT_DOMAIN_NAME);

        for (Tenant tenant : tenants) {
            handleTask(tenant.getDomain());
        }

    }

    private void handleTask(String tenantDomain) {
        if (log.isDebugEnabled()) {
            log.debug("Handling trial account expiry task for tenant: " + tenantDomain);
        }

        Property[] identityProperties;
        boolean isEnabled = false;
        long trialAccountPeriod = TrialAccountDataHolder.getInstance().getTrialAccountPeriod();
        try{
            PrivilegedCarbonContext.startTenantFlow();
            PrivilegedCarbonContext privilegedCarbonContext = PrivilegedCarbonContext.getThreadLocalCarbonContext();
            privilegedCarbonContext.setTenantId(IdentityTenantUtil.getTenantId(tenantDomain));
            privilegedCarbonContext.setTenantDomain(tenantDomain);
            lockTrialAccounts(tenantDomain,trialAccountPeriod);

        }  catch (IdentityException e) {
            log.error("Unable to disable user accounts", e);
        } finally {
            PrivilegedCarbonContext.endTenantFlow();
        }
    }

//    private String[] getPropertyNames() {
//        List<String> properties = new ArrayList<>();
//        properties.add(TrialAccountConstants.TRIAL_ACCOUNT_PERIOD);
//        properties.add(TrialAccountConstants.TRIAL_ACCOUNT_EXPIRY_ENABLED);
//        return properties.toArray(new String[properties.size()]);
//    }

    private void lockTrialAccounts(String tenantDomain, long trialPeriod) throws IdentityException {
        List<TrialAccountUser> trialUsers = null;
        try {
            trialUsers = TrialAccountRetrievalManager.getReceivers(trialPeriod, tenantDomain);
            // Filter according to claims.
            log.info("-------------- returned from get receivers is empty : " + !trialUsers.isEmpty());
            if (!trialUsers.isEmpty()){
                for(TrialAccountUser user: trialUsers){
                    RealmService realmService = TrialAccountDataHolder.getInstance().getRealmService();
                    int tenantId = IdentityTenantUtil.getTenantId(tenantDomain);

                    UserRealm userRealm;
                    try {
                        userRealm = (UserRealm) realmService.getTenantUserRealm(tenantId);
                    } catch (UserStoreException e) {
                        throw new IdentityException("Failed retrieve the user realm for tenant: " + tenantDomain, e);
                    }

                    UserStoreManager userStoreManager;
                    try {
                        userStoreManager = userRealm.getUserStoreManager();
                    } catch (org.wso2.carbon.user.core.UserStoreException e) {
                        throw new IdentityException("Failed retrieve the user store manager for tenant: " + tenantDomain,
                                e);
                    }
                    boolean isTrialAccount = false;
                    boolean isTrialExpired = false;
                    try {
                        //TODO Check for null ?
                         isTrialAccount = Boolean.parseBoolean(userStoreManager.getUserClaimValue(IdentityUtil.addDomainToName(user.getUsername(),user.getUserStoreDomain()),
                                TrialAccountConstants.TRIAL_ACCOUNT_CLAIM,null));
                         isTrialExpired = Boolean.parseBoolean(userStoreManager.getUserClaimValue(IdentityUtil.addDomainToName(user.getUsername(),user.getUserStoreDomain()),
                                 TrialAccountConstants.TRIAL_ACCOUNT_EXPIRED_CLAIM,null));
                    } catch(org.wso2.carbon.user.core.UserStoreException e){
                        throw new IdentityException("Failed retrieve claims: " + tenantDomain,
                                e);
                    }
                    if(!isTrialAccount || (isTrialAccount && isTrialExpired)){
                        continue;
                    }
                    Map<String, String> updatedClaims = new HashMap<>();
                    updatedClaims.put(TrialAccountConstants.TRIAL_ACCOUNT_EXPIRY_ENABLED, Boolean.TRUE.toString());

                    try {
                        userStoreManager.setUserClaimValues(IdentityUtil.addDomainToName(user.getUsername(),
                                user.getUserStoreDomain()), updatedClaims, UserCoreConstants.DEFAULT_PROFILE);
                    } catch (org.wso2.carbon.user.core.UserStoreException e) {
                        throw new IdentityException("Failed to update claim values for user: " + IdentityUtil
                                .addDomainToName(user.getUsername(), user.getUserStoreDomain()) + " in tenant: " +
                                tenantDomain);
                    }
                }
            } else {
                log.info("-------- NO TRIAL ACCOUNTS");
            }
        } catch (TrialAccountException e) {
            throw IdentityException.error("Error occurred while retrieving users for account disable", e);
        }
    }
}
