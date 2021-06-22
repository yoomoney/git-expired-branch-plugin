package ru.yoomoney.gradle.plugins.library.git.expired.branch;

import org.eclipse.jgit.lib.Ref;
import org.gradle.api.DefaultTask;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.api.tasks.TaskAction;
import ru.yoomoney.gradle.plugins.library.git.expired.branch.git.GitSettings;
import ru.yoomoney.gradle.plugins.library.git.expired.branch.notification.MailSender;
import ru.yoomoney.gradle.plugins.library.git.expired.branch.settings.EmailConnectionSettings;
import ru.yoomoney.gradle.plugins.library.git.expired.branch.settings.GitExpiredBranchSettings;

import java.io.File;
import java.util.List;

import static java.util.stream.Collectors.toList;

/**
 * Задача для удаления бранчей, которые не используются и могут быть автоматически удалены
 */
class GitExpiredBranchDeleteTask extends DefaultTask {
    /**
     * Логгер
     */
    private final Logger log = Logging.getLogger(GitExpiredBranchDeleteTask.class);

    /**
     * Имя таски
     */
    static final String TASK_NAME = "removeExpiredGitBranches";

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


    @SuppressWarnings("PublicConstructorInNonPublicClass")
    public GitExpiredBranchDeleteTask() {
    }

    /**
     * Удаляет ветки
     */
    @TaskAction
    void removeExpiredGitBranches() {
        File repoDir = expiredBranchSettings.getRepoDir();
        MailSender mailSender = new MailSender(emailConnectionSettings);

        GitExpiredBranchRemover gitExpiredBranchRemover = new GitExpiredBranchRemover(
                expiredBranchSettings,
                mailSender,
                gitSettings,
                repoDir,
                getProject().getName()
        );

        log.lifecycle("Collecting data for deletion");
        List<BranchInfo> staleBranchesInfo = gitExpiredBranchRemover.collectGitStaleBranchesInfoToDelete();
        List<String> staleBranchesName = staleBranchesInfo.stream().map(BranchInfo::getBranch).map(Ref::getName).collect(toList());
        log.lifecycle("Found stale branches={}", staleBranchesName);

        log.lifecycle("Deleting branches ");
        List<BranchInfo> deletedBranches = gitExpiredBranchRemover.deleteStaleBranches(staleBranchesInfo);

        log.lifecycle("Notifying commiters about deletion");
        gitExpiredBranchRemover.notifyAboutDeletedBranches(deletedBranches);
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