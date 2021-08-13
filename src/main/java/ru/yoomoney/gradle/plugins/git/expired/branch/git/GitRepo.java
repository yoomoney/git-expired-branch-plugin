package ru.yoomoney.gradle.plugins.git.expired.branch.git;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import org.eclipse.jgit.api.AddCommand;
import org.eclipse.jgit.api.CheckoutCommand;
import org.eclipse.jgit.api.CommitCommand;
import org.eclipse.jgit.api.DeleteBranchCommand;
import org.eclipse.jgit.api.DescribeCommand;
import org.eclipse.jgit.api.DiffCommand;
import org.eclipse.jgit.api.FetchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListBranchCommand;
import org.eclipse.jgit.api.LogCommand;
import org.eclipse.jgit.api.MergeCommand;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.api.RemoteAddCommand;
import org.eclipse.jgit.api.RemoteListCommand;
import org.eclipse.jgit.api.RemoteRemoveCommand;
import org.eclipse.jgit.api.RemoteSetUrlCommand;
import org.eclipse.jgit.api.RenameBranchCommand;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.StatusCommand;
import org.eclipse.jgit.api.TagCommand;
import org.eclipse.jgit.api.TransportCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.FetchResult;
import org.eclipse.jgit.transport.JschConfigSessionFactory;
import org.eclipse.jgit.transport.OpenSshConfig;
import org.eclipse.jgit.transport.SshTransport;
import org.eclipse.jgit.util.FS;
import org.eclipse.jgit.util.StringUtils;


import static org.eclipse.jgit.lib.Constants.R_TAGS;

/**
 * Класс для работы с git
 *
 * @author Oleg Kandaurov
 * @since 28.01.2019
 */
@SuppressFBWarnings("ITU_INAPPROPRIATE_TOSTRING_USE")
public class GitRepo implements AutoCloseable {
    private final Git git;
    private final GitSettings settings;

    GitRepo(Git git, GitSettings settings) {
        this.git = git;
        this.settings = settings;
    }

    /**
     * Прокси для вызова {@link PushCommand}
     *
     * @param command команда для выполнения
     * @return сообщение об ошибке, в случае неуспешного выполнения
     */
    public Optional<String> push(Consumer<PushCommand> command) {
        Objects.requireNonNull(command, "command");
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PushCommand pushCommand = git.push();
            command.accept(pushCommand);
            configureTransport(pushCommand, settings);
            pushCommand.setOutputStream(out);
            pushCommand.call();
            String resultMessage = out.toString();
            if (StringUtils.isEmptyOrNull(resultMessage) ||
                    resultMessage.contains("Create pull request") ||
                    resultMessage.contains("View pull request")) {
                return Optional.empty();
            } else {
                return Optional.of(resultMessage);
            }
        } catch (GitAPIException | IOException exc) {
            return Optional.of(exc.getMessage());
        }
    }

    /**
     * Получение коммита, на который указывает head
     *
     * @return ObjectId коммита, на который указывает head
     */
    public ObjectId getHeadCommit() {
        try {
            return git.getRepository().resolve(Constants.HEAD);
        } catch (IOException e) {
            throw new RuntimeException("Can't get head commit", e);
        }
    }

    /**
     * Получение директории репозитория
     *
     * @return директория репозитория
     */
    public File getDirectory() {
        return git.getRepository().getDirectory().getParentFile();
    }

    /**
     * Получение объекта с данными по репозитории
     *
     * @return данные репозитория
     */
    public Repository getRepository() {
        return git.getRepository();
    }

    /**
     * Прокси для вызова {@link CheckoutCommand}
     */
    public CheckoutCommand checkout() {
        return git.checkout();
    }

    /**
     * Прокси для вызова {@link RemoteAddCommand}
     */
    public RemoteAddCommand remoteAdd() {
        return git.remoteAdd();
    }

    /**
     * Прокси для вызова {@link DeleteBranchCommand}
     */
    public DeleteBranchCommand branchDelete() {
        return git.branchDelete();
    }

    /**
     * Прокси для вызова {@link ListBranchCommand}
     */
    public ListBranchCommand branchList() {
        return git.branchList();
    }

    /**
     * Прокси для вызова {@link LogCommand}
     */
    public LogCommand log() {
        return git.log();
    }

    /**
     * Прокси для вызова {@link DiffCommand}
     */
    public DiffCommand diff() {
        return git.diff();
    }

    /**
     * Прокси для вызова {@link CommitCommand}
     */
    public CommitCommand commit() {
        CommitCommand commit = git.commit();
        commit.setAuthor(settings.getUsername(), settings.getEmail());
        return commit;
    }

    /**
     * Прокси для вызова {@link StatusCommand}
     */
    public StatusCommand status() {
        return git.status();
    }

    /**
     * Прокси для вызова {@link AddCommand}
     */
    public AddCommand add() {
        return git.add();
    }

    /**
     * Прокси для вызова {@link MergeCommand}
     */
    public MergeResult merge(Consumer<MergeCommand> command) throws GitAPIException {
        MergeCommand merge = git.merge();
        command.accept(merge);
        MergeResult result = merge.call();
        MergeResult.MergeStatus status = result.getMergeStatus();
        if (status == MergeResult.MergeStatus.MERGED || status == MergeResult.MergeStatus.MERGED_SQUASHED) {
            RevCommit headCommit;
            try {
                headCommit = git.getRepository().parseCommit(getHeadCommit());
            } catch (IOException e) {
                throw new RuntimeException("cannot read git repository", e);
            }
            // JGit падает если amend не меняет данные коммита
            if (!headCommit.getAuthorIdent().equals(new PersonIdent(settings.getUsername(), settings.getEmail()))) {
                // Переписываем данные мерж коммита новым автором
                commit().setMessage(headCommit.getFullMessage()).setAmend(true).call();
            }
        }
        return result;
    }

    @Override
    public void close() {
        git.close();
    }

    /**
     * Конфигурирование взаимодействия с гитом
     *
     * @param command  команда, для которой нужна конфигурация
     * @param settings настройки для конфигурации
     */
    static void configureTransport(TransportCommand<?, ?> command, GitSettings settings) {
        if (!settings.getSshKeyPath().isPresent()) {
            return;
        }
        JschConfigSessionFactory sessionFactory = new JschConfigSessionFactory() {
            @Override
            protected void configure(OpenSshConfig.Host hc, Session session) {
                session.setConfig("StrictHostKeyChecking", "no");
            }

            @Override
            protected JSch getJSch(OpenSshConfig.Host hc, FS fs) throws JSchException {
                JSch jsch = super.getJSch(hc, fs);
                jsch.removeAllIdentity();
                jsch.addIdentity(settings.getSshKeyPath().get(), settings.getPassphraseSshKey().orElse(null));
                return jsch;
            }
        };
        command.setTransportConfigCallback(transport -> {
            if (transport instanceof SshTransport) {
                SshTransport sshTransport = (SshTransport) transport;
                sshTransport.setSshSessionFactory(sessionFactory);
            }
        });
    }
}
