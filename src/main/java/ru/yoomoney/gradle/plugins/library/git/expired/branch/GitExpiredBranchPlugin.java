package ru.yoomoney.gradle.plugins.library.git.expired.branch;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import ru.yoomoney.gradle.plugins.library.git.expired.branch.git.GitSettings;
import ru.yoomoney.gradle.plugins.library.git.expired.branch.settings.EmailConnectionExtension;
import ru.yoomoney.gradle.plugins.library.git.expired.branch.settings.EmailConnectionSettings;
import ru.yoomoney.gradle.plugins.library.git.expired.branch.settings.GitConnectionExtension;
import ru.yoomoney.gradle.plugins.library.git.expired.branch.settings.GitExpiredBranchExtension;
import ru.yoomoney.gradle.plugins.library.git.expired.branch.settings.GitExpiredBranchSettings;

/**
 * Плагин для работы с брошенными ветками git
 */
public class GitExpiredBranchPlugin implements Plugin<Project> {

    /**
     * Действия при применении плагина GitExpiredBranchPlugin
     *
     * @param target проект, для которого применяется плагин
     */
    @Override
    public void apply(Project target) {
        GitExpiredBranchNotifyTask gitExpiredBranchNotifyTask = createGitStaleBranchNotifyTask(target);
        GitExpiredBranchDeleteTask gitExpiredBranchDeleteTask = createGitStaleBranchDeleteTask(target);

        EmailConnectionExtension emailConnection = target.getExtensions()
                .create("emailForGitExpiredBranches", EmailConnectionExtension.class);

        GitConnectionExtension gitConnection = target.getExtensions()
                .create("gitForGitExpiredBranches", GitConnectionExtension.class);

        GitExpiredBranchExtension expiredBranch = target.getExtensions()
                .create("expiredBranchSettings", GitExpiredBranchExtension.class);

        target.afterEvaluate(project -> {
            GitExpiredBranchSettings expiredBranchSettings = createGitExpiredBranchSettings(expiredBranch, target);
            EmailConnectionSettings emailConnectionSettings = createEmailConnectionSettings(emailConnection);
            GitSettings gitSettings = createGitSettings(gitConnection);

            gitExpiredBranchNotifyTask.setEmailConnectionSettings(emailConnectionSettings);
            gitExpiredBranchNotifyTask.setExpiredBranchSettings(expiredBranchSettings);
            gitExpiredBranchNotifyTask.setGitSettings(gitSettings);

            gitExpiredBranchDeleteTask.setEmailConnectionSettings(emailConnectionSettings);
            gitExpiredBranchDeleteTask.setExpiredBranchSettings(expiredBranchSettings);
            gitExpiredBranchDeleteTask.setGitSettings(gitSettings);
        });
    }

    private static GitExpiredBranchNotifyTask createGitStaleBranchNotifyTask(Project target) {
        GitExpiredBranchNotifyTask task = target.getTasks()
                .create(GitExpiredBranchNotifyTask.TASK_NAME, GitExpiredBranchNotifyTask.class);
        task.setGroup("report");
        task.setDescription("Make notifications about git expired branches");
        return task;
    }

    private GitExpiredBranchDeleteTask createGitStaleBranchDeleteTask(Project target) {
        GitExpiredBranchDeleteTask task = target.getTasks()
                .create(GitExpiredBranchDeleteTask.TASK_NAME, GitExpiredBranchDeleteTask.class);
        task.setGroup("build");
        task.setDescription("Delete git expired branches");
        return task;
    }

    private static GitSettings createGitSettings(GitConnectionExtension gitConnection) {
        return GitSettings.builder()
            .withSshKeyPath(gitConnection.getPathToGitPrivateSshKey())
            .withPassphraseSshKey(gitConnection.getPassphraseSshKey())
            .withUsername(gitConnection.getUsername())
            .withEmail(gitConnection.getEmail())
            .build();
    }

    private static EmailConnectionSettings createEmailConnectionSettings(EmailConnectionExtension emailConnection) {
        return new EmailConnectionSettings.Builder()
            .withEmailHost(emailConnection.emailHost)
            .withEmailPort(emailConnection.emailPort)
            .withEmailAuthUser(emailConnection.emailAuthUser)
            .withEmailAuthPassword(emailConnection.emailAuthPassword)
            .build();
    }

    private static GitExpiredBranchSettings createGitExpiredBranchSettings(
        GitExpiredBranchExtension expiredBranch,
        Project target
    ) {
        return new GitExpiredBranchSettings.Builder()
            .withRepoDir(expiredBranch.repoDir == null ? target.getRootDir() : expiredBranch.repoDir)
            .withGitArchiveRepository(expiredBranch.gitArchiveRepository)
            .withNotifierEmail(expiredBranch.notifierEmail)
            .withRemoverEmail(expiredBranch.removerEmail)
            .withAdminEmail(expiredBranch.adminEmail)
            .withStaleDaysToNotify(expiredBranch.staleDaysToNotify)
            .withStaleDaysToDelete(expiredBranch.staleDaysToDelete)
            .withIgnoreBranchesPatterns(expiredBranch.ignoreBranchesPatterns)
            .build();
    }
}