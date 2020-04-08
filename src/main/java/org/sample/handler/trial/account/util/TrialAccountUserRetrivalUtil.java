package org.sample.handler.trial.account.util;

import org.apache.commons.lang.StringUtils;
import org.sample.handler.trial.account.exceptions.TrialAccountException;
import org.sample.handler.trial.account.internal.TrialAccountDataHolder;
import org.sample.handler.trial.account.task.TrialAccountRetriver;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.user.api.RealmConfiguration;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.core.UserCoreConstants;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class TrialAccountUserRetrivalUtil {
    public static final String TRIAL_ACCOUNT_RETRIEVAL_CLASS = "TrialAccountRetriverClass";

    public static Set<String> getSuspensionNotificationEnabledUserStores(String tenantDomain)
            throws TrialAccountException {

        RealmConfiguration realmConfiguration;
        Set<String> userStoreSet = new HashSet<>();
        String domain;

        try {
            realmConfiguration = CarbonContext.getThreadLocalCarbonContext().getUserRealm().getRealmConfiguration();
            domain = IdentityUtil.getPrimaryDomainName();

            if (isEffectiveUserStore(realmConfiguration, true)) {
                userStoreSet.add(domain);
            }

            do {
                realmConfiguration = realmConfiguration.getSecondaryRealmConfig();
                if (realmConfiguration != null) {
                    if (isEffectiveUserStore(realmConfiguration, false)) {
                        userStoreSet.add(realmConfiguration
                                .getUserStoreProperty(UserCoreConstants.RealmConfig.PROPERTY_DOMAIN_NAME));
                    }
                }
            } while (realmConfiguration != null);

        } catch (UserStoreException e) {
            throw new TrialAccountException("Error while getting the trial Account enabled user stores",
                    e);
        }

        return userStoreSet;
    }

    public static TrialAccountRetriver getTrialAccountRetriversForDomain(String domain,
                                                                                  String tenantDomain) throws TrialAccountException {

        TrialAccountRetriver notificationReceiversRetrieval = null;

        if (StringUtils.isEmpty(domain)) {
            domain = IdentityUtil.getPrimaryDomainName();
        }

        RealmConfiguration realmConfiguration = getUserStoreList(tenantDomain).get(domain);

        if (realmConfiguration != null) {
            String retrieverType = realmConfiguration.getUserStoreProperty(TRIAL_ACCOUNT_RETRIEVAL_CLASS);

            if (StringUtils.isNotBlank(retrieverType)) {
                notificationReceiversRetrieval = TrialAccountDataHolder.getInstance()
                        .getTrialAccountRetrivalFactories().get(retrieverType)
                        .buildCountRetriever(realmConfiguration);
            }
            if (notificationReceiversRetrieval == null) {
                throw new TrialAccountException("Could not create an instance of class: " +
                        retrieverType + " for the domain: " + domain);
            }

        }
        return notificationReceiversRetrieval;
    }

    public static Map<String, RealmConfiguration> getUserStoreList(String tenantDomain) throws
            TrialAccountException {
        String domain;
        RealmConfiguration realmConfiguration;
        Map<String, RealmConfiguration> userStoreList = new HashMap<>();

        try {
            PrivilegedCarbonContext.startTenantFlow();
            PrivilegedCarbonContext privilegedCarbonContext = PrivilegedCarbonContext.getThreadLocalCarbonContext();
            privilegedCarbonContext.setTenantId(IdentityTenantUtil.getTenantId(tenantDomain));
            privilegedCarbonContext.setTenantDomain(tenantDomain);
            realmConfiguration = CarbonContext.getThreadLocalCarbonContext().getUserRealm().getRealmConfiguration();
            domain = IdentityUtil.getPrimaryDomainName();
            if (isEffectiveUserStore(realmConfiguration, true)) {
                userStoreList.put(domain, realmConfiguration);
            }

            do {
                realmConfiguration = realmConfiguration.getSecondaryRealmConfig();
                if (realmConfiguration != null) {
                    if (isEffectiveUserStore(realmConfiguration, false)) {
                        domain = realmConfiguration
                                .getUserStoreProperty(UserCoreConstants.RealmConfig.PROPERTY_DOMAIN_NAME);
                        userStoreList.put(domain, realmConfiguration);
                    }
                }
            } while (realmConfiguration != null);

        } catch (UserStoreException e) {
            throw new TrialAccountException(
                    "Error while listing user stores for notification functionality", e);
        } finally {
            PrivilegedCarbonContext.endTenantFlow();
        }

        return userStoreList;
    }

    private static boolean isEffectiveUserStore(RealmConfiguration realmConfiguration, boolean isPrimaryUserStore) {

        if (realmConfiguration == null) {
            return false;
        }

        //Primary User store cannot be disabled

        if (!isPrimaryUserStore) {
            if (Boolean.valueOf(
                    realmConfiguration.getUserStoreProperty(UserCoreConstants.RealmConfig.USER_STORE_DISABLED))) {
                return false;
            }
        }

        if (StringUtils.isBlank(realmConfiguration.getUserStoreProperty(TRIAL_ACCOUNT_RETRIEVAL_CLASS))) {
            return false;
        }

        return true;
    }
}
