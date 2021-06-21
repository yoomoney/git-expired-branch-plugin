package ru.yoomoney.gradle.plugins.library.git.expired.branch.settings;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static java.util.Objects.requireNonNull;

/**
 * Настройки подключения к Git
 *
 * @author horyukova
 * @since 22.04.2019
 */
public class GitConnectionExtension {
    /**
     *  Путь до приватного ssh ключа для доступа в git
     */
    private String pathToGitPrivateSshKey;

    @Nullable
    public String getPathToGitPrivateSshKey() {
        return pathToGitPrivateSshKey;
    }

    public void setPathToGitPrivateSshKey(String pathToGitPrivateSshKey) {
        this.pathToGitPrivateSshKey = pathToGitPrivateSshKey;
    }

    /**
     *  Парольная фраза ssh ключа для доступа в git
     */
    private String passphraseSshKey;

    @Nullable
    public String getPassphraseSshKey() {
        return passphraseSshKey;
    }

    public void setPassphraseSshKey(String passphraseSshKey) {
        this.passphraseSshKey = passphraseSshKey;
    }

    /**
     *  Пользователь, от имени которого будет производиться коммит в гит
     */
    @Nonnull
    private String username;

    public String getUsername() {
        return username;
    }

    public void setUsername(@Nonnull String username) {
        this.username = requireNonNull(username);
    }

    /**
     *  Email пользователя, от имени которого будет производиться коммит в гит
     */
    @Nonnull
    private String email;

    public String getEmail() {
        return email;
    }

    public void setEmail(@Nonnull String email) {
        this.email = requireNonNull(email);
    }
}
