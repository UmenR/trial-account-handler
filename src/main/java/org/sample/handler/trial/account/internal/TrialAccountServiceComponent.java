package org.sample.handler.trial.account.internal;

import org.apache.log4j.Logger;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.sample.handler.trial.account.TrialAccountHandler;
import org.sample.handler.trial.account.ldap.LDAPTrialAccountRetriver;
import org.sample.handler.trial.account.ldap.LDAPTrialAccountRetriverFactory;
import org.sample.handler.trial.account.task.TrialAccountRetriverFactory;
import org.wso2.carbon.identity.event.handler.AbstractEventHandler;
import org.wso2.carbon.identity.governance.IdentityGovernanceService;
import org.wso2.carbon.user.core.UserStoreException;
import org.wso2.carbon.user.core.service.RealmService;

public class TrialAccountServiceComponent {
    private static final Logger log = Logger.getLogger(TrialAccountServiceComponent.class);

    protected void activate(ComponentContext context) throws UserStoreException {
        BundleContext bundleContext = context.getBundleContext();
        TrialAccountDataHolder.getInstance().setBundleContext(bundleContext);

        TrialAccountHandler handler = new TrialAccountHandler();
        context.getBundleContext().registerService(AbstractEventHandler.class.getName(), handler, null);

        LDAPTrialAccountRetriverFactory ldapTrialAccountRetriverFactory = new
                LDAPTrialAccountRetriverFactory();
        bundleContext.registerService(TrialAccountRetriverFactory.class.getName(),
                ldapTrialAccountRetriverFactory, null);
    }

    protected void deactivate(ComponentContext context) {
        if (log.isDebugEnabled()) {
            log.debug("Trial account bundle de-activated");
        }
    }

    protected void unsetIdentityGovernanceService(IdentityGovernanceService idpManager) {
        TrialAccountDataHolder.getInstance().setIdentityGovernanceService(null);
    }

    protected void setIdentityGovernanceService(IdentityGovernanceService idpManager) {
        TrialAccountDataHolder.getInstance().setIdentityGovernanceService(idpManager);
    }

    protected void setRealmService(RealmService realmService) {

        TrialAccountDataHolder.getInstance().setRealmService(realmService);
        if (log.isDebugEnabled()) {
            log.debug("RealmService is set in the Trial bundle");
        }
    }

    protected void unsetRealmService(RealmService realmService) {

        TrialAccountDataHolder.getInstance().setRealmService(null);
        if (log.isDebugEnabled()) {
            log.debug("RealmService is unset in the Trial Account bundle");
        }
    }

    protected void unsetNotificationReceiversRetrievalFactory(
            TrialAccountRetriverFactory notificationReceiversRetrievalFactory) {

        TrialAccountDataHolder.getInstance().getTrialAccountRetrivalFactories()
                .remove(notificationReceiversRetrievalFactory.getType());

        if (log.isDebugEnabled()) {
            log.debug("Removed notification retriever : " + notificationReceiversRetrievalFactory.getType());
        }
    }

    protected void setNotificationReceiversRetrievalFactory(
            TrialAccountRetriverFactory notificationReceiversRetrievalFactory) {

        TrialAccountDataHolder.getInstance().getTrialAccountRetrivalFactories()
                .put(notificationReceiversRetrievalFactory.getType(), notificationReceiversRetrievalFactory);
        if (log.isDebugEnabled()) {
            log.debug("Added notification retriever : " + notificationReceiversRetrievalFactory.getType());
        }

    }
}
