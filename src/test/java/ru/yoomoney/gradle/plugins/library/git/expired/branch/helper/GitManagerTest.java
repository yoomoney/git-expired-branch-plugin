package ru.yoomoney.gradle.plugins.library.git.expired.branch.helper;

import org.eclipse.jgit.api.CreateBranchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.URIish;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import ru.yoomoney.gradle.plugins.library.git.expired.branch.git.GitSettings;


import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;

/**
 * @author horyukova
 * @since 11.04.2019
 */
public class GitManagerTest {
    private GitManager gitManager;
    private Git git;
    private Path projectDir;

    @BeforeMethod
    public void before() throws IOException, GitAPIException, URISyntaxException {
        projectDir = Files.createTempDirectory("git_manager_test");
        git = initProjectRepoWithMasterBranch(projectDir);
        gitManager = new GitManager(projectDir.toFile(), GitSettings.builder()
                .withEmail("SvcReleaserBackend@yoomoney.ru")
                .withUsername("SvcReleaserBackend")
                .build());
    }

    @Test
    public void should_return_last_commit() throws IOException, GitAPIException {
        createBranch("origin/dev", "master");
        push();

        File gradleProperties = new File(projectDir.toAbsolutePath().toString(), "gradle.properties");
        Files.write(Paths.get(gradleProperties.getAbsolutePath()), Collections.singletonList("version=1.0.1"),
                Charset.forName("UTF-8"));
        git.add().addFilepattern("gradle.properties")
                .call();
        commit("build.gradle commit");
        //добавили 1 коммит, ожидаем, что вернется он
        RevCommit buildCommit = gitManager.getLastCommitFromBranch(git.getRepository().findRef("origin/dev"));
        assertThat(buildCommit.getFullMessage(), equalTo("build.gradle commit"));


        File testFile = new File(projectDir.toAbsolutePath().toString(), "test.txt");
        Files.write(Paths.get(testFile.getAbsolutePath()), Collections.singletonList("testing"),
                Charset.forName("UTF-8"));
        git.add().addFilepattern("test.txt")
                .call();
        commit("test.txt commit");

        //добавили 2 коммита, ожидаем возвращения последнего
        RevCommit testCommit = gitManager.getLastCommitFromBranch(git.getRepository().findRef("origin/dev"));
        assertThat(testCommit.getFullMessage(), equalTo("test.txt commit"));
    }

    @Test
    public void should_find_lowest_common_ancestor() throws IOException, GitAPIException {
        //делаем коммит в мастер
        File gradleProperties = new File(projectDir.toAbsolutePath().toString(), "gradle.properties");
        Files.write(Paths.get(gradleProperties.getAbsolutePath()), Collections.singletonList("version=1.0.1"),
                Charset.forName("UTF-8"));
        git.add().addFilepattern("gradle.properties")
                .call();
        commit("build.gradle commit");
        push();
        String expectedBaseCommit = gitManager.getLastCommitFromBranch(git.getRepository().findRef("master"))
                .getId().getName();

        //создаем от мастера ветку
        createBranch("origin/dev", "master");
        push();

        //делаем коммит в новую ветку
        File testFile = new File(projectDir.toAbsolutePath().toString(), "test.txt");
        Files.write(Paths.get(testFile.getAbsolutePath()), Collections.singletonList("testing"),
                Charset.forName("UTF-8"));
        git.add().addFilepattern("test.txt")
                .call();
        commit("test.txt commit");
        push();


        git.checkout()
                .setName("master")
                .call();

        //делаем коммит в мастер
        File masterFile = new File(projectDir.toAbsolutePath().toString(), "master.txt");
        Files.write(Paths.get(masterFile.getAbsolutePath()), Collections.singletonList("master file"),
                Charset.forName("UTF-8"));
        git.add().addFilepattern("master.txt")
                .call();
        commit("master.txt commit");
        push();

        //ожидаем, что вернется коммит, от которого создавалась ветка dev
        assertThat(gitManager.getLastCommitFromBranch(git.getRepository().findRef("master")).getFullMessage(),
                equalTo("master.txt commit"));
        assertThat(gitManager.getLastCommitFromBranch(git.getRepository().findRef("origin/dev")).getFullMessage(),
                equalTo("test.txt commit"));

        String baseCommit = gitManager.getGoodCommonAncestorsCommit("master", "origin/dev")
                .orElse("dev");
        assertThat(baseCommit, equalTo(expectedBaseCommit));
    }

    @Test
    public void should_return_diff_from_head() throws IOException, GitAPIException {
        File gradleProperties = new File(projectDir.toAbsolutePath().toString(), "gradle.properties");
        Files.write(Paths.get(gradleProperties.getAbsolutePath()), Collections.singletonList("version=1.0.1"),
                Charset.forName("UTF-8"));
        git.add().addFilepattern("gradle.properties")
                .call();
        commit("build.gradle commit");
        push();

        createBranch("origin/dev", "master");
        push();

        File testFile = new File(projectDir.toAbsolutePath().toString(), "test.txt");
        Files.write(Paths.get(testFile.getAbsolutePath()), Collections.singletonList("testing"),
                Charset.forName("UTF-8"));
        git.add().addFilepattern("test.txt")
                .call();
        commit("test.txt commit");
        push();

        git.checkout()
                .setName("master")
                .call();
        File masterFile = new File(projectDir.toAbsolutePath().toString(), "master.txt");
        Files.write(Paths.get(masterFile.getAbsolutePath()), Collections.singletonList("master file"),
                Charset.forName("UTF-8"));
        git.add().addFilepattern("master.txt")
                .call();
        commit("master.txt commit");
        push();

        String diff = gitManager.makeDiffBranch("origin/dev");
        assertThat(diff, equalTo("diff --git a/master.txt b/master.txt\n" +
                "deleted file mode 100644\n" +
                "index e36e0c4..0000000\n" +
                "--- a/master.txt\n" +
                "+++ /dev/null\n" +
                "@@ -1 +0,0 @@\n" +
                "-master file\n" +
                "diff --git a/test.txt b/test.txt\n" +
                "new file mode 100644\n" +
                "index 0000000..038d718\n" +
                "--- /dev/null\n" +
                "+++ b/test.txt\n" +
                "@@ -0,0 +1 @@\n" +
                "+testing\n"));

    }

    private Git initProjectRepoWithMasterBranch(Path projectDir) throws GitAPIException, IOException, URISyntaxException {
        git = Git.init().setDirectory(projectDir.toFile()).call();

        File buildFile = new File(projectDir.toAbsolutePath().toString(), "build.gradle");
        List<String> lines = Arrays.asList("The first line", "The second line");

        Files.write(Paths.get(buildFile.getAbsolutePath()), lines, Charset.forName("UTF-8"));
        git.add().addFilepattern("build.gradle")
                .call();

        git.commit().setMessage("build.gradle commit").call();

        Path originRepoFolder = Files.createTempDirectory("origin");
        Git.init().setDirectory(originRepoFolder.toFile())
                .setBare(true)
                .call();

        git.remoteAdd().setUri(new URIish("file://" + originRepoFolder.toAbsolutePath() + "/"))
                .setName("origin")
                .call();
        git.push()
                .setPushAll()
                .setRemote("origin")
                .setPushTags()
                .call();
        return git;
    }

    private void createBranch(String newBranchName, String fromBranchName) throws GitAPIException {
        git.checkout()
                .setCreateBranch(true)
                .setName(newBranchName)
                .setStartPoint(fromBranchName)
                .setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.SET_UPSTREAM)
                .call();
    }

    private void push() throws GitAPIException, IOException {
        PushCommand pushCommand = git.push()
                .add(git.getRepository().getFullBranch())
                .setRemote("origin");

        pushCommand.call();
    }

    private void commit(String message) throws GitAPIException {
        git.commit()
                .setAll(true)
                .setMessage(message)
                .call();
    }
}
