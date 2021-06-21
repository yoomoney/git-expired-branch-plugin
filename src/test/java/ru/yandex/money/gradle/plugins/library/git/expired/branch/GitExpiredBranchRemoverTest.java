package ru.yandex.money.gradle.plugins.library.git.expired.branch;

import org.eclipse.jgit.api.CreateBranchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.InitCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.URIish;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import ru.yandex.money.gradle.plugins.library.git.expired.branch.helper.GitManager;
import ru.yandex.money.gradle.plugins.library.git.expired.branch.notification.MailSender;
import ru.yandex.money.gradle.plugins.library.git.expired.branch.settings.GitExpiredBranchSettings;
import ru.yandex.money.gradle.plugins.library.git.expired.branch.settings.GitExpiredBranchExtension;
import ru.yandex.money.tools.git.GitRepo;
import ru.yandex.money.tools.git.GitRepoFactory;
import ru.yandex.money.tools.git.GitSettings;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

public class GitExpiredBranchRemoverTest {

    private final MailSender mailSenderMock = mock(MailSender.class);


    private final GitSettings gitSettings = GitSettings.builder()
            .withUsername("username")
            .withEmail("username@yoomoney.ru")
            .build();

    private GitExpiredBranchSettings expiredBranchSettings;
    private Path testProjectDir;
    private Path originRepoFolder;
    private String archiveRepoUri;
    private RevCommit lastCommit;

    @BeforeMethod
    public void setUp() throws IOException, GitAPIException, URISyntaxException {
        testProjectDir = Files.createTempDirectory("expired_branch_repo_test");

        InitCommand initCommand = Git.init().setDirectory(testProjectDir.toFile());
        GitRepo gitRepo = new GitRepoFactory(gitSettings).createGitRepo(initCommand);

        addOriginRepo(gitRepo);

        commitToMaster(gitRepo);

        String staleBranchname = "stale-branch";

        commitToStaleBranch(gitRepo, staleBranchname);
        mergeToMaster(gitRepo, staleBranchname);
    }

    private void mergeToMaster(GitRepo gitRepo, String staleBranchname) throws GitAPIException, IOException {
        gitRepo.checkout().setName("master").call();
        ObjectId staleBranchId = gitRepo.getRepository().resolve("origin/" + staleBranchname);
        gitRepo.merge(mergeCommand -> mergeCommand.include(staleBranchId));
    }

    private void commitToStaleBranch(GitRepo gitRepo, String staleBranch) throws GitAPIException {
        gitRepo.checkout()
                .setCreateBranch(true)
                .setName("stale-branch")
                .setStartPoint("master")
                .setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.SET_UPSTREAM)
                .call();

        PersonIdent defaultCommitter = new PersonIdent(gitRepo.getRepository());
        Date dateTwoMonthAgo = Date.from(LocalDateTime.now().minusDays(61).toInstant(ZoneOffset.UTC));
        PersonIdent committer = new PersonIdent(defaultCommitter, dateTwoMonthAgo);

        lastCommit = gitRepo.commit().setAllowEmpty(true).setMessage("Second Commit").setCommitter(committer).call();

        gitRepo.push(pushCommand -> pushCommand.setRemote("origin").add(staleBranch))
                .ifPresent(it -> {
                    throw new RuntimeException("Failed to push: " + it);
                });
    }

    private void commitToMaster(GitRepo gitRepo) throws GitAPIException {
        gitRepo.commit().setAllowEmpty(true).setMessage("Initial Commit").call();
        gitRepo.push(pushCommand -> pushCommand.setRemote("origin").add("master"))
                .ifPresent(it -> {
                    throw new RuntimeException("Failed to push: " + it);
                });
    }

    private void addOriginRepo(GitRepo localRepo) throws IOException, GitAPIException, URISyntaxException {
        originRepoFolder = Files.createTempDirectory("expired_branch_repo_test_origin");
        Git.init()
                .setDirectory(originRepoFolder.toFile())
                .setBare(true)
                .call();
        archiveRepoUri = "file://" + originRepoFolder.toAbsolutePath() + '/';
        localRepo.remoteAdd()
                .setUri(new URIish(archiveRepoUri))
                .setName("origin")
                .call();

        GitExpiredBranchExtension expiredBranch = new GitExpiredBranchExtension();

        expiredBranchSettings = new GitExpiredBranchSettings.Builder()
                .withRepoDir(new File(originRepoFolder.toString()))
                .withGitArchiveRepository(archiveRepoUri)
                .withNotifierEmail(expiredBranch.notifierEmail)
                .withRemoverEmail(expiredBranch.removerEmail)
                .withAdminEmail(expiredBranch.adminEmail)
                .withStaleDaysToNotify(expiredBranch.staleDaysToNotify)
                .withStaleDaysToDelete(expiredBranch.staleDaysToDelete)
                .withIgnoreBranchesPatterns(expiredBranch.ignoreBranchesPatterns)
                .build();
    }

    @Test
    public void should_collect_stale_branches() {
        GitExpiredBranchRemover gitExpiredBranchRemover =
                new GitExpiredBranchRemover(expiredBranchSettings, mailSenderMock, gitSettings, testProjectDir.toFile(), null);
        List<BranchInfo> branchInfos = gitExpiredBranchRemover.collectGitStaleBranchesInfoToDelete();

        assertEquals(branchInfos.size(), 1);
        assertEquals(branchInfos.get(0).getBranch().getName(), "refs/remotes/origin/stale-branch");
    }

    @Test
    public void should_delete_delete_stale_branches() {
        GitExpiredBranchRemover gitExpiredBranchRemover =
                new GitExpiredBranchRemover(expiredBranchSettings, mailSenderMock, gitSettings, testProjectDir.toFile(), null);
        Ref ref = mock(Ref.class);
        when(ref.getName()).thenReturn("refs/remotes/origin/stale-branch");
        RevCommit revCommit = mock(RevCommit.class);
        BranchInfo branchInfo = new BranchInfo(ref, revCommit);
        List<BranchInfo> branchInfos = gitExpiredBranchRemover.deleteStaleBranches(singletonList(branchInfo));

        assertEquals(branchInfos.size(), 1);
        assertEquals(branchInfos.get(0).getBranch().getName(), "refs/remotes/origin/stale-branch");
    }

    @Test
    public void should_notify_about_stale_branches() {
        GitExpiredBranchRemover gitExpiredBranchRemover
                = new GitExpiredBranchRemover(expiredBranchSettings, mailSenderMock, gitSettings, testProjectDir.toFile(), "projectName");
        Ref ref = mock(Ref.class);
        when(ref.getName()).thenReturn("refs/remotes/origin/stale-branch");

        BranchInfo branchInfo = new BranchInfo(ref, lastCommit);
        gitExpiredBranchRemover.notifyAboutDeletedBranches(singletonList(branchInfo));

        GitManager gitManager = new GitManager(new File("."), gitSettings);

        String expectedEmailBody = "Добрый день, уважаемый коллега username!\n\n" +
                "В репозитории " + gitManager.getBitbucketBranchesUrlFromCloneUrl(originRepoFolder.getFileName().toString()) + " были найдены и удалены ветки, в которые вы не делали коммиты целых 60 дней!\n" +
                "Архив патчей удаленных веток находится тут: " + gitManager.getBitbucketBranchesUrlFromCloneUrl(expiredBranchSettings.getGitArchiveRepository()) + "\n" +
                "Ветка: \n" +
                "\t* refs/remotes/stale-branch\n";

        verify(mailSenderMock)
                .sendEmail("bitbucket-stale-branch-delete@yoomoney.ru",
                        singleton("username@yoomoney.ru"),
                        "Уведомление об удалении устаревших бранчей projectName",
                        expectedEmailBody);
    }
}