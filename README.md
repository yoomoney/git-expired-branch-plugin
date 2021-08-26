[![Build Status](https://travis-ci.com/yoomoney/git-expired-branch-plugin.svg?branch=master)](https://travis-ci.com/yoomoney/git-expired-branch-plugin)
[![codecov](https://codecov.io/gh/yoomoney/git-expired-branch-plugin/branch/master/graph/badge.svg)](https://codecov.io/gh/yoomoney/git-expired-branch-plugin)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

# yoomoney-git-expired-branch-plugin

Плагин для отслеживания брошенных бранчей в репозитории.

## Подключение

```groovy
buildscript {
    repositories {
        jcenter()
    }
    dependencies {
        classpath 'ru.yoomoney.gradle.plugins:git-expired-branch-plugin:6.+'
    }
}

apply plugin: 'ru.yoomoney.gradle.plugins.git-expired-branch-plugin'

```

## Функционал

Содержит две gradle-задачи:

* **notifyAboutGitExpiredBranches** - высылает авторам уведомление о бранчах, в которых давно не было активности
* **removeExpiredGitBranches** - переносит бранчи, в которых давно не было активности, в специальный репозиторий, 
 указанные в настройках


## Конфигурация плагина
```groovy
expiredBranchSettings {
    // Локальная папка с репозиторием, по-умолчанию - текущая рабочая директория gradle
    repoDir = new File('.').absoluteFile
    // Репозиторий для архивации диффов
    gitArchiveRepository = 'ssh://git@git_domain/branches-archive.git'
    // Адрес почты от которого придет уведомление об устаревших ветках
    notifierEmail = 'expiredBranchNotifier@test.ru'                 
    // Адрес почты от которого придет уведомление об удалении устаревших веток
    removerEmail = 'expiredBranchDelete@test.ru'
    // Адрес почты на который придет письмо при неуспехе отправки уведомлений
    adminEmail = 'admin@test.ru'
    // Количество дней по истечении которых начинаем пинговать авторов веток, если в ветках не было коммитов. По-умолчанию - 30 дней
    staleDaysToNotify = 30
    // Количество дней по истечении которых ветка удаляется, а дифф переносится в архивный репозиторий. По-умолчанию - 60 дней
    staleDaysToDelete = 60
    // Список паттернов веток, которые не нужно удалять. По-умолчанию не удаляются master, dev
    ignoreBranchesPatterns = [
        '^refs/remotes/origin/dev$',
        '^refs/remotes/origin/master$',
        '^refs/remotes/origin/HEAD$'
    ]                                                                 
}

// Настройки подключения к Git
gitForGitExpiredBranches {
    // Email пользователя, от имени которого будет производиться коммит в гит. Обязательная настройка.
    email = 'gitArchiver@test.ru'
    // Пользователь, от имени которого будет производиться коммит в гит. Обязательная настройка.
    username = 'GitArchiver'
    //Passphrase для приватного ssh ключа. По умолчанию не задана, имеет смысл совместно с pathToGitPrivateSshKey
    passphraseSshKey = null
    // Путь до приватного ssh ключа для доступа в git
    pathToGitPrivateSshKey = null
}

// Настройки подключения к Email шлюзу
emailForGitExpiredBranches {
    // Хост шлюза для отправки email. Обязательный параметр
    emailHost = 'mail.test.ru'
    // Порт шлюза для отправки email.
    emailPort = 25
    // Пользователь для авторзации в email шлюзе. Может быть установлено через переменную окружения "EMAIL_USER". Обязательный параметр.
    emailAuthUser = 'testUser'
    // Пароль для авторзации в email шлюзе. Может быть установлено через переменную окружения "EMAIL_PASSWORD". Обязательный параметр
    emailAuthPassword = 'testPassword'
}
```