package org.sample.handler.trial.account.util;

public class TrialAccountUser {
        private String username;
        private String firstName;
        private String expireDate;
        private String email;
        private String userStoreDomain;

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getExpireDate() {
            return expireDate;
        }

        public void setExpireDate(String expireDate) {
            this.expireDate = expireDate;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getFirstName() {
            return firstName;
        }

        public void setFirstName(String firstName) {
            this.firstName = firstName;
        }

        @Override public String toString() {
            return "Username: " + getUsername() + ", Email: " + getEmail() + ", FirstName: " + getFirstName()
                    + ", ExpireDate: " + getExpireDate();
        }

        public String getUserStoreDomain() { return userStoreDomain; }

        public void setUserStoreDomain(String userStoreDomain) { this.userStoreDomain = userStoreDomain; }
    }
