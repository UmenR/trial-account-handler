package org.sample.handler.trial.account;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sample.handler.trial.account.constants.TrialAccountConstants;
import org.sample.handler.trial.account.exceptions.TrialAccountException;
import org.sample.handler.trial.account.internal.TrialAccountDataHolder;
import org.sample.handler.trial.account.task.TrialAccountExpiryThread;
import org.sample.handler.trial.account.util.TrialAccountUtil;
import org.wso2.carbon.identity.base.IdentityRuntimeException;
import org.wso2.carbon.identity.core.handler.InitConfig;
import org.wso2.carbon.identity.core.model.IdentityErrorMsgContext;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.identity.event.IdentityEventConstants;
import org.wso2.carbon.identity.event.IdentityEventException;
import org.wso2.carbon.identity.event.event.Event;
import org.wso2.carbon.identity.event.handler.AbstractEventHandler;
import org.wso2.carbon.identity.governance.IdentityGovernanceException;
import org.wso2.carbon.identity.governance.common.IdentityConnectorConfig;
import org.wso2.carbon.user.core.UserCoreConstants;
import org.wso2.carbon.user.core.UserStoreException;
import org.wso2.carbon.user.core.UserStoreManager;
import org.wso2.carbon.user.core.util.UserCoreUtil;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class TrialAccountHandler extends AbstractEventHandler {
    private static final Log log = LogFactory.getLog(TrialAccountHandler.class);
    @Override
    public String getName() {
        return "trialAccountHandler";
    }

//    @Override
//    public String getFriendlyName() {
//        return "Trial Account Handling";
//    }
//
//    @Override
//    public String getCategory() {
//        return "Login Policies";
//    }
//
//    @Override
//    public String getSubCategory() {
//        return "DEFAULT";
//    }
//
//    @Override
//    public int getOrder() {
//        return 0;
//    }
//
//    @Override
//    public Map<String, String> getPropertyNameMapping() {
//        return null;
//    }
//
//    @Override
//    public Map<String, String> getPropertyDescriptionMapping() {
//        return null;
//    }
//
//    @Override
//    public String[] getPropertyNames() {
//        return new String[0];
//    }
//
//    @Override
//    public Properties getDefaultPropertyValues(String s) throws IdentityGovernanceException {
//        return null;
//    }
//
//    @Override
//    public Map<String, String> getDefaultPropertyValues(String[] strings, String s) throws IdentityGovernanceException {
//        return null;
//    }

    @Override
    public void init(InitConfig configuration) throws IdentityRuntimeException {
        log.info("INIT trial account handler");
        super.init(configuration);

        if (StringUtils.isBlank(configs.getModuleProperties().
                getProperty(TrialAccountConstants.TRIAL_ACCOUNT_SUSPENSION_TRIGGER_TIME))) {
            TrialAccountDataHolder.getInstance().setExpiryTriggerTime(configs.getModuleProperties().
                    getProperty(TrialAccountConstants.TRIAL_ACCOUNT_SUSPENSION_TRIGGER_TIME));
        }

        TrialAccountDataHolder.getInstance().setExpiryTriggerTime(configs.getModuleProperties().
                getProperty(TrialAccountConstants.TRIAL_ACCOUNT_SUSPENSION_TRIGGER_TIME));
        log.info("starting trial account scheduler");
        startScheduler();
        TrialAccountDataHolder.getInstance().getBundleContext()
                .registerService(IdentityConnectorConfig.class.getName(), this, null);
    }

    @Override
    public void handleEvent(Event event) throws IdentityEventException,TrialAccountException {
        log.info("TRIAL ACCOUNT HANDLER INITIATING");
        Map<String, Object> eventProperties = event.getEventProperties();
        String userName = (String) eventProperties.get(IdentityEventConstants.EventProperty.USER_NAME);
        UserStoreManager userStoreManager = (UserStoreManager) eventProperties.get(IdentityEventConstants.EventProperty.USER_STORE_MANAGER);
        String userStoreDomainName = TrialAccountUtil.getUserStoreDomainName(userStoreManager);
        String tenantDomain = (String) eventProperties.get(IdentityEventConstants.EventProperty.TENANT_DOMAIN);

        String usernameWithDomain = UserCoreUtil.addDomainToName(userName, userStoreDomainName);
        boolean userExists;
        try {
            userExists = userStoreManager.isExistingUser(usernameWithDomain);
        } catch (UserStoreException e) {
            throw new IdentityEventException("Error in accessing user store", e);
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
                    if(isTrialAccount(userName,userStoreManager) && !isTrialExpired(userName,userStoreManager)) {
                        String message;
                        if (StringUtils.isNotBlank(userStoreDomainName)) {
                            message = "Trial period has ended for user " + userName + " in user store "
                                    + userStoreDomainName + " in tenant " + tenantDomain + ".";
                        } else {
                            message = "Trial period has ended for user " + userName + " in tenant " + tenantDomain + ".";
                        }
//                      TODO Error message
                        IdentityErrorMsgContext customErrorMessageContext = new IdentityErrorMsgContext(UserCoreConstants.ErrorCode.USER_IS_LOCKED);
                        IdentityUtil.setIdentityErrorMsg(customErrorMessageContext);
                        throw new TrialAccountException(UserCoreConstants.ErrorCode.USER_IS_LOCKED, message);
                    }
            }
        }
    }

    private boolean isAuthPolicyAccountExistCheck() {
        return Boolean.parseBoolean(IdentityUtil.getProperty("AuthenticationPolicy.CheckAccountExist"));
    }

    private boolean isUserExistsInDomain(UserStoreManager userStoreManager, String userName) throws TrialAccountException {

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

        String trialExpiredClaim;
        try {
            Map<String, String> values = userStoreManager.getUserClaimValues(userName, new String[]{
                    TrialAccountConstants.TRIAL_ACCOUNT_EXPIRED_CLAIM}, UserCoreConstants.DEFAULT_PROFILE);
            trialExpiredClaim = values.get(TrialAccountConstants.TRIAL_ACCOUNT_EXPIRED_CLAIM);

        } catch (UserStoreException e) {
            throw new TrialAccountException("Error occurred while retrieving " + TrialAccountConstants
                    .TRIAL_ACCOUNT_EXPIRED_CLAIM + " claim value", e);
        }
        return Boolean.parseBoolean(trialExpiredClaim);
    }

    protected boolean isTrialAccount(String userName, UserStoreManager userStoreManager) throws TrialAccountException {

        String trialAccountClaim;
        try {
            Map<String, String> values = userStoreManager.getUserClaimValues(userName, new String[]{
                    TrialAccountConstants.TRIAL_ACCOUNT_CLAIM}, UserCoreConstants.DEFAULT_PROFILE);
            trialAccountClaim = values.get(TrialAccountConstants.TRIAL_ACCOUNT_CLAIM);

        } catch (UserStoreException e) {
            throw new TrialAccountException("Error occurred while retrieving " + TrialAccountConstants
                    .TRIAL_ACCOUNT_CLAIM + " claim value", e);
        }
        return Boolean.parseBoolean(trialAccountClaim);
    }

    public void startScheduler(){
//        if(!Boolean.parseBoolean(configs.getModuleProperties().getProperty(NotificationConstants.
//                SUSPENSION_NOTIFICATION_ENABLED))) {
//            return;
//        }
        log.info("SCHEDULER STARTEDD!!!");
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
        log.info("TRIAL ACCOUNT delay : " + delay + "schedulerDelayInSeconds : " + schedulerDelayInSeconds);
        scheduler.scheduleAtFixedRate(new TrialAccountExpiryThread(), delay, schedulerDelayInSeconds, TimeUnit.SECONDS);
    }
}
