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

package org.sample.handler.trial.account.constants;

public class TrialAccountConstants {
    public static final String TRIAL_ACCOUNT_CLAIM = "http://wso2.org/claims/identity/trialAccount";
    public static final String TRIAL_ACCOUNT_EXPIRED_CLAIM = "http://wso2.org/claims/identity/trialAccountExpired";
    public static final String ACCOUNT_CREATED_TIME = "http://wso2.org/claims/created";

    public static final String TRIAL_ACCOUNT_PERIOD = "trialAccountHandler.trial.account.period";
    public static final String TRIAL_ACCOUNT_EXPIRY_ENABLED = "trialAccountHandler.trial.account.expiry.enabled";
    public static final String TRIAL_ACCOUNT_SUSPENSION_TRIGGER_TIME =
            "trialAccountHandler.trial.account.expiry.trigger.time";
    public static final String TRIGGER_TIME_FORMAT = "HH:mm:ss";
    public static final long SCHEDULER_DELAY = 24;
    public static final String TRIAL_ERROR_CODE = "91111";
}

