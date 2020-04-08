package org.sample.handler.trial.account.task;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sample.handler.trial.account.constants.TrialAccountConstants;
import org.sample.handler.trial.account.internal.TrialAccountDataHolder;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.identity.application.common.model.Property;
import org.wso2.carbon.identity.base.IdentityException;
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;
import org.wso2.carbon.identity.governance.IdentityGovernanceException;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.carbon.user.core.tenant.Tenant;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

import java.util.ArrayList;
import java.util.List;

public class TrialAccountExpiryThread implements Runnable {

    private static final Log log = LogFactory.getLog(TrialAccountExpiryThread.class);

    @Override
    public void run() {

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
        long trialAccountPeriod = 0;
        try{
            PrivilegedCarbonContext.startTenantFlow();
            PrivilegedCarbonContext privilegedCarbonContext = PrivilegedCarbonContext.getThreadLocalCarbonContext();
            privilegedCarbonContext.setTenantId(IdentityTenantUtil.getTenantId(tenantDomain));
            privilegedCarbonContext.setTenantDomain(tenantDomain);

            identityProperties = TrialAccountDataHolder.getInstance().getIdentityGovernanceService()
                    .getConfiguration(getPropertyNames(), tenantDomain);

            for(Property identityProperty: identityProperties){
                if (identityProperty == null) {
                    continue;
                }

                if (TrialAccountConstants.TRIAL_ACCOUNT_EXPIRY_ENABLED.equals(identityProperty.getName())) {
                    isEnabled = Boolean.parseBoolean(identityProperty.getValue());
                    if (!isEnabled) {
                        return;
                    }
                }

                if (TrialAccountConstants.TRIAL_ACCOUNT_PERIOD.equals(identityProperty.getName())) {
                    try{
                        trialAccountPeriod = Long.parseLong(identityProperty.getValue());
                    } catch(NumberFormatException e) {
                        log.error("Error occurred while reading trial account period for tenant: " + tenantDomain,
                                e);
                    }
                    }


                if (!isEnabled) {
                    return;
                }

                lockTrialAccounts(tenantDomain,trialAccountPeriod);

                }







        } catch (IdentityGovernanceException e) {
            log.error("Error occurred while loading governance configuration for tenants", e);
        } catch (IdentityException e) {
            log.error("Unable to disable user accounts", e);
        } finally {
            PrivilegedCarbonContext.endTenantFlow();
        }
    }

    private String[] getPropertyNames() {
        List<String> properties = new ArrayList<>();
        properties.add(TrialAccountConstants.TRIAL_ACCOUNT_PERIOD);
        properties.add(TrialAccountConstants.TRIAL_ACCOUNT_EXPIRY_ENABLED);
        return properties.toArray(new String[properties.size()]);
    }

    private void lockTrialAccounts(String tenantDomain, long trialPeriod) throws IdentityException {

    }
}
