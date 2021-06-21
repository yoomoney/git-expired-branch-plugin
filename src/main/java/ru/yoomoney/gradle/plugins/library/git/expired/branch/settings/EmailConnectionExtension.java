package ru.yoomoney.gradle.plugins.library.git.expired.branch.settings;

/**
 * Настройки подключения к Email шлюзу
 */
@SuppressWarnings({"WeakerAccess", "PublicField"})
public class EmailConnectionExtension {

    /**
     * Хост шлюза для отправки email
     */
    public String emailHost;

    /**
     * Порт шлюза для отправки email
     */
    public int emailPort;

    /**
     * Пользователь для авторзации в email шлюзе
     */
    public String emailAuthUser;

    /**
     * Пароль для авторзации в email шлюзе
     */
    public String emailAuthPassword;

    /**
     * Конструктор
     */
    @SuppressWarnings("CallToSystemGetenv")
    public EmailConnectionExtension() {
        emailAuthUser = System.getenv("EMAIL_USER");
        emailAuthPassword = System.getenv("EMAIL_PASSWORD");
    }
}
