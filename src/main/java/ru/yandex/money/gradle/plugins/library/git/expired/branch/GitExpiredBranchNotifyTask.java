package ru.yandex.money.gradle.plugins.library.git.expired.branch;

import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Ref;
import org.gradle.api.DefaultTask;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.api.tasks.TaskAction;
import ru.yandex.money.gradle.plugins.library.git.expired.branch.helper.GitManager;
import ru.yandex.money.gradle.plugins.library.git.expired.branch.notification.MailSender;
import ru.yandex.money.gradle.plugins.library.git.expired.branch.settings.EmailConnectionSettings;
import ru.yandex.money.gradle.plugins.library.git.expired.branch.settings.GitExpiredBranchSettings;
import ru.yandex.money.tools.git.GitSettings;

import java.text.Format;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Задача для уведомления пользователей о бранчах, которые не используются
 */
public class GitExpiredBranchNotifyTask extends DefaultTask {
    /**
     * Количество миллисекунд в секундах
     */
    private static final long MILLIS_IN_SECOND = 1000L;

    /**
     * Логгер
     */
    private final Logger log = Logging.getLogger(GitExpiredBranchNotifyTask.class);

    /**
     * Имя таски
     */
    public static final String TASK_NAME = "notifyAboutGitExpiredBranches";

    /**
     * Настройки плагина
     */
    private GitExpiredBranchSettings expiredBranchSettings;

    /**
     * Настройки smtp
     */
    private EmailConnectionSettings emailConnectionSettings;

    /**
     * Настройки git
     */
    private GitSettings gitSettings;

    /**
     * Отправляет нотификацию
     */
    @TaskAction
    void notifyAboutGitExpiredBranches() {
        GitManager gitManager = new GitManager(expiredBranchSettings.getRepoDir(), gitSettings);

        log.lifecycle("Collecting notification data");
        Map<PersonIdent, Set<BranchInfo>> staleBranchesInfo = collectGitStaleBranchesInfo(
                gitManager,
                expiredBranchSettings.getStaleDaysToNotify(),
                expiredBranchSettings.getIgnoreBranches()
        );
        log.lifecycle("Notifying about git expired branches");
        notifyAboutStaleBranches(staleBranchesInfo, gitManager, expiredBranchSettings.getStaleDaysToNotify());
    }

    private static Map<PersonIdent, Set<BranchInfo>> collectGitStaleBranchesInfo(
            GitManager gitManager,
            long staleDaysCount,
            Collection<Pattern> ignoreBranches
    ) {
        long staleTimeSeconds = LocalDateTime.now().minusDays(staleDaysCount).atZone(ZoneId.systemDefault()).toEpochSecond();

        return gitManager.getRemoteBranches().stream()
                .filter(branch -> isBranchNotIgnored(branch, ignoreBranches))
                .map(repoBranch -> new BranchInfo(repoBranch, gitManager.getLastCommitFromBranch(repoBranch)))
                .filter(branchInfo -> branchInfo.getLastCommit().getCommitTime() < staleTimeSeconds)
                .collect(Collectors.groupingBy(branchInfo -> branchInfo.getLastCommit().getAuthorIdent(),
                        Collectors.mapping(Function.identity(), Collectors.toSet())
                ));
    }

    private static boolean isBranchNotIgnored(Ref branch, Collection<Pattern> ignoreBranches) {
        for (Pattern ignoreBranchPattern : ignoreBranches) {
            if (ignoreBranchPattern.matcher(branch.getName()).matches()) {
                return false;
            }
        }
        return true;
    }

    private void notifyAboutStaleBranches(
            Map<PersonIdent,
            Set<BranchInfo>> staleBranchesInfo,
            GitManager gitManager,
            long staleDaysCount
    ) {
        MailSender mailSender = new MailSender(emailConnectionSettings);
        log.info("Send notification to {}", staleBranchesInfo.keySet().stream()
                .map(PersonIdent::getEmailAddress).collect(Collectors.joining(", ")));
        staleBranchesInfo.forEach((person, setOfBranchInfo) -> {
            String repositoryName = gitManager.getRepositoryName();
            String mailSubject = getStaleNotificationMailSubject(repositoryName);
            String mailBody = getStaleNotificationMailBody(person, setOfBranchInfo, gitManager, repositoryName, staleDaysCount);
            try {
                mailSender.sendEmail(
                        expiredBranchSettings.getNotifierEmail(),
                        Collections.singleton(person.getEmailAddress()),
                        mailSubject,
                        mailBody
                );
            } catch (Exception e) {
                mailSender.sendEmail(
                        expiredBranchSettings.getNotifierEmail(),
                        Collections.singleton(expiredBranchSettings.getAdminEmail()),
                        mailSubject,
                        mailBody + "/n" + e.getMessage()
                );
            }
        });
    }

    private static String getStaleNotificationMailBody(
            PersonIdent person,
            Set<BranchInfo> setOfBranchInfo,
            GitManager gitManager,
            String repositoryName,
            long staleDaysCount
    ) {
        StringBuilder builder = new StringBuilder();
        builder.append("Добрый день, уважаемый коллега ").append(person.getName()).append("!\n\n");
        builder.append("В проекте ").append(repositoryName)
                .append(" найдены ветки, в которые вы не делали коммиты целых ").append(staleDaysCount).append(" дней!\n");
        builder.append("Пожалуйста обновите (merge, rebase) или удалите эти ветки.\n");

        builder.append(setOfBranchInfo.size() > 1 ? "Ветки " : "Ветка ")
                .append(gitManager.getBitbucketProjectBranchesUrl()).append(":\n");
        Format formatter = new SimpleDateFormat("dd.MM.yyyy hh:mm");
        setOfBranchInfo.stream()
                .sorted(Comparator.comparingInt(branch -> branch.getLastCommit().getCommitTime()))
                .forEach(branchInfo -> builder
                .append("\t* ").append(getBranchUrl(branchInfo.getBranch().getName())).append("\n\t  последний коммит: ")
                .append(formatter.format(new Date(branchInfo.getLastCommit().getCommitTime() * MILLIS_IN_SECOND)))
                .append(", ").append(branchInfo.getLastCommit().getShortMessage()).append('\n'));

        return builder.toString();
    }

    private static String getBranchUrl(String name) {
        return name.replaceAll("origin/", "");
    }

    private static String getStaleNotificationMailSubject(String repositoryName) {
        return "Напоминание об устаревших бранчах " + repositoryName;
    }

    void setExpiredBranchSettings(GitExpiredBranchSettings expiredBranchSettings) {
        this.expiredBranchSettings = expiredBranchSettings;
    }

    void setEmailConnectionSettings(EmailConnectionSettings emailConnectionSettings) {
        this.emailConnectionSettings = emailConnectionSettings;
    }

    void setGitSettings(GitSettings gitSettings) {
        this.gitSettings = gitSettings;
    }

}