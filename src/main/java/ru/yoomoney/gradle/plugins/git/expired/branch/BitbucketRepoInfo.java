package ru.yoomoney.gradle.plugins.git.expired.branch;

/**
 * Информация о репозитории
 */
public class BitbucketRepoInfo {
    private String bitbucketUrl;
    private String project;
    private String repo;

    public BitbucketRepoInfo(String bitbucketUrl, String project, String repo) {
        this.bitbucketUrl = bitbucketUrl;
        this.project = project;
        this.repo = repo;
    }

    public String getBitbucketUrl() {
        return bitbucketUrl;
    }

    public String getProject() {
        return project;
    }

    public String getRepo() {
        return repo;
    }
}
