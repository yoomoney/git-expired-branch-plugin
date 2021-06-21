# yamoney-git-expired-branch-plugin

Плагин для отслеживания брошенных бранчей в репозитории.

## Подключение

```groovy
buildscript {
    repositories {
        maven { url 'http://nexus.yamoney.ru/repository/thirdparty/' }
        maven { url 'http://nexus.yamoney.ru/repository/central/' }
        maven { url 'http://nexus.yamoney.ru/repository/releases/' }
        maven { url 'http://nexus.yamoney.ru/repository/jcenter.bintray.com/' }
    }
    dependencies {
        classpath 'ru.yandex.money.gradle.plugins:yamoney-git-expired-branch-plugin:3.+'
    }
}

apply plugin: 'yamoney-git-expired-branch-plugin'

```

## Функционал

Содержит две gradle-задачи:

* **notifyAboutGitExpiredBranches** - высылает авторам уведомление о бранчах, в которых давно не было активности
* **removeExpiredGitBranches** - переносит бранчи, в которых давно не было активности, в специальный репозиторий, 
 по-умолчанию это - `ssh://git@bitbucket.yamoney.ru/backend-archive/branches-archive2.git`


Пример настройки плагина:
```groovy
expiredBranchSettings {
    repoDir = new File('.').absoluteFile                                                   // Не обязательный параметр, по-умолчанию - текущая рабочая директория gradle
    gitArchiveRepository = 'ssh://git@bitbucket-public.yamoney.ru/fa/branches-archive.git' // Не обязательный параметр, по-умолчанию - `ssh://git@bitbucket.yamoney.ru/backend-archive/branches-archive2.git`
    notifierEmail = 'bitbucket-stale-branch-notifier@yoomoney.ru'                           // Не обязательный параметр, по-умолчанию `bitbucket-stale-branch-notifier@yoomoney.ru`
    removerEmail = 'bitbucket-stale-branch-delete@yoomoney.ru'                              // Не обязательный параметр, по-умолчанию `bitbucket-stale-branch-delete@yoomoney.ru`
    adminEmail = 'SvcReleaserBackend@yoomoney.ru'                                           // Не обязательный параметр, по-умолчанию `SvcReleaserBackend@yoomoney.ru`
    staleDaysToNotify = 30                                                                 // Не обязательный параметр, по-умолчанию 30
    staleDaysToDelete = 60                                                                 // Не обязательный параметр, по-умолчанию 60
    // Не обязательный параметр, по-умолчанию не удаляются master, dev, release/*
    ignoreBranchesPatterns = [
        '^refs/remotes/origin/dev$',
        '^refs/remotes/origin/master$',
        '^refs/remotes/origin/HEAD$',
        '^refs/remotes/origin/release/.*$',
        '^refs/remotes/origin/support/.*$'
    ]                                                                 
}

gitForGitExpiredBranches {
   email = 'SvcReleaserBackend@yoomoney.ru'     // Обязательный параметр
   username = 'SvcReleaserBackend'             // Обязательный параметр
   passphraseSshKey = null                     // Парольная фраза для ssh ключа. Может быть не задана
   pathToGitPrivateSshKey = null               // Путь до приватного ssh ключа
}

emailForGitExpiredBranches {
   emailHost = 'mail.yoomoney.ru'               // Обязательный параметр
   emailPort = 25                              // Обязательный параметр
   emailAuthUser = 'testUser'                  // Обязательный параметр
   emailAuthPassword = 'testPassword'          // Обязательный параметр
}
```