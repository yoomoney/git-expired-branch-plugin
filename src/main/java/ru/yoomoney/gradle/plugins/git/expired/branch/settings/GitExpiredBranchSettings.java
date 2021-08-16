package ru.yoomoney.gradle.plugins.git.expired.branch.settings;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.regex.Pattern;

/**
 * Настройки плагина
 */
public class GitExpiredBranchSettings {
    private final File repoDir;
    private final String gitArchiveRepository;
    private final String notifierEmail;
    private final String removerEmail;
    private final String adminEmail;
    private final long staleDaysToNotify;
    private final long staleDaysToDelete;
    private final Collection<Pattern> ignoreBranches;

    private GitExpiredBranchSettings(
            File repoDir,
            String gitArchiveRepository,
            String notifierEmail,
            String removerEmail,
            String adminEmail,
            long staleDaysToNotify,
            long staleDaysToDelete,
            Collection<Pattern> ignoreBranches
    ) {
        this.repoDir = repoDir;
        this.gitArchiveRepository = gitArchiveRepository;
        this.notifierEmail = notifierEmail;
        this.removerEmail = removerEmail;
        this.adminEmail = adminEmail;
        this.staleDaysToNotify = staleDaysToNotify;
        this.staleDaysToDelete = staleDaysToDelete;
        this.ignoreBranches = ignoreBranches;
    }

    public File getRepoDir() {
        return repoDir;
    }

    public String getGitArchiveRepository() {
        return gitArchiveRepository;
    }

    public String getNotifierEmail() {
        return notifierEmail;
    }

    public String getRemoverEmail() {
        return removerEmail;
    }

    public String getAdminEmail() {
        return adminEmail;
    }

    public long getStaleDaysToNotify() {
        return staleDaysToNotify;
    }

    public long getStaleDaysToDelete() {
        return staleDaysToDelete;
    }

    public Collection<Pattern> getIgnoreBranches() {
        return ignoreBranches;
    }

    /**
     * Билдер
     */
    public static class Builder {
        /**
         * Локальная папка с репозиторием
         */
        private File repoDir;

        /**
         * Репозиторий для архивации диффов
         */
        private String gitArchiveRepository;

        /**
         * Адрес почты от которого придет уведомление об устареших ветках
         */
        private String notifierEmail;

        /**
         * Адрес почты от которого придет уведомление об удалении устаревших веток
         */
        private String removerEmail;

        /**
         * Адрес почты на который придет письмо при неуспехе отправки уведомлений
         */
        private String adminEmail;

        /**
         * Количество дней по истечении которых начинаем пинговать авторов веток,
         * если в ветках не было коммитов
         */
        private long staleDaysToNotify;

        /**
         * Количество дней по истечении которых ветка удаляется,
         * а дифф переносится в архивный репозиторий
         */
        private long staleDaysToDelete;

        /**
         * Список паттернов веток, которые не нужно удалять
         */
        private Collection<String> ignoreBranchesPatterns;

        public Builder withRepoDir(File repoDir) {
            this.repoDir = repoDir;
            return this;
        }

        public Builder withGitArchiveRepository(String gitArchiveRepository) {
            this.gitArchiveRepository = gitArchiveRepository;
            return this;
        }

        public Builder withRemoverEmail(String removerEmail) {
            this.removerEmail = removerEmail;
            return this;
        }

        public Builder withNotifierEmail(String notifierEmail) {
            this.notifierEmail = notifierEmail;
            return this;
        }

        public Builder withAdminEmail(String adminEmail) {
            this.adminEmail = adminEmail;
            return this;
        }

        public Builder withStaleDaysToNotify(long staleDaysToNotify) {
            this.staleDaysToNotify = staleDaysToNotify;
            return this;
        }

        public Builder withStaleDaysToDelete(long staleDaysToDelete) {
            this.staleDaysToDelete = staleDaysToDelete;
            return this;
        }

        public Builder withIgnoreBranchesPatterns(Collection<String> ignoreBranchesPatterns) {
            this.ignoreBranchesPatterns = ignoreBranchesPatterns;
            return this;
        }

        private Collection<Pattern> compilePatterns(Collection<String> patterns) {
            Collection<Pattern> result = new ArrayList<>();

            for (String pattern : patterns) {
                result.add(Pattern.compile(pattern));
            }

            return result;
        }

        /**
         * Создает экземпляр GitExpiredBranchSettings
         */
        public GitExpiredBranchSettings build() {
            Collection<Pattern> ignoreBranches = compilePatterns(ignoreBranchesPatterns);

            return new GitExpiredBranchSettings(
                repoDir,
                gitArchiveRepository,
                notifierEmail,
                removerEmail,
                adminEmail,
                staleDaysToNotify,
                staleDaysToDelete,
                ignoreBranches
            );
        }
    }
}
