package org.sample.handler.trial.account.task;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sample.handler.trial.account.constants.TrialAccountConstants;
import org.sample.handler.trial.account.exceptions.TrialAccountException;
import org.sample.handler.trial.account.internal.TrialAccountDataHolder;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.identity.application.common.model.Property;
import org.wso2.carbon.identity.base.IdentityException;
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.core.UserCoreConstants;
import org.wso2.carbon.user.core.UserRealm;
import org.wso2.carbon.user.core.UserStoreManager;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.carbon.user.core.tenant.Tenant;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.HashMap;
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
        int trialAccountPeriod = TrialAccountDataHolder.getInstance().getTrialAccountPeriod();
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

    private void lockTrialAccounts(String tenantDomain, int trialPeriod) throws IdentityException {
        try {
            RealmService realmService = TrialAccountDataHolder.getInstance().getRealmService();
            int tenantId = IdentityTenantUtil.getTenantId(tenantDomain);
            UserRealm userRealm = null;
            UserStoreManager userStoreManager = null;
            try {
                userRealm = (UserRealm) realmService.getTenantUserRealm(tenantId);
                if (log.isDebugEnabled()) {
                    log.debug("Realm Service received for trial account termination in tenant : " + tenantDomain);
                }
                userStoreManager = userRealm.getUserStoreManager();
                if (log.isDebugEnabled()) {
                    log.debug("User store manager received for trial account termination in tenant : "
                            + tenantDomain);
                }
            } catch (UserStoreException e) {
                log.error("Failed retrieve the user store manager for trial account termination in tenant: "
                        + tenantDomain, e);
            }
            if(userRealm != null && userStoreManager != null){
                String [] users;
                try{
                    users = userStoreManager.getUserList(TrialAccountConstants.ACCOUNT_CREATED_TIME,
                            getCreationDateToLockTrialAccounts(trialPeriod)+"*",
                            null);
                }catch(UserStoreException e){
                    throw new IdentityException("Failed retrieve the user store manager for tenant: " + tenantDomain,
                            e);
                }
                for(String user: users){
                    try {
                        Map<String,String> userClaims = userStoreManager.getUserClaimValues(user,
                                new String[] {TrialAccountConstants.TRIAL_ACCOUNT_CLAIM,
                                        TrialAccountConstants.TRIAL_ACCOUNT_EXPIRED_CLAIM},null);
                        if (log.isDebugEnabled()) {
                            log.debug("Claims user : " + user + " " + userClaims);
                        }
                        boolean isTrialAccount = false;
                        boolean isTrialOver = false;
                        if(!userClaims.isEmpty()){
                            for(Map.Entry<String,String> claim : userClaims.entrySet()){
                                if(TrialAccountConstants.TRIAL_ACCOUNT_CLAIM.equals(claim.getKey())){
                                    isTrialAccount = Boolean.parseBoolean(claim.getValue());
                                    continue;
                                }
                                if(TrialAccountConstants.TRIAL_ACCOUNT_EXPIRED_CLAIM.equals(claim.getKey())){
                                    isTrialOver = Boolean.parseBoolean(claim.getValue());
                                    continue;
                                }
                            }
                        }
                        if(isTrialAccount && !isTrialOver){
                            Map<String, String> updatedClaims = new HashMap<>();
                            updatedClaims.put(TrialAccountConstants.TRIAL_ACCOUNT_EXPIRED_CLAIM,
                                    Boolean.TRUE.toString());
                            userStoreManager.setUserClaimValues(user, updatedClaims, UserCoreConstants.DEFAULT_PROFILE);
                            if (log.isDebugEnabled()) {
                                log.debug("Trial account terminated for trial user : " + user);
                            }
                        }
                    } catch (UserStoreException e){
                        log.error("Error while checking trial status for user : " + user + e);
                    }
                }
            }
        } catch (TrialAccountException e) {
            log.error("Error occurred while terminating trial users for tenant : " + tenantDomain, e);
        }
    }
    private String getCreationDateToLockTrialAccounts(int trialPeriod){
        DateFormat df = new SimpleDateFormat(TrialAccountConstants.DATE_FORMAT);
        return df.format(LocalDate.now().plusDays(-trialPeriod));
    }
}
