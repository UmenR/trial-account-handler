package org.sample.handler.trial.account.ldap;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sample.handler.trial.account.constants.TrialAccountConstants;
import org.sample.handler.trial.account.exceptions.TrialAccountException;
import org.sample.handler.trial.account.internal.TrialAccountDataHolder;
import org.sample.handler.trial.account.task.TrialAccountRetriver;
import org.sample.handler.trial.account.util.TrialAccountUser;
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.user.api.RealmConfiguration;
import org.wso2.carbon.user.core.UserCoreConstants;
import org.wso2.carbon.user.core.UserStoreException;
import org.wso2.carbon.user.core.claim.ClaimManager;
import org.wso2.carbon.user.core.ldap.LDAPConnectionContext;
import org.wso2.carbon.user.core.ldap.LDAPConstants;
import org.wso2.carbon.user.core.service.RealmService;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class LDAPTrialAccountRetriver implements TrialAccountRetriver {
    private static final Log log = LogFactory.getLog(LDAPTrialAccountRetriver.class);
    private RealmConfiguration realmConfiguration = null;

    @Override
    public void init(RealmConfiguration realmConfiguration) {
        this.realmConfiguration = realmConfiguration;
    }

    @Override
    public List<TrialAccountUser> getTrialAccounts(long lookupMin, long lookupMax, String tenantDomain) throws TrialAccountException {

        List<TrialAccountUser> users = new ArrayList<TrialAccountUser>();

        if (realmConfiguration != null) {
            String ldapSearchBase = realmConfiguration.getUserStoreProperty(LDAPConstants.USER_SEARCH_BASE);
            RealmService realmService = TrialAccountDataHolder.getInstance().getRealmService();

            try {
                ClaimManager claimManager = (ClaimManager) realmService.getTenantUserRealm(IdentityTenantUtil.
                        getTenantId(tenantDomain)).getClaimManager();
                String userStoreDomain = realmConfiguration.getUserStoreProperty(UserCoreConstants.RealmConfig.
                        PROPERTY_DOMAIN_NAME);
                if (StringUtils.isBlank(userStoreDomain)) {
                    userStoreDomain = IdentityUtil.getPrimaryDomainName();
                }

                String usernameMapAttribute = claimManager.getAttributeName(userStoreDomain, TrialAccountConstants.USERNAME_CLAIM);
                String accountCreationTimeMilis = claimManager.getAttributeName(userStoreDomain, TrialAccountConstants.ACCOUNT_CREATED_TIME);

                if (log.isDebugEnabled()) {
                    log.debug("Retrieving ldap user list for lookupMin: " + lookupMin + " - lookupMax: " + lookupMax);
                }

                LDAPConnectionContext ldapConnectionContext = new LDAPConnectionContext(realmConfiguration);
                DirContext ctx = ldapConnectionContext.getContext();

                //carLicense is the mapped LDAP attribute for LastLoginTime claim
//                String searchFilter = "(&("+accountCreationTimeMilis+">=" + lookupMin + ")("+accountCreationTimeMilis+"<="
//                        + lookupMax + "))";
                int testval = 600000;
                String searchFilter = "("+accountCreationTimeMilis+">=" + testval + ")";

                SearchControls searchControls = new SearchControls();
                searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);

                NamingEnumeration<SearchResult> results = ctx.search(ldapSearchBase, searchFilter, searchControls);

                if (log.isDebugEnabled()) {
                    log.debug("LDAP user list retrieved.");
                }

                while (results.hasMoreElements()) {
                    SearchResult result = results.nextElement();
                    TrialAccountUser trialUser = new TrialAccountUser();
                    trialUser.setUsername((String) result.getAttributes().get(usernameMapAttribute).get());
                    trialUser.setUserStoreDomain(userStoreDomain);
                    users.add(trialUser);
                }
            } catch (NamingException e) {
                throw new TrialAccountException("Failed to filter users from LDAP user store.", e);
            } catch (UserStoreException e) {
                throw new TrialAccountException("Failed to load LDAP connection context.", e);
            } catch (org.wso2.carbon.user.api.UserStoreException e) {
                throw new TrialAccountException("Error occurred while getting tenant user realm for "
                        + "tenant:" + tenantDomain, e);
            }
        }
        return users;
    }
}
