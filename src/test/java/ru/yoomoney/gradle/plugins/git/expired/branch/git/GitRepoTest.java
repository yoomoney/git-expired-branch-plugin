package ru.yoomoney.gradle.plugins.git.expired.branch.git;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.MergeCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevCommit;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

/**
 * @author Oleg Kandaurov
 * @since 21.06.2019
 */
public class GitRepoTest {

    private static final GitSettings settings = GitSettings.builder()
            .withEmail("releaser@yoomoney.ru")
            .withUsername("Releaser")
            .build();

    @Test
    public void should_init_git_repository() throws IOException, GitAPIException {
        File tempDir = Files.createTempDirectory(getClass().getSimpleName()).toFile();
        GitRepoFactory gitRepoFactory = new GitRepoFactory(settings);

        try (GitRepo gitRepo = gitRepoFactory.createGitRepo(Git.init().setDirectory(tempDir))) {
            gitRepo.commit().setAllowEmpty(true).setMessage("createGitRepo").call();
            Iterable<RevCommit> logs = gitRepo.log().call();
            for (RevCommit revCommit : logs) {
                assertEquals(revCommit.getAuthorIdent().getEmailAddress(), settings.getEmail());
                assertEquals(revCommit.getAuthorIdent().getName(), settings.getUsername());
                return;
            }
            fail("no commits in repository");
        }
    }

    @Test
    public void should_get_repository_from_existin_directory() throws IOException, GitAPIException {
        File tempDir = Files.createTempDirectory(getClass().getSimpleName()).toFile();
        GitRepoFactory gitRepoFactory = new GitRepoFactory(settings);
        try (GitRepo gitRepo = gitRepoFactory.createGitRepo(Git.init().setDirectory(tempDir))) {
            gitRepo.commit().setAllowEmpty(true).setMessage("createGitRepo").call();

            try (GitRepo existingRepo = gitRepoFactory.createFromExistingDirectory(tempDir)) {
                Iterable<RevCommit> logs = existingRepo.log().call();
                for (RevCommit ignored : logs) {
                    return;
                }
                fail("no commits in repository");
            }

        }
    }

    @Test
    public void should_find_all_tags() throws GitAPIException, IOException, URISyntaxException {
        File tempDir = Files.createTempDirectory(getClass().getSimpleName()).toFile();
        GitRepoFactory gitRepoFactory = new GitRepoFactory(settings);
        try (GitRepo gitRepo = gitRepoFactory.createGitRepo(Git.init().setDirectory(tempDir))) {

            File buildFile = new File(tempDir.getPath(), "build.gradle");
            List<String> lines = Arrays.asList("The first line", "The second line");

            Files.write(Paths.get(buildFile.getAbsolutePath()), lines, Charset.forName("UTF-8"));
            gitRepo.add().addFilepattern("build.gradle")
                    .call();

            gitRepo.commit().setMessage("build.gradle commit").call();

            gitRepo.tag().setName("2.0.0").call();
            gitRepo.tag().setName("3.0.0").call();

            assertEquals(gitRepo.listTags().get(0).getName(), "refs/tags/2.0.0");
            assertEquals(gitRepo.listTags().get(1).getName(), "refs/tags/3.0.0");
        }
    }

    @Test
    public void should_set_author_when_merge() throws GitAPIException, IOException, URISyntaxException {
        File tempDir = Files.createTempDirectory(getClass().getSimpleName()).toFile();
        GitRepoFactory gitRepoFactory = new GitRepoFactory(settings);
        try (GitRepo gitRepo = gitRepoFactory.createGitRepo(Git.init().setDirectory(tempDir))) {
            gitRepo.commit().setAllowEmpty(true).setMessage("init").call();

            gitRepo.checkout().setName("1").setCreateBranch(true).call();
            Files.write(gitRepo.getDirectory().toPath().resolve("file1"), "1".getBytes(UTF_8));
            gitRepo.add().addFilepattern("file1").call();
            gitRepo.commit().setAll(true).setMessage("commit1").call();

            gitRepo.checkout().setName("master").call();

            gitRepo.checkout().setName("2").setCreateBranch(true).call();
            Files.write(gitRepo.getDirectory().toPath().resolve("file2"), "2".getBytes(UTF_8));
            gitRepo.add().addFilepattern("file2").call();
            gitRepo.commit().setAll(true).setMessage("commit2").call();


            ObjectId branch1Ref = gitRepo.getRepository().resolve("1");
            gitRepo.merge(mergeCommand -> mergeCommand
                    .setFastForward(MergeCommand.FastForwardMode.NO_FF)
                    .include(branch1Ref)
                    .setMessage("merge")
                    .setCommit(true));

            Iterable<RevCommit> logs = gitRepo.log().setMaxCount(1).call();
            for (RevCommit commit : logs) {
                assertEquals(commit.getShortMessage(), "merge");
                assertEquals(commit.getAuthorIdent().getEmailAddress(), settings.getEmail());
                assertEquals(commit.getAuthorIdent().getName(), settings.getUsername());
                return;
            }
            fail("no commits in repository");

        }
    }
}