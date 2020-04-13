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

package org.sample.handler.trial.account;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sample.handler.trial.account.constants.TrialAccountConstants;
import org.sample.handler.trial.account.exceptions.TrialAccountException;
import org.sample.handler.trial.account.internal.TrialAccountDataHolder;
import org.sample.handler.trial.account.task.TrialAccountExpiryThread;
import org.wso2.carbon.identity.base.IdentityRuntimeException;
import org.wso2.carbon.identity.core.handler.InitConfig;
import org.wso2.carbon.identity.core.model.IdentityErrorMsgContext;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.identity.event.IdentityEventConstants;
import org.wso2.carbon.identity.event.IdentityEventException;
import org.wso2.carbon.identity.event.event.Event;
import org.wso2.carbon.identity.event.handler.AbstractEventHandler;
import org.wso2.carbon.identity.governance.IdentityGovernanceException;
import org.wso2.carbon.identity.governance.IdentityGovernanceUtil;
import org.wso2.carbon.identity.governance.common.IdentityConnectorConfig;
import org.wso2.carbon.user.core.UserCoreConstants;
import org.wso2.carbon.user.core.UserStoreException;
import org.wso2.carbon.user.core.UserStoreManager;
import org.wso2.carbon.user.core.util.UserCoreUtil;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class TrialAccountHandler extends AbstractEventHandler implements IdentityConnectorConfig {
    private static final Log log = LogFactory.getLog(TrialAccountHandler.class);

    @Override
    public String getName() {
        return "trialAccountHandler";
    }

    @Override
    public String getFriendlyName() {
        return "Trial Account Handling";
    }

    @Override
    public String getCategory() {
        return "Custom Login Policy";
    }

    @Override
    public String getSubCategory() {
        return "DEFAULT";
    }

    @Override
    public int getOrder() {
        return 0;
    }

    @Override
    public Map<String, String> getPropertyNameMapping() {
        Map<String, String> nameMapping = new HashMap<>();
        nameMapping.put(TrialAccountConstants.TRIAL_ACCOUNT_EXPIRY_ENABLED, "Enable");
        nameMapping.put(TrialAccountConstants.TRIAL_ACCOUNT_PERIOD, "Trial Period");
        nameMapping.put(TrialAccountConstants.TRIAL_ACCOUNT_SUSPENSION_TRIGGER_TIME, "Scheduler Trigger Time ");
        return nameMapping;
    }

    @Override
    public Map<String, String> getPropertyDescriptionMapping() {
        Map<String, String> nameMapping = new HashMap<>();
        nameMapping.put(TrialAccountConstants.TRIAL_ACCOUNT_EXPIRY_ENABLED, "Enable");
        nameMapping.put(TrialAccountConstants.TRIAL_ACCOUNT_PERIOD, "Trial Period");
        nameMapping.put(TrialAccountConstants.TRIAL_ACCOUNT_SUSPENSION_TRIGGER_TIME, "Scheduler Trigger Time ");
        return nameMapping;
    }

    @Override
    public String[] getPropertyNames() {
        List<String> properties = new ArrayList<>();
        properties.add(TrialAccountConstants.TRIAL_ACCOUNT_EXPIRY_ENABLED);
        properties.add(TrialAccountConstants.TRIAL_ACCOUNT_PERIOD);
        properties.add(TrialAccountConstants.TRIAL_ACCOUNT_SUSPENSION_TRIGGER_TIME);
        return properties.toArray(new String[properties.size()]);
    }

    @Override
    public Properties getDefaultPropertyValues(String s) {
        Map<String, String> defaultProperties = new HashMap<>();

        defaultProperties.put(TrialAccountConstants.TRIAL_ACCOUNT_EXPIRY_ENABLED,
                configs.getModuleProperties().getProperty(TrialAccountConstants.TRIAL_ACCOUNT_EXPIRY_ENABLED));

        defaultProperties.put(TrialAccountConstants.TRIAL_ACCOUNT_PERIOD,
                configs.getModuleProperties()
                        .getProperty(TrialAccountConstants.TRIAL_ACCOUNT_PERIOD));

        defaultProperties.put(TrialAccountConstants.TRIAL_ACCOUNT_SUSPENSION_TRIGGER_TIME,
                configs.getModuleProperties().getProperty(TrialAccountConstants.TRIAL_ACCOUNT_SUSPENSION_TRIGGER_TIME));

        Properties properties = new Properties();
        properties.putAll(defaultProperties);
        return properties;
    }

    @Override
    public Map<String, String> getDefaultPropertyValues(String[] strings, String s) throws IdentityGovernanceException {
        return null;
    }

    @Override
    public void init(InitConfig configuration) throws IdentityRuntimeException {
        if (log.isDebugEnabled()) {
            log.debug("Initiating Trial Account Handler");
        }
        super.init(configuration);
        try {
            if (StringUtils.isBlank(configs.getModuleProperties().
                    getProperty(TrialAccountConstants.TRIAL_ACCOUNT_SUSPENSION_TRIGGER_TIME))) {
                TrialAccountDataHolder.getInstance().setExpiryTriggerTime(configs.getModuleProperties().
                        getProperty(TrialAccountConstants.TRIAL_ACCOUNT_SUSPENSION_TRIGGER_TIME));
            }

            TrialAccountDataHolder.getInstance().setExpiryTriggerTime(configs.getModuleProperties().
                    getProperty(TrialAccountConstants.TRIAL_ACCOUNT_SUSPENSION_TRIGGER_TIME));
            TrialAccountDataHolder.getInstance().setIsTrialAccountEnabled(Boolean.parseBoolean(configs.
                    getModuleProperties().getProperty(TrialAccountConstants.TRIAL_ACCOUNT_EXPIRY_ENABLED)));
            TrialAccountDataHolder.getInstance().setTrialAccountPeriod(Integer.parseInt(configs.getModuleProperties().
                    getProperty(TrialAccountConstants.TRIAL_ACCOUNT_PERIOD)));
            startScheduler();
        } catch (Exception e) {
            e.printStackTrace();
        }
        TrialAccountDataHolder.getInstance().getBundleContext()
                .registerService(IdentityConnectorConfig.class.getName(), this, null);
    }

    @Override
    public void handleEvent(Event event) throws IdentityEventException, TrialAccountException {
        Map<String, Object> eventProperties = event.getEventProperties();
        String userName = (String) eventProperties.get(IdentityEventConstants.EventProperty.USER_NAME);
        UserStoreManager userStoreManager =
                (UserStoreManager) eventProperties.get(IdentityEventConstants.EventProperty.USER_STORE_MANAGER);
        String userStoreDomainName = IdentityGovernanceUtil.getUserStoreDomainName(userStoreManager);
        String tenantDomain = (String) eventProperties.get(IdentityEventConstants.EventProperty.TENANT_DOMAIN);

        String usernameWithDomain = UserCoreUtil.addDomainToName(userName, userStoreDomainName);
        boolean userExists;
        try {
            userExists = userStoreManager.isExistingUser(usernameWithDomain);
        } catch (UserStoreException e) {
            log.error("Error in checking User ", e);
            return;
        }
        if (!userExists) {
            return;
        }

        if (IdentityEventConstants.Event.PRE_AUTHENTICATION.equals(event.getEventName())) {
            if (isAuthPolicyAccountExistCheck() && !isUserExistsInDomain(userStoreManager, userName)) {
                IdentityErrorMsgContext customErrorMessageContext = new IdentityErrorMsgContext(UserCoreConstants
                        .ErrorCode.USER_DOES_NOT_EXIST);
                IdentityUtil.setIdentityErrorMsg(customErrorMessageContext);
            } else {
                if (isTrialAccount(userName, userStoreManager) && isTrialExpired(userName, userStoreManager)) {
                    String message;
                    if (StringUtils.isNotBlank(userStoreDomainName)) {
                        message = "Trial period has ended for user " + userName + " in user store "
                                + userStoreDomainName + " in tenant " + tenantDomain + ".";
                    } else {
                        message = "Trial period has ended for user " + userName + " in tenant " + tenantDomain + ".";
                    }
                    IdentityErrorMsgContext customErrorMessageContext =
                            new IdentityErrorMsgContext(TrialAccountConstants.TRIAL_ERROR_CODE);
                    IdentityUtil.setIdentityErrorMsg(customErrorMessageContext);
                    throw new TrialAccountException(TrialAccountConstants.TRIAL_ERROR_CODE, message);
                }
            }
        }
        return;
    }

    private boolean isAuthPolicyAccountExistCheck() {
        return Boolean.parseBoolean(IdentityUtil.getProperty("AuthenticationPolicy.CheckAccountExist"));
    }

    private boolean isUserExistsInDomain(UserStoreManager userStoreManager, String userName) throws
            TrialAccountException {

        boolean isExists = false;
        try {
            if (userStoreManager.isExistingUser(userName)) {
                isExists = true;
            }
        } catch (UserStoreException e) {
            throw new TrialAccountException("Error occurred while check user existence: " + userName, e);
        }
        return isExists;
    }

    protected boolean isTrialExpired(String userName, UserStoreManager userStoreManager) throws TrialAccountException {

        try {
            Map<String, String> values = userStoreManager.getUserClaimValues(userName, new String[]{
                    TrialAccountConstants.TRIAL_ACCOUNT_EXPIRED_CLAIM}, UserCoreConstants.DEFAULT_PROFILE);
            return Boolean.parseBoolean(values.get(TrialAccountConstants.TRIAL_ACCOUNT_EXPIRED_CLAIM));

        } catch (UserStoreException e) {
            throw new TrialAccountException("Error in checking claim : is trial expired",e);
        }
    }

    protected boolean isTrialAccount(String userName, UserStoreManager userStoreManager) throws TrialAccountException {
        try {
            Map<String, String> values = userStoreManager.getUserClaimValues(userName, new String[]{
                    TrialAccountConstants.TRIAL_ACCOUNT_CLAIM}, UserCoreConstants.DEFAULT_PROFILE);
            return Boolean.parseBoolean(values.get(TrialAccountConstants.TRIAL_ACCOUNT_CLAIM));

        } catch (UserStoreException e) {
            throw new TrialAccountException("Error in checking claim : is trial Account",e);
        }
    }

    public void startScheduler() {
        if (!Boolean.parseBoolean(configs.getModuleProperties().getProperty(TrialAccountConstants.
                TRIAL_ACCOUNT_EXPIRY_ENABLED))) {
            return;
        }
        if (log.isDebugEnabled()) {
            log.debug("Initiating Scheduler for trial account suspension");
        }
        Date notificationTriggerTime = null;
        String notificationTriggerTimeProperty = configs.getModuleProperties().getProperty(TrialAccountConstants.
                TRIAL_ACCOUNT_SUSPENSION_TRIGGER_TIME);

        DateFormat dateFormat = new SimpleDateFormat(TrialAccountConstants.TRIGGER_TIME_FORMAT);

        if (notificationTriggerTimeProperty != null) {
            try {
                notificationTriggerTime = dateFormat.parse(notificationTriggerTimeProperty);
            } catch (ParseException e) {
                log.error("Invalid Date format for Notification trigger time", e);
            }
        }

        long schedulerDelayInSeconds = TimeUnit.HOURS.toSeconds(TrialAccountConstants.SCHEDULER_DELAY);

        Calendar currentTime = Calendar.getInstance();
        Calendar triggerTime = Calendar.getInstance();
        // If notificationTriggerTimeProperty is not found or not in right format default to 20:00:00.
        // In Calender.HOUR_OF_DAY (i.e. in 24-hour clock) it is 20.
        if (notificationTriggerTime != null) {
            triggerTime.setTime(notificationTriggerTime);
        } else {
            triggerTime.set(Calendar.HOUR_OF_DAY, 20);
            triggerTime.set(Calendar.MINUTE, 0);
            triggerTime.set(Calendar.SECOND, 0);
        }


        // Convert times into seconds
        long currentSecond =
                (currentTime.get(Calendar.HOUR_OF_DAY) * 3600) + currentTime.get(Calendar.MINUTE) * 60 + currentTime
                        .get(Calendar.SECOND);
        long triggerSecond =
                (triggerTime.get(Calendar.HOUR_OF_DAY) * 3600) + triggerTime.get(Calendar.MINUTE) * 60 + triggerTime
                        .get(Calendar.SECOND);
        long delay = triggerSecond - currentSecond;
        // If the notification time has passed, schedule the next day
        if (delay < 0) {
            delay += schedulerDelayInSeconds;
        }

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(TrialAccountDataHolder.getInstance().
                getTrialAccountSuspentionThreadPoolSize());
        if (log.isDebugEnabled()) {
            log.debug("Trial Account suspension scheduled to execute in : " + delay + " Scheduler delay : " +
                    schedulerDelayInSeconds);
        }
        scheduler.scheduleAtFixedRate(new TrialAccountExpiryThread(), delay, schedulerDelayInSeconds, TimeUnit.SECONDS);
    }
}
