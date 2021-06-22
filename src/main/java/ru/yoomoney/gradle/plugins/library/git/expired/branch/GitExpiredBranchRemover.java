package ru.yoomoney.gradle.plugins.library.git.expired.branch;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Ref;
import ru.yoomoney.gradle.plugins.library.git.expired.branch.git.GitSettings;
import ru.yoomoney.gradle.plugins.library.git.expired.branch.helper.GitManager;
import ru.yoomoney.gradle.plugins.library.git.expired.branch.notification.MailSender;
import ru.yoomoney.gradle.plugins.library.git.expired.branch.settings.GitExpiredBranchSettings;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Удаляет ветки которые устарели
 *
 * @author Salavat Mustafin
 * @since 26.09.2019
 */
public class GitExpiredBranchRemover {
    private static final Pattern REMOTE_PATTERN = Pattern.compile("refs/remotes/origin/", Pattern.LITERAL);

    private GitExpiredBranchSettings expiredBranchSettings;
    private final GitSettings gitSettings;
    private final String projectName;
    private final GitManager gitManager;
    private final MailSender mailSender;

    GitExpiredBranchRemover(
            GitExpiredBranchSettings expiredBranchSettings,
            MailSender mailSender,
            GitSettings gitSettings,
            File repoDir,
            String projectName
    ) {
        this.expiredBranchSettings = expiredBranchSettings;
        this.mailSender = mailSender;
        this.gitManager = new GitManager(repoDir, gitSettings);
        this.gitSettings = gitSettings;
        this.projectName = projectName;
    }

    /**
     * Собирает устаревшие ветки
     */
    List<BranchInfo> collectGitStaleBranchesInfoToDelete() {
        long dateWeekAgo = LocalDateTime.now().minusDays(expiredBranchSettings.getStaleDaysToDelete())
                .atZone(ZoneId.systemDefault()).toEpochSecond();

        return gitManager.getRemoteBranches().stream()
                .filter(branch -> isBranchNotIgnored(branch, expiredBranchSettings.getIgnoreBranches()))
                .map(repoBranch -> new BranchInfo(repoBranch, gitManager.getLastCommitFromBranch(repoBranch)))
                .filter(branchInfo -> branchInfo.getLastCommit().getCommitTime() < dateWeekAgo)
                .collect(Collectors.toList());
    }

    private static boolean isBranchNotIgnored(Ref branch, Collection<Pattern> ignoreBranches) {
        for (Pattern ignoreBranchPattern : ignoreBranches) {
            if (ignoreBranchPattern.matcher(branch.getName()).matches()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Удаляет устаревшие ветки
     */
    List<BranchInfo> deleteStaleBranches(List<BranchInfo> staleBranchesInfo) {
        List<BranchInfo> deletedStaleBranches = new ArrayList<>();
        staleBranchesInfo.forEach(branchInfo -> {
            saveBranchDiffToArchiveRepo(branchInfo, gitManager);

            gitManager.deleteRemoteBranch(
                    REMOTE_PATTERN.matcher(branchInfo.getBranch().getName())
                            .replaceAll(Matcher.quoteReplacement("")));

            deletedStaleBranches.add(branchInfo);
        });
        return deletedStaleBranches;
    }

    /**
     * Отправляет email об удаленных ветках
     */
    void notifyAboutDeletedBranches(List<BranchInfo> deletedBranches) {
        deletedBranches.stream().collect(Collectors.groupingBy(branchInfo -> branchInfo.getLastCommit().getAuthorIdent(),
                Collectors.mapping(Function.identity(), Collectors.toSet())
        )).forEach((person, setOfBranchInfo) -> {
            String mailSubject = getDeleteBranchNotificationMailSubject();
            String mailBody = getDeleteBranchNotificationMailBody(person, setOfBranchInfo);
            try {
                mailSender.sendEmail(
                        expiredBranchSettings.getRemoverEmail(),
                        Collections.singleton(person.getEmailAddress()),
                        mailSubject,
                        mailBody
                );
            } catch (Exception e) {
                mailSender.sendEmail(
                        expiredBranchSettings.getRemoverEmail(),
                        Collections.singleton(expiredBranchSettings.getAdminEmail()),
                        mailSubject,
                        mailBody + "/n" + e.getMessage()
                );
            }
        });
    }

    private String getDeleteBranchNotificationMailBody(PersonIdent person, Set<BranchInfo> setOfBranchInfo) {
        StringBuilder builder = new StringBuilder();
        builder.append("Добрый день, уважаемый коллега ").append(person.getName()).append("!\n\n");
        builder.append("В репозитории ")
                .append(gitManager.getBitbucketProjectBranchesUrl())
                .append(" были найдены и удалены ветки, в которые вы не делали коммиты целых ")
                .append(expiredBranchSettings.getStaleDaysToDelete())
                .append(" дней!\n");

        builder.append("Архив патчей удаленных веток находится тут: ");
        builder.append(gitManager.getBitbucketBranchesUrlFromCloneUrl(expiredBranchSettings.getGitArchiveRepository()) + "\n");

        builder.append(setOfBranchInfo.size() > 1 ? "Ветки: " : "Ветка: ").append("\n");
        setOfBranchInfo.forEach(branchInfo ->
                builder.append("\t* ").append(getBranchUrl(branchInfo.getBranch().getName())).append('\n'));

        return builder.toString();
    }

    private static String getBranchUrl(String name) {
        return name.replaceAll("origin/", "");
    }

    private String getDeleteBranchNotificationMailSubject() {
        return "Уведомление об удалении устаревших бранчей " + projectName;
    }

    /**
     * Сохранить патч для данного бранча в Архивный репозиторий перед удалением бранча.
     * Действие необходимо для того, чтобы была возможность восстановить ветку, если она кому-то потребуется.
     *
     * @param branchInfo - ветка, diff для которой нужно сохранить
     */
    private void saveBranchDiffToArchiveRepo(BranchInfo branchInfo, GitManager gitManager) {
        String branchName = branchInfo.getBranch().getName();
        String branchNameWithoutOrigin =
                REMOTE_PATTERN.matcher(branchName).replaceAll(Matcher.quoteReplacement(""));

        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmssSSS");
        Date date = new Date(System.currentTimeMillis());

        String branchNameWithTimestamp = String.format(
                "%s_%s_%s-%s",
                gitManager.getProjectName(),
                gitManager.getRepositoryName(),
                branchNameWithoutOrigin,
                formatter.format(date)
        );

        String fileName = String.format("%s.diff",
                gitManager.getGoodCommonAncestorsCommit("origin/master", branchName)
                        .orElse(branchName.replaceAll("/", "_")));

        commitAndPushToArchiveRepository(gitManager.makeDiffBranch(branchName), branchNameWithTimestamp, fileName);
    }

    /**
     * Закоммитить и запушить данные строки fileLines в данную ветку.
     *
     * @param diff       - строки, которые нужно запушить в репозиторий.
     * @param branchName - ветка, куда нужно сделать пуш.
     * @param fileName   - название файла, в который нужно сделать пуш
     */
    private void commitAndPushToArchiveRepository(String diff,
                                                  String branchName,
                                                  String fileName) {
        try {
            Path tempDir = Files.createTempDirectory("git");

            Git.init().setDirectory(tempDir.toFile()).call();
            GitManager archiveRepo = new GitManager(tempDir.toFile(), gitSettings);

            Path fullPath = tempDir.resolve(fileName);
            Files.write(fullPath, Collections.singletonList(diff));

            archiveRepo.remoteAdd(expiredBranchSettings.getGitArchiveRepository());
            archiveRepo.addFileIntoRemoteBranch(branchName, fileName);
        } catch (IOException | GitAPIException e) {
            throw new RuntimeException("Cannot create tmpFile or write data to it: fileName=" + fileName, e);
        }
    }
}
