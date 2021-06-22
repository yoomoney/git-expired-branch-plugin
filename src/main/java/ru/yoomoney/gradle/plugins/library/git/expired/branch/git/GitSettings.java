package ru.yoomoney.gradle.plugins.library.git.expired.branch.git;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Optional;

/**
 * Настройки для работы с git-репозиторием
 *
 * @author Oleg Kandaurov
 * @since 30.01.2019
 */
public class GitSettings {

    private final String username;
    private final String email;
    @Nullable
    private final String sshKeyPath;
    @Nullable
    private final String passphraseSshKey;

    private GitSettings(@Nonnull String username,
                        @Nonnull String email,
                        @Nullable String sshKeyPath,
                        @Nullable String passphraseSshKey) {
        this.username = Objects.requireNonNull(username, "username");
        this.email = Objects.requireNonNull(email, "email");
        this.sshKeyPath = sshKeyPath;
        this.passphraseSshKey = passphraseSshKey;
    }

    /**
     * Имя пользователя, проводящего операции с git
     *
     * @return имя пользователя
     */
    @Nonnull
    public String getUsername() {
        return username;
    }

    /**
     * Email пользователя, проводящего операции с git
     *
     * @return email пользователя
     */
    @Nonnull
    public String getEmail() {
        return email;
    }

    /**
     * Путь к ssh-ключу, если таковой был предоставлен
     */
    Optional<String> getSshKeyPath() {
        return Optional.ofNullable(sshKeyPath);
    }

    /**
     * Парольная фраза ssh ключа
     */
    Optional<String> getPassphraseSshKey() {
        return Optional.ofNullable(passphraseSshKey);
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Билдер для класса {@link GitSettings}
     */
    public static class Builder {
        private String username;
        private String email;
        private String sshKeyPath;
        private String passphraseSshKey;

        private Builder() {
        }

        public Builder withUsername(String username) {
            this.username = username;
            return this;
        }

        public Builder withEmail(String email) {
            this.email = email;
            return this;
        }

        public Builder withSshKeyPath(String sshKeyPath) {
            this.sshKeyPath = sshKeyPath;
            return this;
        }

        public Builder withPassphraseSshKey(String passphraseSshKey) {
            this.passphraseSshKey = passphraseSshKey;
            return this;
        }

        public GitSettings build() {
            return new GitSettings(username, email, sshKeyPath, passphraseSshKey);
        }
    }
}
