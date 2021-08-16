package ru.yoomoney.gradle.plugins.git.expired.branch.settings;

/**
 * Настройки подключения к хосту с Email
 *
 * @author Vasily Sozykin
 *         Date: 14.03.2017.
 */
public class EmailConnectionSettings {
    private final String emailHost;
    private final Integer emailPort;
    private final String emailAuthUser;
    private final String emailAuthPassword;

    private EmailConnectionSettings(String emailHost, Integer emailPort, String emailAuthUser, String emailAuthPassword) {
        this.emailHost = emailHost;
        this.emailPort = emailPort;
        this.emailAuthUser = emailAuthUser;
        this.emailAuthPassword = emailAuthPassword;
    }

    public String getEmailHost() {
        return emailHost;
    }

    public Integer getEmailPort() {
        return emailPort;
    }

    public String getEmailAuthUser() {
        return emailAuthUser;
    }

    public String getEmailAuthPassword() {
        return emailAuthPassword;
    }

    /**
     * Билдер
     */
    @SuppressWarnings("PackageVisibleField")
    public static class Builder {
        /**
         * Хост
         */
        String emailHost;
        /**
         * Порт
         */
        Integer emailPort;
        /**
         * Логин
         */
        String emailAuthUser;
        /**
         * Пароль
         */
        String emailAuthPassword;

        public Builder withEmailHost(String emailHost) {
            this.emailHost = emailHost;
            return this;
        }

        public Builder withEmailPort(Integer emailPort) {
            this.emailPort = emailPort;
            return this;
        }

        public Builder withEmailAuthUser(String emailAuthUser) {
            this.emailAuthUser = emailAuthUser;
            return this;
        }

        public Builder withEmailAuthPassword(String emailAuthPassword) {
            this.emailAuthPassword = emailAuthPassword;
            return this;
        }

        public EmailConnectionSettings build() {
            return new EmailConnectionSettings(emailHost, emailPort, emailAuthUser, emailAuthPassword);
        }
    }
}