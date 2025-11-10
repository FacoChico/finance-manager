# Finance Manager

### Описание

**Finance Manager** — консольное Spring Boot-приложение для управления личными финансами.
Данные хранятся в памяти и сохраняются в JSON-файлы в папке `data/`.

#### Структура приложения

```
.
├── java
│   └── com
│       └── mephi
│           └── skillfactory
│               └── oop
│                   └── finance
│                       └── manager
│                           ├── FinanceManagerApplication.java   <- Стартовая точка приложения
│                           ├── cli
│                           │   └── CliRunner.java   <- Runner коммандной строки
│                           ├── domain   <- Доменные сущности
│                           │   ├── Budget.java
│                           │   ├── Operation.java
│                           │   ├── User.java
│                           │   ├── Wallet.java
│                           │   └── enumeration
│                           │       └── OperationType.java
│                           ├── repository   <- Работа с данными
│                           │   ├── CredentialsRepository.java   <- Сохранение и получение аутентификационных данных
│                           │   ├── FileBasedCredentialsRepository.java
│                           │   ├── WalletRepository.java   <- Сохранение и получение данных о кошельке
│                           │   ├── FileBasedWalletRepository.java
│                           │   └── exception
│                           │       └── FileContentTypeMismatchException.java
│                           └── service
│                               ├── AlertService.java   <- Сервис уведомлений о превышениях лимитов
│                               ├── auth
│                               │   ├── AuthService.java   <- Сервис аутентификации
│                               │   └── exception
│                               │       └── IllegalCredentialsException.java
│                               ├── exception
│                               │   └── UserNotFoundException.java
│                               └── wallet
│                                   ├── WalletService.java   <- Сервис управления кошельком пользователя
│                                   └── exception
│                                       ├── AmountException.java
│                                       ├── BudgetException.java
│                                       ├── CategoryNotFoundException.java
│                                       └── WalletImportSourceException.java
└── resources
    └── application.yaml
```

### Сборка

```bash
./gradlew clean build
```

### Запуск приложения

```bash
java -jar build/libs/finance-manager-0.0.1-SNAPSHOT.jar
```

### Запуск тестов

```bash
./gradlew test
```

### Основные команды CLI

- `help` — показать список команд;
- `register <login> <password>` — регистрация нового пользователя с логином login и паролем password;
- `login <login> <password>` — вход в аккаунт пользователя с логином login и паролем password;
- `logout` — выход из аккаунта;
- `add-income <amount> <category> [description]` — добавление дохода в количестве amount по категории category, описание description опционально;
- `add-expense <amount> <category> [description]` — добавление траты в количестве amount по категории category, описание description опционально;
- `edit-category <old-category-name> <new-category-name>` — изменение названия категории old-category-name на new-category-name;
- `set-budget <category> <amount>` — установление бюджета на категорию category в количестве amount;
- `edit-budget <category>` — изменение бюджета для категории category: удаление бюджета или изменение суммы через вложенные опции;
- `transfer <toLogin> <amount> [category] [description]` — перевод пользователю toLogin в количестве amount, опционально по категории category с
  описанием description;
- `summary` — получение сводной статистики по кошельку: доходы, расходы, бюджеты;
- `summary-by-categories <category1 ... categoryN>` — получение сводной статистики по переданным категориям: доходы, расходы, бюджеты;
- `export` — сохранение кошелька в файл по пути 'data/<login>.json';
- `import <path/to/wallet-file.json>` — импорт кошелька из json-файла в 'data/<login>.json' c присвоением кошелька текущему пользователю;
- `exit` — выход из приложения с сохранением всех кошельков пользователей на диск.

### Notes

- **Категория** не является отдельной **сущностью**, над которой можно выполнять CRUD-операции — это **свойство объектов** операция и бюджет;
- При изменении названия категории категория будет изменена во всех операциях и бюджетах пользователя; 
- При выходе из приложения все кошельки пользователей сохраняются в папке `data/`;
- Путь к файлам кошельков имеет вид `data/<login>.json`;
- **_Экспорт_ кошелька** и **_сохранение_ кошелька** пользователя — **_одна и та же операция_**.
  При необходимости использования файла кошелька **копируйте** его;
- При импорте кошелька из json-файла убедитесь в корректности структуры файла.
