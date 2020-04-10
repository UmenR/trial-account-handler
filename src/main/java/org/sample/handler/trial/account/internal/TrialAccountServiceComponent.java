package org.sample.handler.trial.account.internal;

import org.apache.log4j.Logger;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.sample.handler.trial.account.TrialAccountHandler;
import org.sample.handler.trial.account.ldap.LDAPTrialAccountRetriver;
import org.sample.handler.trial.account.ldap.LDAPTrialAccountRetriverFactory;
import org.sample.handler.trial.account.task.TrialAccountRetriverFactory;
import org.wso2.carbon.identity.event.handler.AbstractEventHandler;
import org.wso2.carbon.identity.event.services.IdentityEventService;
import org.wso2.carbon.identity.governance.IdentityGovernanceService;
import org.wso2.carbon.user.core.UserStoreException;
import org.wso2.carbon.user.core.service.RealmService;

@Component(name = "org.sample.handler.trial.account.internal.component", service = TrialAccountServiceComponent.class, immediate = true)
public class TrialAccountServiceComponent {
    private static final Logger log = Logger.getLogger(TrialAccountServiceComponent.class);

    @Activate
    protected void activate(ComponentContext context) throws UserStoreException {
        BundleContext bundleContext = context.getBundleContext();
        TrialAccountDataHolder.getInstance().setBundleContext(bundleContext);

        TrialAccountHandler handler = new TrialAccountHandler();
        context.getBundleContext().registerService(AbstractEventHandler.class.getName(), handler, null);

        LDAPTrialAccountRetriverFactory ldapTrialAccountRetriverFactory = new
                LDAPTrialAccountRetriverFactory();
        LDAPTrialAccountRetriver ldapRetriver = new LDAPTrialAccountRetriver();
        TrialAccountDataHolder.getInstance().addTrialAccountRetrivalFactories(ldapRetriver.getClass()
                .getName(),ldapTrialAccountRetriverFactory);
        bundleContext.registerService(TrialAccountRetriverFactory.class.getName(),
                ldapTrialAccountRetriverFactory, null);
        log.info("Custom event handler activated successfully.");
    }
    @Deactivate
    protected void deactivate(ComponentContext context) {
        if (log.isDebugEnabled()) {
            log.debug("Trial account bundle de-activated");
        }
    }

    protected void unsetIdentityEventService(IdentityEventService eventService) {
        TrialAccountDataHolder.getInstance().setIdentityEventService(null);
    }

    @Reference(
            name = "EventMgtService",
            service = org.wso2.carbon.identity.event.services.IdentityEventService.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetIdentityEventService")
    protected void setIdentityEventService(IdentityEventService identityEventService) {
        TrialAccountDataHolder.getInstance().setIdentityEventService(identityEventService);
    }



    protected void unsetIdentityGovernanceService(IdentityGovernanceService idpManager) {
        TrialAccountDataHolder.getInstance().setIdentityGovernanceService(null);
    }
    @Reference(
            name = "IdentityGovernanceService",
            service = org.wso2.carbon.identity.governance.IdentityGovernanceService.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetIdentityGovernanceService")
    protected void setIdentityGovernanceService(IdentityGovernanceService idpManager) {
        TrialAccountDataHolder.getInstance().setIdentityGovernanceService(idpManager);
    }

    protected void unsetRealmService(RealmService realmService) {

        TrialAccountDataHolder.getInstance().setRealmService(null);
        if (log.isDebugEnabled()) {
            log.debug("RealmService is unset in the Trial Account bundle");
        }
    }

    @Reference(
            name = "user.realmservice.default",
            service = org.wso2.carbon.user.core.service.RealmService.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetRealmService")
    protected void setRealmService(RealmService realmService) {

        TrialAccountDataHolder.getInstance().setRealmService(realmService);
        if (log.isDebugEnabled()) {
            log.debug("RealmService is set in the Trial bundle");
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
    @Reference(
            name = "NotificationTaskServiceComponent",
            service = org.sample.handler.trial.account.task.TrialAccountRetriverFactory.class,
            cardinality = ReferenceCardinality.MULTIPLE,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetNotificationReceiversRetrievalFactory")
    protected void setNotificationReceiversRetrievalFactory(
            TrialAccountRetriverFactory notificationReceiversRetrievalFactory) {

        TrialAccountDataHolder.getInstance().getTrialAccountRetrivalFactories()
                .put(notificationReceiversRetrievalFactory.getType(), notificationReceiversRetrievalFactory);
        if (log.isDebugEnabled()) {
            log.debug("Added notification retriever : " + notificationReceiversRetrievalFactory.getType());
        }

    }

}
