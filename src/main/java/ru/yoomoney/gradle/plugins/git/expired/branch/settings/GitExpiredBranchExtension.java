package ru.yoomoney.gradle.plugins.git.expired.branch.settings;

import javax.annotation.Nullable;
import java.io.File;
import java.util.Arrays;
import java.util.Collection;

/**
 * Настройки плагина
 */
@SuppressWarnings({"WeakerAccess", "PublicField"})
public class GitExpiredBranchExtension {
    /**
     * Локальная папка с репозиторием
     */
    @Nullable
    public File repoDir = null;

    /**
     * Репозиторий для архивации диффов
     */
    @Nullable
    public String gitArchiveRepository;

    /**
     * Адрес почты от которого придет уведомление об устареших ветках
     */
    @Nullable
    public String notifierEmail;

    /**
     * Адрес почты от которого придет уведомление об удалении устаревших веток
     */
    @Nullable
    public String removerEmail;

    /**
     * Адрес почты на который придет письмо при неуспехе отправки уведомлений
     */
    @Nullable
    public String adminEmail;

    /**
     * Количество дней по истечении которых начинаем пинговать авторов веток,
     * если в ветках не было коммитов
     */
    @Nullable
    public long staleDaysToNotify = 30L;

    /**
     * Количество дней по истечении которых ветка удаляется,
     * а дифф переносится в архивный репозиторий
     */
    @Nullable
    public long staleDaysToDelete = 60L;

    /**
     * Список паттернов веток, которые не нужно удалять
     */
    @Nullable
    public Collection<String> ignoreBranchesPatterns = Arrays.asList(new String[]{
        "^refs/remotes/origin/dev$",
        "^refs/remotes/origin/master$",
        "^refs/remotes/origin/HEAD$"
    });
}
