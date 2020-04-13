/*
 * Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

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
import org.wso2.carbon.identity.event.handler.AbstractEventHandler;
import org.wso2.carbon.identity.event.services.IdentityEventService;
import org.wso2.carbon.identity.governance.IdentityGovernanceService;
import org.wso2.carbon.user.core.UserStoreException;
import org.wso2.carbon.user.core.service.RealmService;

@Component(name = "org.sample.handler.trial.account.internal.component", service = TrialAccountServiceComponent.class,
        immediate = true)
public class TrialAccountServiceComponent {
    private static final Logger log = Logger.getLogger(TrialAccountServiceComponent.class);

    @Activate
    protected void activate(ComponentContext context) throws UserStoreException {
        BundleContext bundleContext = context.getBundleContext();
        TrialAccountDataHolder.getInstance().setBundleContext(bundleContext);
        TrialAccountHandler handler = new TrialAccountHandler();
        context.getBundleContext().registerService(AbstractEventHandler.class.getName(), handler, null);
        if (log.isDebugEnabled()) {
            log.debug("Trial account bundle activated");
        }
    }
    @Deactivate
    protected void deactivate(ComponentContext context) {
        if (log.isDebugEnabled()) {
            log.debug("Trial account bundle de-activated");
        }
    }

    protected void unsetIdentityEventService(IdentityEventService eventService) {
        TrialAccountDataHolder.getInstance().setIdentityEventService(null);
        if (log.isDebugEnabled()) {
            log.debug("IdentityEventService is un-set in the Trial Account bundle");
        }
    }

    @Reference(
            name = "EventMgtService",
            service = org.wso2.carbon.identity.event.services.IdentityEventService.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetIdentityEventService")
    protected void setIdentityEventService(IdentityEventService identityEventService) {
        TrialAccountDataHolder.getInstance().setIdentityEventService(identityEventService);
        if (log.isDebugEnabled()) {
            log.debug("IdentityEventService is set in the Trial Account bundle");
        }
    }



    protected void unsetIdentityGovernanceService(IdentityGovernanceService idpManager) {
        TrialAccountDataHolder.getInstance().setIdentityGovernanceService(null);
        if (log.isDebugEnabled()) {
            log.debug("IdentityGovernanceService is un-set in the Trial Account bundle");
        }
    }
    @Reference(
            name = "IdentityGovernanceService",
            service = org.wso2.carbon.identity.governance.IdentityGovernanceService.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetIdentityGovernanceService")
    protected void setIdentityGovernanceService(IdentityGovernanceService idpManager) {
        TrialAccountDataHolder.getInstance().setIdentityGovernanceService(idpManager);
        if (log.isDebugEnabled()) {
            log.debug("IdentityGovernanceService is set in the Trial Account bundle");
        }
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

}
