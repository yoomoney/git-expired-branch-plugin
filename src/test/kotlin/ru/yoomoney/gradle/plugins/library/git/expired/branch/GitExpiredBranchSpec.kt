package ru.yoomoney.gradle.plugins.library.git.expired.branch

import org.eclipse.jgit.api.Git
import org.gradle.internal.impldep.org.junit.rules.TemporaryFolder
import org.gradle.testkit.runner.GradleRunner
import org.hamcrest.core.StringContains.containsString
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Test
import java.io.File

/**
 * Тесты GitExpiredBranchPlugin
 *
 * @author horyukova
 * @since 24.04.2019
 */

class GitExpiredBranchSpec {

    private val testProjectDir = TemporaryFolder()
    private lateinit var setupBuildFile: String
    private lateinit var buildFile: File

    @Before
    fun setup() {
        testProjectDir.create()
        buildFile = testProjectDir.newFile("build.gradle")

        Git.init().setDirectory(File(testProjectDir.root.absolutePath))
                .setBare(false)
                .call()

        setupBuildFile = """

                buildscript {
                    repositories {
                        maven { url 'http://nexus.yamoney.ru/content/repositories/thirdparty/' }
                        maven { url 'http://nexus.yamoney.ru/content/repositories/central/' }
                        maven { url 'http://nexus.yamoney.ru/content/repositories/releases/' }
                        maven { url 'http://nexus.yamoney.ru/content/repositories/public/' }
                    }
                }
                plugins {
                    id 'java'
                    id 'yoomoney-git-expired-branch-plugin'
                }
        """
    }

    @Test
    fun `should successfully notifyAboutGitExpiredBranches`() {
        buildFile.writeText(setupBuildFile + """
                    emailForGitExpiredBranches {
                        emailHost = "ho6st"
                        emailPort = 1
                    }
                    gitForGitExpiredBranches {
                        email = 'user@mail.ru'
                        username = 'user'
                    }
        """.trimIndent())

        val result = GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withArguments("notifyAboutGitExpiredBranches")
                .withPluginClasspath()
                .withDebug(true)
                .build()
        assertThat(result.output, containsString("Notifying about git expired branches"))
    }

    @Test
    fun `should successfully removeExpiredGitBranches`() {
        buildFile.writeText(setupBuildFile + """
                    emailForGitExpiredBranches {
                        emailHost = "ho6st"
                        emailPort = 1
                    }
                    gitForGitExpiredBranches {
                        email = 'user@mail.ru'
                        username = 'user'
                        pathToGitPrivateSshKey = "pithpath"
                    }
        """.trimIndent())

        val result = GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withArguments("removeExpiredGitBranches")
                .withPluginClasspath()
                .withDebug(true)
                .build()
        assertThat(result.output, containsString("Deleting branches \n" +
                "Notifying commiters about deletion"))
    }
}