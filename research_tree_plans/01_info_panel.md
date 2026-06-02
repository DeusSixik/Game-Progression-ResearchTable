# План 1. `ResearchInfoPanel` и контент через Builder

## Зачем это нужно

Сейчас информативная панель уже умеет показывать базовые данные исследования:

- `title`
- `description`
- `state`
- `study mode`
- кнопку запуска исследования
- progress bar для `TIMED`

Но дальше панель почти гарантированно будет разрастаться. В будущем туда хочется добавлять:

- условия открытия
- награды
- предметы, которые игрок получит
- дополнительные секции под разные типы исследований
- специальные блоки для `TABLE`
- кастомные подсказки или предупреждения

Если оставлять это в виде набора полей внутри `ResearchTreeScreenMainScreen`, код быстро превратится в большой UI-метод, который тяжело поддерживать.

Главная цель этого плана: отделить **данные панели** от **рендера панели**.

---

## Целевое устройство

Нужно разделить систему на 3 слоя:

1. `ResearchInfoContent`
   - чистые данные для панели
   - без логики рендера

2. `ResearchInfoContent.Builder`
   - удобный fluent API для сборки контента
   - нужен для экранов и будущих definition/provider систем

3. `ResearchInfoPanel`
   - отдельный UI-компонент
   - получает `ResearchInfoContent`
   - сам решает, какие секции показать, скрыть или обновить

---

## Предлагаемые классы

### `ResearchInfoContent`

Основной объект данных панели.

Предлагаемые поля:

- `title`
- `description`
- `groupTitle`
- `stateText`
- `studyModeText`
- `showResearchButton`
- `researchButtonText`
- `showTimedProgress`
- `timedProgressText`
- `timedProgress01`
- `List<ResearchConditionViewData> conditions`
- `List<ResearchRewardViewData> rewards`
- `List<ResearchInfoTagViewData> tags`
- `List<ResearchInfoSectionViewData> extraSections`

Важно: это должен быть **view-model**, а не игровая доменная модель.  
То есть сюда лучше класть уже подготовленные для UI строки/значения, а не тяжёлые игровые объекты.

### `ResearchInfoContent.Builder`

Fluent API, чтобы код сборки выглядел компактно.

Пример целевого использования:

```java
ResearchInfoContent content = ResearchInfoContent.builder()
        .title(node.getTitle())
        .groupTitle(node.getGroup().getTitle())
        .description(node.getDescription())
        .stateText(formatStateText(state, node))
        .studyModeText(formatStudyType(node))
        .researchButton("Start Timed Research", true)
        .timedProgress("Progress: 35%", 0.35f, true)
        .addCondition(new ResearchConditionViewData("Open Metallurgy", true))
        .addReward(new ResearchRewardViewData("Copper Pickaxe"))
        .build();
```

### `ResearchInfoPanel`

Отдельный UI-класс, который:

- создаёт все нужные labels/sections/buttons
- умеет принимать `ResearchInfoContent`
- обновляет только нужные части
- умеет скрывать пустые секции

Панель должна сама владеть:

- title label
- group label
- mode label
- state label
- description label
- timed progress label
- timed progress bar
- conditions container
- rewards container
- extra sections container
- research button
- close button

---

## Какие вспомогательные модели стоит завести

### `ResearchConditionViewData`

Для условий.

Поля:

- `text`
- `completed`
- `iconId` или `icon`
- `hint`

### `ResearchRewardViewData`

Для наград.

Поля:

- `title`
- `amountText`
- `icon`
- `description`

### `ResearchInfoSectionViewData`

Для универсальных кастомных секций.

Поля:

- `sectionTitle`
- `lines`
- `accentColor`
- `visible`

Эта модель пригодится, когда появятся особые блоки для разных типов исследований.

---

## Как встроить в текущую архитектуру

Сейчас лучше сделать так:

### В `ResearchTreeScreenMainScreen`

Оставить только orchestration:

- выбрать `selectedNode`
- вычислить `ResearchState`
- собрать `ResearchInfoContent`
- передать его в `ResearchInfoPanel`

То есть экран не должен сам управлять десятком label'ов.

### Новый метод

Например:

```java
protected ResearchInfoContent buildInfoContent(ResearchNode node, ResearchState state)
```

По умолчанию базовый экран будет собирать стандартный контент.

А конкретный экран сможет переопределить:

- добавить rewards
- добавить conditions
- добавить секции для `TABLE`
- добавить особую информацию о группе

---

## Поддержка разных типов исследований

### `INSTANT`

Панель показывает:

- базовое описание
- кнопку `Research`

### `TIMED`

Панель показывает:

- описание
- длительность до старта
- progress bar во время активного исследования
- скрывает progress после завершения

### `TABLE`

Панель показывает:

- описание
- кнопку `Open Table Research`
- может показывать отдельную секцию с механикой исследования

---

## Что важно не делать

Не стоит:

- держать все UI labels как публичные поля экрана
- собирать весь текст панели прямо в `screenTick()`
- делать rewards/conditions жёстко зашитыми в `ResearchNode`

Лучше:

- `ResearchNode` хранит ссылку на definition
- definition/provider умеет отдавать данные для info panel
- panel рендерит итоговый контент

---

## Минимальный этап внедрения

### Этап 1

- создать `ResearchInfoContent`
- создать `ResearchInfoContent.Builder`
- создать `ResearchInfoPanel`
- перевести текущую панель на эту систему без rewards/conditions

### Этап 2

- добавить `conditions`
- добавить `rewards`
- добавить универсальные `extraSections`

### Этап 3

- дать конкретным экранам возможность переопределять сборку контента

---

## Ожидаемый результат

После внедрения:

- код `ResearchTreeScreenMainScreen` станет заметно чище
- панель станет расширяемой без переписывания экрана
- награды, условия и будущие блоки можно будет добавлять декларативно
- появится стабильная точка расширения для серверных и gameplay-систем
