package ru.yoomoney.gradle.plugins.git.expired.branch.helper;

import org.eclipse.jgit.api.ListBranchCommand;
import org.eclipse.jgit.api.LogCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.revwalk.filter.RevFilter;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import ru.yoomoney.gradle.plugins.git.expired.branch.BitbucketRepoInfo;
import ru.yoomoney.gradle.plugins.git.expired.branch.git.GitRepo;
import ru.yoomoney.gradle.plugins.git.expired.branch.git.GitSettings;
import ru.yoomoney.gradle.plugins.git.expired.branch.git.GitRepoFactory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.StreamSupport;

import static java.lang.String.format;

/**
 * Класс для выполнения операций с git репозиторием
 *
 * @author Vasily Sozykin
 *         Date: 19.03.2017.
 */
public class GitManager {
    private final Logger log = Logging.getLogger(GitManager.class);

    private static final Pattern BITBUCKET_GIT_CLONE_URL =
        Pattern.compile("ssh://git@(?<bitbucketUrl>.+)/(?<project>.+)/(?<repo>.+)\\.git");

    private final GitRepo git;

    public GitManager(File projectDir, GitSettings gitSettings) {
        GitRepoFactory gitRepoFactory = new GitRepoFactory(gitSettings);
        this.git = gitRepoFactory.createFromExistingDirectory(projectDir);
    }

    /**
     * Получение информации из cloneUrl
     *
     * @param cloneUrl ssh clone урл
     * @return информация
     */
    public BitbucketRepoInfo getInfoFromBitbucketCloneUrl(String cloneUrl) {
        Matcher matcher = BITBUCKET_GIT_CLONE_URL.matcher(cloneUrl);

        if (!matcher.matches()) {
            log.lifecycle("Wrong cloneUrl: " + cloneUrl);
            return new BitbucketRepoInfo("", "", "");
        }

        return new BitbucketRepoInfo(
            matcher.group("bitbucketUrl"),
            matcher.group("project"),
            matcher.group("repo")
        );
    }

    public String getBitbucketBranchesUrlFromCloneUrl(String cloneUrl) {
        return getBitbucketUrlFromCloneUrl("https://%s/projects/%s/repos/%s/branches", cloneUrl);
    }

    /**
     * Получение последнего коммита ветки
     *
     * @param repoBranch ref ветки
     * @return последний коммит
     */
    public RevCommit getLastCommitFromBranch(Ref repoBranch) {
        LogCommand logCommand = git.log();

        logCommand.setMaxCount(1);
        Iterable<RevCommit> commits;
        try {
            logCommand.add(repoBranch.getObjectId());
            commits = logCommand.call();
        } catch (GitAPIException | MissingObjectException | IncorrectObjectTypeException ex) {
            throw new RuntimeException("Can't get latest commit", ex);
        }

        return StreamSupport.stream(commits.spliterator(), false)
            .findFirst()
            .orElseThrow(() -> new RuntimeException(format("Commits not found, branch:%s", repoBranch.getName())));

    }

    /**
     * Получение RemoteConfig
     *
     * @return RemoteConfig
     */
    private RemoteConfig getRemoteConfig() {
        RemoteConfig remoteConfig;
        try {
            remoteConfig = RemoteConfig
                    .getAllRemoteConfigs(git.getRepository().getConfig()).get(0);
        } catch (URISyntaxException ex) {
            throw new RuntimeException("Can't get remote config", ex);
        }

        return remoteConfig;
    }

    /**
     * Получение имени репозитория в битбакете
     *
     * @return имя удаленного репозитория
     */
    public String getRepositoryName() {
        RemoteConfig remoteConfig = getRemoteConfig();
        String cloneUrl = remoteConfig.getURIs().get(0).toString();
        BitbucketRepoInfo info = getInfoFromBitbucketCloneUrl(cloneUrl);

        return info.getRepo();
    }

    /**
     * Получение имени проекта в битбакете
     *
     * @return имя проекта
     */
    public String getProjectName() {
        RemoteConfig remoteConfig = getRemoteConfig();
        String cloneUrl = remoteConfig.getURIs().get(0).toString();
        BitbucketRepoInfo info = getInfoFromBitbucketCloneUrl(cloneUrl);

        return info.getProject();
    }

    /**
     * Получение url проекта в битбакете
     *
     * @return url проекта в битбакете
     */
    public String getBitbucketProjectBranchesUrl() {
        RemoteConfig remoteConfig = getRemoteConfig();
        String url = remoteConfig.getURIs().get(0).toString();

        return getBitbucketBranchesUrlFromCloneUrl(url);
    }

    /**
     * Возвращает адрес до битбакета
     *
     * @param format формат
     * @param cloneUrl ssh урл
     * @return url в битбакете
     */
    private String getBitbucketUrlFromCloneUrl(String format, String cloneUrl) {
        BitbucketRepoInfo info = getInfoFromBitbucketCloneUrl(cloneUrl);

        return String.format(
                format,
                info.getBitbucketUrl(),
                info.getProject(),
                info.getRepo()
        );
    }

    /**
     * Получить список remote бранчей.
     *
     * @return список бранчей
     */
    public List<Ref> getRemoteBranches() {
        ListBranchCommand listOperation = git.branchList()
            .setListMode(ListBranchCommand.ListMode.REMOTE);
        try {
            return listOperation.call();
        } catch (GitAPIException ex) {
            throw new RuntimeException("Can't get remote branches", ex);
        }
    }

    /**
     * Удалить ветку локально и в удалённом репозитории
     *
     * @param branchName название ветки для удаления (например, "release/99.9")
     */
    public void deleteRemoteBranch(String branchName) {
        log.lifecycle("delete branches: from={}", branchName);
        try {
            git.branchDelete().setBranchNames(branchName).call();
            //аналог команды git push origin :branchName
            RefSpec refSpec = new RefSpec()
                    .setSource(null)
                    .setDestination("refs/heads/" + branchName);

            git.push(deletePushCommand -> deletePushCommand
                    .setRefSpecs(refSpec)
                    .setRemote("origin"))
                    .ifPresent(resultMessage -> {
                        throw new RuntimeException(format("Can't push: %s", resultMessage));
                    });
        } catch (GitAPIException ex) {
            throw new RuntimeException(format("Can't delete branch, branchName:%s", branchName), ex);
        }
    }

    /**
     * Сформировать diff для ветки branchName относительно HEAD
     * <p>
     *          x---y---z---branch
     *         /
     * ---a---b---c---d---e---HEAD
     * <p>
     * Метод вернёт diff, содержащий x, y, z
     *
     * @param branchName - название ветки, для которой требуется diff
     * @return diff ветки
     * @see <a href="https://stackoverflow.com/q/53569/1756750">https://stackoverflow.com/q/53569/1756750</a>
     */
    public String makeDiffBranch(String branchName) {
        Repository repository = git.getRepository();

        try (ObjectReader reader = repository.newObjectReader();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            ObjectId from = repository.resolve("HEAD^{tree}");
            ObjectId to = repository.resolve(branchName + "^{tree}");

            CanonicalTreeParser oldTreeIter = new CanonicalTreeParser();
            oldTreeIter.reset(reader, from);
            CanonicalTreeParser newTreeIter = new CanonicalTreeParser();
            newTreeIter.reset(reader, to);

            git.diff()
                    .setNewTree(newTreeIter)
                    .setOldTree(oldTreeIter)
                    .setOutputStream(baos)
                    .call();
            return baos.toString();
        } catch (IOException | GitAPIException ex) {
            throw new RuntimeException(format("Can't get diff between HEAD and branch, branchName:%s", branchName), ex);
        }
    }

    /**
     * Найти идентификатор коммита, который является Наименьшим общим предком (Lowest common ancestor) для каждого из бранчей.
     * Метод может использоваться, для того, чтобы найти идентификатор коммита в ветке {@code baseBranchName},
     * от которого было выполнено создание ветки {@code branchName}.
     * <p>
     *          x---y---z---branch
     *         /
     * ---a---b---c---d---e---master
     * <p>
     * Метод вернёт идентификатор коммита b, если выполнить вызов getGoodCommonAncestorsCommit("master", "branch")
     * или пустой Optional, если такой коммит не будет найден.
     *
     * @param baseBranchName - ветка, от которой отводилась ветка {@code branchName}.
     * @param branchName     - ветка, для которой требуется узнать идентификатор родительского коммита.
     * @return - идентификатор коммита
     */
    public Optional<String> getGoodCommonAncestorsCommit(String baseBranchName, String branchName) {
        try {
            RevWalk walk = new RevWalk(git.getRepository());
            RevCommit revBaseBranch = walk.lookupCommit(git.getRepository().findRef(baseBranchName).getObjectId());
            RevCommit revBranch = walk.lookupCommit(git.getRepository().findRef(branchName).getObjectId());
            walk.setRevFilter(RevFilter.MERGE_BASE);
            walk.markStart(revBaseBranch);
            walk.markStart(revBranch);
            RevCommit baseCommit = walk.next();
            return null == baseCommit ? Optional.empty() : Optional.of(baseCommit.getId().getName());
        } catch (IOException e) {
            throw new RuntimeException(String.format("Failed to write commitIds to files: baseBranchName=%s, branchName=%s",
                    baseBranchName, branchName), e);
        }
    }

    /**
     * Добавляет файлы, коммитит и пушит в указанную ветку
     *
     * @param branchName  имя ветки, куда будут внесены изменения
     * @param fileName    имя добавляемого файла
     */
    public void addFileIntoRemoteBranch(String branchName, String fileName) {
        try {
            git.checkout()
                    .setOrphan(true)
                    .setName(branchName)
                    .call();
            git.add()
                    .addFilepattern(fileName)
                    .call();
            git.commit()
                    .setAll(true)
                    .setMessage("add new file")
                    .call();
        } catch (GitAPIException ex) {
            throw new RuntimeException(format("Can't add file into remote branch, branchName:%s", branchName), ex);
        }
        push();
    }

    /**
     * Добавляет удаленный репозиторий
     *
     * @param url адрес репозитория
     */
    public void remoteAdd(String url) {
        try {
            git.remoteAdd()
                    .setName("origin")
                    .setUri(new URIish(url))
                    .call();
        } catch (URISyntaxException | GitAPIException ex) {
            throw new RuntimeException(format("Can't add remote, url:%s", url), ex);
        }
    }

    /**
     * Отправляет на удалённый сервер все коммиты
     */
    private void push() {
        try {
            String branchName = git.getRepository().getFullBranch();
            git.push(pushCommand -> pushCommand
                    .add(branchName)
                    .setRemote("origin"))
                    .ifPresent(resultMessage -> {
                        throw new RuntimeException(format("Can't push: %s", resultMessage));
                    });
        } catch (IOException exc) {
            throw new RuntimeException("Can't push", exc);
        }
    }

}