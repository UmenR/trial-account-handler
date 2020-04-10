package org.sample.handler.trial.account.task;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sample.handler.trial.account.exceptions.TrialAccountException;
import org.sample.handler.trial.account.internal.TrialAccountDataHolder;
import org.sample.handler.trial.account.util.TrialAccountUser;
import org.sample.handler.trial.account.util.TrialAccountUserRetrivalUtil;

import java.text.ParseException;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class TrialAccountRetrievalManager {
    private static final Log log = LogFactory.getLog(TrialAccountRetrievalManager.class);

    public static List<TrialAccountUser> getReceivers(long trialPeriod, String tenantDomain)
            throws TrialAccountException {

        Set<String> userStoreDomains = TrialAccountUserRetrivalUtil.
                getSuspensionNotificationEnabledUserStores(tenantDomain);

        List<TrialAccountUser> receivers = new ArrayList<>();

        for (String userStoreDomain : userStoreDomains) {
            if (log.isDebugEnabled()) {
                log.debug("Idle account suspension task enabled for user store: " + userStoreDomain + " in tenant: "
                        + tenantDomain);
            }
            TrialAccountRetriver trialAccountRetriver = TrialAccountUserRetrivalUtil
                    .getTrialAccountRetriversForDomain(userStoreDomain, tenantDomain);
            if (trialAccountRetriver != null) {
                long MinTrialPeriodinMilis;
                long MaxTrialPeriodinMilis;
                try {
                    MinTrialPeriodinMilis = getCurrentExecutionTime(TrialAccountDataHolder.getInstance().
                            getExpiryTriggerTime()).getTimeInMillis() - TimeUnit.DAYS.toMillis(trialPeriod);
                    MaxTrialPeriodinMilis = MinTrialPeriodinMilis + TimeUnit.DAYS.toMillis(2) - 1;
                } catch (ParseException e) {
                    throw new TrialAccountException("Error occurred while reading notification "
                            + "trigger time", e);
                }
                List<TrialAccountUser> newReceivers = trialAccountRetriver
                        .getTrialAccounts(MinTrialPeriodinMilis,MaxTrialPeriodinMilis, tenantDomain);
                receivers.addAll(newReceivers);
            }
        }
        return receivers;
    }

    private static Calendar getCurrentExecutionTime(Date triggerTime) {

        Calendar tr = Calendar.getInstance();
        tr.setTime(triggerTime);

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, tr.get(Calendar.HOUR_OF_DAY));
        calendar.set(Calendar.MINUTE, tr.get(Calendar.MINUTE));
        calendar.set(Calendar.SECOND, tr.get(Calendar.SECOND));
        calendar.set(Calendar.MILLISECOND, calendar.getActualMinimum(Calendar.MILLISECOND));
        return calendar;
    }


}
