package ru.yoomoney.gradle.plugins.library.git.expired.branch.git;

import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.InitCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Collections;

import static java.util.Objects.requireNonNull;
import static ru.yoomoney.gradle.plugins.library.git.expired.branch.git.GitRepo.configureTransport;

/**
 * Класс для создания GitRepo
 *
 * @author horyukova
 * @since 24.06.2019
 */
public class GitRepoFactory {
    private static final Logger log = LoggerFactory.getLogger(GitRepoFactory.class);

    private final GitSettings settings;

    public GitRepoFactory(@Nonnull GitSettings settings) {
        this.settings =  requireNonNull(settings, "settings");
    }

    /**
     * Получить объект для работы с git-репозиторием из уже существующей директории
     *
     * @param directory директория с git-репозиторием
     * @return объект для работы с git-репозиторием
     */
    public GitRepo createFromExistingDirectory(@Nonnull File directory) {
        requireNonNull(directory, "directory");
        try {
            return new GitRepo(new Git(new FileRepositoryBuilder()
                    .readEnvironment()
                    .findGitDir(directory)
                    .build()), settings);
        } catch (IOException exc) {
            throw new RuntimeException("cannot clone repository", exc);
        }
    }

    /**
     * Прокси для вызова {@link InitCommand}
     *
     * @param command  команда для выполнения
     * @return объект для работы с git-репозиторием
     */
    public GitRepo createGitRepo(@Nonnull InitCommand command) {
        requireNonNull(command, "command");
        try {
            return new GitRepo(command.call(), settings);
        } catch (GitAPIException exc) {
            throw new RuntimeException("cannot createGitRepo repository", exc);
        }
    }

    /**
     * Прокси для вызова {@link CloneCommand}
     *
     * @param command  команда для выполнения
     * @return объект для работы с git-репозиторием
     */
    public GitRepo clone(@Nonnull CloneCommand command) {
        requireNonNull(command, "command");
        try {
            configureTransport(command, settings);
            return new GitRepo(command.call(), settings);
        } catch (GitAPIException exc) {
            throw new RuntimeException("git clone failed", exc);
        }
    }

    /**
     * Склонировать репозиторий.
     *
     * @param uri        адрес репозитория
     * @param repoDir    директория для клонирования
     * @param branchName имя бранча
     * @return объект для работы с git-репозиторием
     */
    public GitRepo clone(@Nonnull URI uri,
                         @Nonnull File repoDir,
                         @Nonnull String branchName) {
        log.info("Git clone: uri={}, branch={}", uri.toASCIIString(), branchName);
        CloneCommand cloneCommand = Git.cloneRepository()
                .setBranch(branchName)
                .setURI(uri.toASCIIString())
                .setDirectory(repoDir)
                .setBranchesToClone(Collections.singletonList("refs/heads/" + branchName));
        return clone(cloneCommand);
    }
}
