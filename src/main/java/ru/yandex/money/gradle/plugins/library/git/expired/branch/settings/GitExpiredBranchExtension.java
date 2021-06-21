package ru.yandex.money.gradle.plugins.library.git.expired.branch.settings;

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
    public String gitArchiveRepository = "ssh://git@bitbucket.yamoney.ru/backend-archive/branches-archive2.git";

    /**
     * Адрес почты от которого придет уведомление об устареших ветках
     */
    @Nullable
    public String notifierEmail = "bitbucket-stale-branch-notifier@yoomoney.ru";

    /**
     * Адрес почты от которого придет уведомление об удалении устаревших веток
     */
    @Nullable
    public String removerEmail = "bitbucket-stale-branch-delete@yoomoney.ru";

    /**
     * Адрес почты на который придет письмо при неуспехе отправки уведомлений
     */
    @Nullable
    public String adminEmail = "SvcReleaserBackend@yoomoney.ru";

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
        "^refs/remotes/origin/HEAD$",
        "^refs/remotes/origin/release/.*$"
    });
}
