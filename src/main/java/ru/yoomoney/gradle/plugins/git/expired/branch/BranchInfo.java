package ru.yoomoney.gradle.plugins.git.expired.branch;

import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.revwalk.RevCommit;

/**
 * Информация о git брачне
 *
 * @author Vasily Sozykin
 *         Date: 22.04.2017.
 */
public class BranchInfo {

    private final Ref branch;
    private final RevCommit lastCommit;

    public BranchInfo(Ref branch, RevCommit lastCommit) {
        this.branch = branch;
        this.lastCommit = lastCommit;
    }

    public Ref getBranch() {
        return branch;
    }

    public RevCommit getLastCommit() {
        return lastCommit;
    }
}
